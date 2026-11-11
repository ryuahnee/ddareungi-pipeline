# 도메인 용어와 쿼리 기준

- shared는 거치율(%)이며 대여소 거치대 수 대비 자전거 수의 비율이다. 고갈은 shared < 10, 혼잡은 shared > 90이다.
- run_id는 Airflow 논리적 실행을 식별한다. 같은 실행의 재시도도 같은 run_id를 사용한다. 실행 식별자의 UTC 시각을 collected_at과 혼동하지 않는다.
- collected_at은 timestamp without time zone에 저장한 KST(Asia/Seoul) 벽시계값이다. 실제 스냅샷 2026-11-08 00:10:45와 run_id의 2026-11-07T15:00:00+00:00를 확인했고, 수집 코드의 LocalDateTime.now() 및 컨테이너 TZ=Asia/Seoul과 일치한다. UTC로 다시 해석하지 않는다.
- 오늘/어제는 Asia/Seoul 기준이다. 날짜 계산은 (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')::date를 사용한다.
- 결측은 보간하지 않는다. 관측되지 않은 시간은 0으로 간주하지 않는다. 데이터가 오래됐어도 현재 시각의 관측이라고 표현하지 않는다.
- 07시대는 KST 07:00 이상 08:00 미만, 09시대는 09:00 이상 10:00 미만이다. 부족 전환은 두 시간대가 모두 관측된 대여소-일 중 07시대 평균 shared >= 10이고 09시대 평균 shared < 10인 경우(is_transition=true)이다.
- mart_station_snapshot: 현재 대여소 전체 현황. 현재 고갈 수는 이 테이블에서 shared < 10인 행을 센다. 대여소를 나열할 때는 station_id와 station_name을 함께 조회하고 station_name을 사람이 읽는 이름으로 쓴다. station_id는 문자열 식별자이므로 이름 대신 단독으로 표시하지 않는다.
- mart_depletion_alert: run별 고갈 경보 이력. 경보 빈도는 대여소별 COUNT(DISTINCT convert_to(run_id, 'UTF8'))로 비교한다. run_id는 ASCII 실행 식별자이므로 convert_to(run_id, 'UTF8')로 바이트 동일성을 비교해 문자열 정렬 비용을 줄인다.
- mart_congestion_alert: run별 혼잡 경보 이력.
- mart_weather_depletion: run별 날씨(강수 유형·기온)별 고갈 대여소 수. precip_type=0은 비/눈 없음, >0은 강수 있음(눈 포함). 비교 시 run별·강수 여부별 depletion_count를 먼저 합산하고 평균을 비교해 관측 횟수 차이를 보정한다. 인과관계를 주장하지 않는다.
- mart_depletion_with_weather: 고갈 대여소와 날씨 관측을 조인한 상세 이력.
- mart_station_version_usage: 대여소 SCD 주소 버전별 평균 거치율과 관측 횟수.
- mart_subway_rush_depletion: 역 반경 500m 내 시간대별 고갈율(depletion_rate, 이미 백분율)과 주변 대여소 수. hour_of_day는 KST 시각이다.
- mart_depletion_transition: 07·09시대 짝 관측 대여소-일의 부족 전환 여부. transition_date는 KST 날짜다.
- 부족 전환 수를 집계할 때는 같은 날짜의 전체 관측 행 수도 observed_station_days로 반환한다. observed_station_days=0이면 전환 0건이 아니라 관측 없음으로 해석한다.
- 위 용도는 README 기준이다. 스키마에 추가 mart가 있어도 질문에 적합한 위 테이블을 우선 사용하고, _bak_ 테이블은 사용하지 않는다.
- 날짜 범위는 collected_at >= 시작 AND collected_at < 끝으로 제한한다. 모든 스키마는 public이다.

# 예시 질문 → SQL

지금 고갈된 대여소 몇 개야?
SELECT count(*) FILTER (WHERE shared < 10) AS target_count, count(*) AS total_count FROM public.mart_station_snapshot;

현재 고갈된 대여소 목록 보여줘
SELECT station_id, station_name, shared FROM public.mart_station_snapshot WHERE shared < 10 ORDER BY shared ASC, station_id LIMIT 10;

고갈 경보가 가장 많이 발생한 대여소 상위 5개
SELECT station_id, max(station_name) AS station_name, count(DISTINCT convert_to(run_id, 'UTF8')) AS alert_runs FROM public.mart_depletion_alert GROUP BY station_id ORDER BY alert_runs DESC, station_id LIMIT 5;

8시에 지하철역 주변 고갈율이 높은 역 top 5
SELECT out_stn_num, stn_kr_nm, line_nm, depletion_rate FROM public.mart_subway_rush_depletion WHERE hour_of_day = 8 ORDER BY depletion_rate DESC NULLS LAST, out_stn_num LIMIT 5;

비 올 때와 안 올 때 고갈 대여소 수 비교
WITH per_run AS (SELECT run_id, CASE WHEN precip_type > 0 THEN '강수 있음(눈 포함)' ELSE '강수 없음' END AS weather, sum(depletion_count) AS depleted_stations FROM public.mart_weather_depletion WHERE precip_type IS NOT NULL GROUP BY run_id, CASE WHEN precip_type > 0 THEN '강수 있음(눈 포함)' ELSE '강수 없음' END) SELECT weather, round(avg(depleted_stations), 2) AS avg_depleted_stations, count(*) AS observed_runs FROM per_run GROUP BY weather ORDER BY weather;

어제 부족 전환된 대여소 수
SELECT count(CASE WHEN is_transition THEN 1 END) AS transitioned_stations, count(*) AS observed_station_days FROM public.mart_depletion_transition WHERE transition_date = (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')::date - 1;
