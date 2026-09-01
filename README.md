# ddareungi-pipeline

서울 따릉이 공공 API 데이터를 수집·집계하고 REST API와 Grafana 대시보드로 서빙하는 데이터 엔지니어링 파이프라인.

---

## 전체 플로우

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            외부 데이터 소스                                  │
│                                                                             │
│  따릉이 실시간 API   기상청 ASOS API   공휴일 API   따릉이 이용통계   지하철역 API  │
│  (매 10분)          (매시)             (매월)        (매월)          (매일)   │
└────┬─────────────────┬────────────────┬────────────┬──────────────┬─────────┘
     │                 │                │            │              │
     ▼                 ▼                ▼            ▼              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     Airflow (BashOperator → Kotlin jar)                     │
│                                                                             │
│  ddareungi_pipeline   weather_pipeline   holiday_pipeline                   │
│  (*/10 * * * *)       (0 * * * *)        (0 0 15 * *)                       │
│                                                                             │
│  station_master_pipeline   subway_pipeline   use_history_mart_pipeline      │
│  (0 0 * * *)               (0 4 * * *)       (0 1 5 * *)                    │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         MySQL (NAS 원장)                                     │
│                                                                             │
│  bike_status          weather_asos        holiday                           │
│  (실시간 스냅샷)        (시간별 기상관측)     (공휴일 목록)                      │
│                                                                             │
│  bike_station (SCD)   bike_use_daily      subway_station                   │
│  (대여소 마스터)        (일별 이용현황)       (지하철역 마스터)                   │
│                                                                             │
│  bike_station_subway_map                                                    │
│  (대여소 ↔ 지하철역 반경 500m 매핑)                                            │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │  집계 (Kotlin jar)
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      PostgreSQL (mart, 로컬 SSD)                             │
│                                                                             │
│  [실시간 — 매 10분]                                                           │
│  mart_station_snapshot          mart_depletion_alert                        │
│  mart_congestion_alert          mart_depletion_with_weather                 │
│  mart_weather_depletion                                                     │
│                                                                             │
│  [날씨 분석 — 매시]                                                            │
│  mart_hourly_weather_bike       mart_weather_bike_stats                     │
│  mart_holiday_bike_stats        mart_station_holiday_depletion              │
│                                                                             │
│  [지하철 연관 — 매일 04:00]                                                    │
│  mart_subway_rush_depletion  (역별 × 시간대별 반경 500m 내 고갈율)              │
│                                                                             │
│  [기타 — 매일/매월]                                                            │
│  mart_station_cluster           mart_cluster_profile                        │
│  mart_morning_rush              mart_evening_rush                           │
│  mart_use_daily_agg             mart_use_monthly_agg                        │
│  mart_weather_threshold                                                     │
└──────────────┬──────────────────────────┬───────────────────────────────────┘
               │                          │
               ▼                          ▼
┌──────────────────────────┐  ┌───────────────────────────────────────────────┐
│   Grafana (port 3000)    │  │          Ktor REST API (port 8090)            │
│                          │  │                                               │
│  • 시간대별 고갈 Geomap   │  │  GET /api/stations/snapshot                   │
│  • 날씨별 이용률 분석      │  │  GET /api/stations/depletion                  │
│  • 공휴일/평일 비교        │  │  GET /api/stations/congestion                 │
│  • 지하철역 주변 고갈 지도  │  │  GET /api/stats/holiday                       │
│  • 출퇴근 혼잡 분석        │  │  GET /api/stats/weather                       │
│                          │  │  ... (총 17개 엔드포인트)                       │
└──────────────────────────┘  └───────────────────────────────────────────────┘
```

---

## 파이프라인 상세

### ddareungi_pipeline `*/10 * * * *`

```
ddareungiRealtimeSync  →  martRealtimeSync
(따릉이 API 수집)           (snapshot / depletion_alert /
(→ MySQL bike_status)       congestion_alert / weather_depletion /
                            depletion_with_weather → PostgreSQL)
```

### weather_pipeline `0 * * * *`

```
asosCollect  →  martWeatherAnalysisSync   (hourly_weather_bike, weather_bike_stats)
(ASOS 수집)  →  martHolidayAnalysisSync   (holiday_bike_stats, station_holiday_depletion)
```

### holiday_pipeline `0 0 15 * *`

```
holidayCollect  →  martHolidayBikeStats
(다음달 공휴일)
```

### station_master_pipeline `0 0 * * *`

```
stationMasterSync  →  martStationScdSync
(대여소 SCD 갱신)      (버전별 거치율 mart)
```

### subway_pipeline `0 4 * * *`

```
subwayStationSync  →  martSubwayRushDepletion
(역 수집 +              (역 × 시간대별
 500m 매핑 계산)          고갈율 집계)
```

### use_history_mart_pipeline `0 1 5 * *`

```
martUseHistorySync
(전월~당월 이용 집계 → use_daily_agg, use_monthly_agg)
```

---

## 기술 스택

| 역할 | 기술 |
|---|---|
| 오케스트레이션 | Apache Airflow 2.x |
| 배치 실행 | Kotlin + JVM (`ddareungi-batch.jar`) |
| REST API | Ktor 2.x (`port 8090`) |
| 원장 DB | MySQL 8 (NAS, `jsh.icehodduk.synology.me:32771`) |
| mart DB | PostgreSQL 15 (로컬 SSD, `localhost:5433`) |
| 시각화 | Grafana (`localhost:3000`) |
| 컨테이너 | Docker Compose |

---

## 실행

```bash
# 인프라 실행
cd infra/docker && docker compose up -d

# batch jar 빌드
./gradlew :batch:shadowJar

# API 서버 실행
GRAFANA_DB_URL=jdbc:postgresql://localhost:5433/grafana \
GRAFANA_DB_USER=grafana \
GRAFANA_DB_PASSWORD=grafana \
./gradlew :api:run
```

---

## 디렉토리 구조

```
ddareungi-pipeline/
├── batch/                  # 수집·집계 배치 (Kotlin)
│   ├── client/             # 외부 API 클라이언트
│   ├── job/                # 잡 단위 실행 로직
│   ├── db/                 # MySQL / PostgreSQL 클라이언트
│   └── model/              # 데이터 모델
├── api/                    # REST API 서버 (Ktor)
│   └── routes/             # 엔드포인트 라우트
└── infra/
    ├── airflow/dags/       # Airflow DAG 정의
    └── docker/             # docker-compose, Dockerfile
```
