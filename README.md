# ddareungi-pipeline

서울 따릉이 공공 API 데이터를 수집·적재하는 데이터 파이프라인.
실시간 대여소 현황부터 날씨, 공휴일, 지하철역 연관까지 다양한 원천을 수집해 mart로 집계하고 REST API와 Grafana로 서빙한다.

---

## 아키텍처

```
외부 API
  ├── 따릉이 실시간    (10분 주기 설계*)
  ├── 기상청 ASOS     (매시)
  ├── 공휴일          (매월)
  └── 지하철역 마스터  (매일)
         │
         ▼
  Airflow (BashOperator → Kotlin jar)
         │
         ▼
  MySQL (NAS)          ← 원장 (raw data)
  ├── bike_status           실시간 대여소 스냅샷  (10분 주기 설계*, 결측 있음)
  ├── weather_asos          기상청 ASOS 시간 관측
  ├── holiday               공휴일 (한국천문연구원)
  ├── bike_station          대여소 마스터 (SCD Type 2)
  ├── subway_station        지하철역 마스터 (1~9호선)
  └── bike_station_subway_map   대여소 ↔ 역 반경 500m 매핑
         │
         │  집계 (Kotlin jar)
         ▼
  PostgreSQL (로컬 SSD) ← mart (집계 결과)
  ├── mart_station_snapshot      현재 대여소 전체 현황
  ├── mart_depletion_alert       고갈 경보 (shared < 10%)
  ├── mart_congestion_alert      혼잡 경보 (shared > 90%)
  ├── mart_weather_depletion     날씨별 고갈 카운트 (run_id별)
  ├── mart_depletion_with_weather  고갈 대여소 + 날씨 조인
  ├── mart_station_version_usage   SCD 버전별 평균 거치율
  ├── mart_subway_rush_depletion   역 반경 내 시간대별 고갈율
  └── mart_depletion_transition    부족 전환 대여소-일 (07시대→09시대)
         │
         ├── Grafana (port 3000)   시각화 대시보드
         └── Ktor REST API (port 8090)   17개 엔드포인트
```

---

## 파이프라인 (DAG)

### ddareungi_pipeline `*/10 * * * *`
```
ddareungiRealtimeSync  →  martRealtimeSync            →  martReconciliationCheck
     따릉이 수집               snapshot / depletion_alert /      원천-mart 고갈 건수 대조
  → MySQL bike_status          congestion_alert /                불일치 시 태스크 실패
                               weather_depletion /
                               depletion_with_weather → PostgreSQL
```

### weather_pipeline `0 * * * *`
```
asosCollect
  기상청 ASOS 당일 시간 자료 → MySQL weather_asos (upsert, 자가복구)
```

### station_master_pipeline `0 0 * * *`
```
stationMasterSync  →  martStationScdSync
  대여소 마스터 SCD 갱신      버전별 거치율 mart → PostgreSQL
```

### subway_pipeline `0 4 * * *`
```
subwayStationSync  →  martSubwayRushDepletion
  역 수집 + 500m 매핑          역×시간대별 고갈율 → PostgreSQL
```

### holiday_pipeline `0 0 15 * *`
```
holidayCollect
  다음 달 공휴일 → MySQL holiday
```

---

## 설계 결정

**원장 / mart 이중 레이어**
- MySQL(NAS)에 raw를 쌓고, PostgreSQL(로컬 SSD)에 집계 결과만 적재
- Grafana는 PostgreSQL만 바라봄 → 원장 쿼리 부하 없음

**멱등성**
- bike_status: `(station_id, collected_at)` unique → 재실행 시 UPDATE
- weather_asos: `(observed_at, stn)` unique → 재실행 시 UPDATE
- mart: 실시간 mart는 run_id 단위 누적, 집계 mart는 전체 DELETE 후 INSERT

**SCD Type 2 (bike_station)**
- 대여소 이전·통폐합 이력 보존
- `is_current=1` 행이 현재 유효 버전
- 변경 감지 시 이전 행 `valid_to` 마감 후 신규 행 삽입

**MySQL NAS 제약**
- IOPS 제한 (~200–1000 rows/sec) → 대량 작업은 daily chunk 단위 처리
- 장기 집계 쿼리 연결 끊김 → `socketTimeout=0` 설정
- 집계 부하 큰 mart(subway)는 매시간 대신 매일 04:00 별도 파이프라인으로 분리

**\* 수집 주기와 결측**
- 10분 주기로 **설계**했으나, Airflow가 로컬 Mac의 Docker Desktop에서 구동됨
- Mac 종료·절전 시 스케줄러가 함께 중단되어 수집 공백 발생 (예: 2026-07-16~08-24 등)
- `bike_status`는 10분 단위 실시간 스냅샷이라 지난 시각을 소급 수집할 수 없음 → 공백 구간은 **영구 결측**
- 결측은 보간하지 않음. 시간대 비교 지표는 두 시간대가 모두 관측된 대여소-일만 사용

**원천-mart 대조 체크** (`martReconciliationCheck`)
- 매 run마다 원천 `bike_status`의 고갈 건수와 `mart_depletion_alert` 건수를 비교
- 불일치 시 태스크를 실패시켜 mart 적재 누락을 즉시 드러냄
- 대조 체크는 적재와 같은 함수(`readMartDepletionAlert`)를 쓰므로 고갈 정의(shared<10, run별 최신 collected_at)가 한 곳에 고정됨 (계산 차이는 대상 아님, 적재 누락만 검출)
- 과거 누락 run은 `martAlertBackfill`(run별 DELETE 후 INSERT, 멱등)로 복구

**부족 전환 지표** (`mart_depletion_transition`)
- **Grain**: 대여소 × 날짜 (07시대·09시대가 모두 관측된 경우만 적재)
- **Key**: (transition_date, station_id)
- **주요 컬럼**
  - `rate_0700`, `rate_0900`: 시간대 스냅샷 평균 거치율 (표시용, 소수 2자리)
  - `is_transition`: 07시대 평균 ≥10% AND 09시대 평균 <10% (원본 평균으로 판정)
  - `is_weekday`, `is_holiday`: 제외하지 않고 표시만, 조회 시 필터
- **적재**: 일별 DAG(`depletion_transition_pipeline`, 01:00 KST), 논리적 실행일 기준 날짜 단위 DELETE 후 INSERT (멱등)
- **정의 위치**: `readDepletionTransition` 한 곳
- **비율 계산**: `SUM(is_transition) / COUNT(*)` 로 mart 하나에서 분자·분모 계산

---

## 기술 스택

| 역할 | 기술 |
|---|---|
| 오케스트레이션 | Apache Airflow 2.x |
| 배치 실행 | Kotlin JVM (`ddareungi-batch.jar`) |
| REST API | Ktor 2.x (port 8090) |
| 원장 DB | MySQL 8 (NAS) |
| mart DB | PostgreSQL 15 (로컬 SSD) |
| 시각화 | Grafana (port 3000) |
| 컨테이너 | Docker Compose |

---

## REST API

PostgreSQL mart를 직접 조회하는 읽기 전용 API. `port 8090`.

### 엔드포인트

| 메서드 | 경로 | 설명 | mart 테이블 |
|---|---|---|---|
| GET | `/api/stations/snapshot` | 현재 전체 대여소 현황 | `mart_station_snapshot` |
| GET | `/api/stations/depletion` | 현재 고갈 대여소 (shared < 10%) | `mart_depletion_alert` |
| GET | `/api/stations/congestion` | 현재 혼잡 대여소 (shared > 90%) | `mart_congestion_alert` |
| GET | `/api/stations/depletion-weather` | 현재 고갈 대여소 + 날씨 정보 | `mart_depletion_with_weather` |
| GET | `/api/stations/version-usage` | 대여소 주소 이전 전후 거치율 비교 | `mart_station_version_usage` |
| GET | `/api/stats/weather-depletion` | run_id별 날씨×고갈 카운트 | `mart_weather_depletion` |
| GET | `/api/subway/rush` | 지하철역 반경 500m 내 시간대별 고갈율 | `mart_subway_rush_depletion` |

### 쿼리 파라미터

`GET /api/subway/rush?hour=8` — 특정 시간대(0~23) 필터. 생략 시 전체 시간대 반환.

### 응답 예시

```bash
# 현재 고갈 대여소
curl http://localhost:8090/api/stations/depletion

# 출근시간(8시) 역 주변 고갈율
curl "http://localhost:8090/api/subway/rush?hour=8"
```

```json
[
  {
    "station_id": "ST-154",
    "station_name": "338. 세운스퀘어 앞",
    "parking_bike_tot_cnt": 0,
    "shared": 0,
    "collected_at": "2026-09-10T08:40:01.000+00:00",
    "run_id": "scheduled__2026-09-10T08:30:00+00:00"
  }
]
```

---

## 실행

```bash
# 인프라 실행
cd infra/docker && docker compose up -d

# batch jar 빌드
./gradlew :batch:shadowJar

# API 서버
GRAFANA_DB_URL=jdbc:postgresql://localhost:5433/grafana \
GRAFANA_DB_USER=grafana \
GRAFANA_DB_PASSWORD=grafana \
./gradlew :api:run
```

---

## 디렉토리 구조

```
ddareungi-pipeline/
├── batch/
│   ├── client/      외부 API 클라이언트 (따릉이, ASOS, 공휴일, 지하철)
│   ├── job/         잡 단위 실행 로직
│   ├── db/          MySqlClient / PostgresClient
│   └── model/       데이터 모델
├── api/             Ktor REST API (port 8090)
└── infra/
    ├── airflow/dags/  DAG 정의 (6개)
    └── docker/        docker-compose, Dockerfile
```
