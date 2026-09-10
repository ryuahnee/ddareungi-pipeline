# ddareungi-pipeline

서울 따릉이 공공 API 데이터를 수집·적재하는 데이터 파이프라인.
실시간 대여소 현황부터 날씨, 공휴일, 지하철역 연관까지 다양한 원천을 수집해 mart로 집계하고 REST API와 Grafana로 서빙한다.

---

## 아키텍처

```
외부 API
  ├── 따릉이 실시간    (매 10분)
  ├── 기상청 ASOS     (매시)
  ├── 공휴일          (매월)
  └── 지하철역 마스터  (매일)
         │
         ▼
  Airflow (BashOperator → Kotlin jar)
         │
         ▼
  MySQL (NAS)          ← 원장 (raw data)
  ├── bike_status           실시간 대여소 스냅샷  (10분 단위)
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
  └── mart_subway_rush_depletion   역 반경 내 시간대별 고갈율
         │
         ├── Grafana (port 3000)   시각화 대시보드
         └── Ktor REST API (port 8090)   17개 엔드포인트
```

---

## 파이프라인 (DAG)

### ddareungi_pipeline `*/10 * * * *`
```
ddareungiRealtimeSync  →  martRealtimeSync
     따릉이 수집               snapshot / depletion_alert /
  → MySQL bike_status          congestion_alert /
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
