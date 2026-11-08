# gpt-5.4-mini-2026-03-17 평가

- 실행 시각(UTC): 2026-11-07T16:59:36.103664Z ~ 2026-11-07T17:00:23.892453Z
- 코드 기준 커밋: `d7e02608446d831e107a8a8c0667c2b2c2ed36c0`
- 평가셋 SHA-256: `3d00e653073a0a5e2ecb8ebea4129db499ca14de82a7eed9845873f42b8d5361`
- 용어 사전 SHA-256: `681d8dd4787c22a31a7a2095319d84ccbd83af93b6717b11006644b1d1132efc`
- 스키마 SHA-256: `ff0034796ba1e89c2e3b73f36422be321c642d2e601953b2d1f2f631ed4c0701`

strict 정확도: 13/25 (52.0%)
relaxed 정확도: 24/25 (96.0%)
평균 LLM 지연(실패 시도 포함): 1559.5 ms
총 토큰: 85520 (prompt=82193, completion=3327, usage 수신=25/25)

정답 SQL은 모두 검증기를 통과했고 nl_reader로 실행해 1~100행을 반환했습니다. 문항마다 REPEATABLE READ 트랜잭션 안에서 정답과 생성 SQL을 비교했습니다. 문항·모델 사이에는 실시간 mart 갱신이 있을 수 있습니다.

컬럼명은 무시하고 숫자 타입 값은 HALF_UP 소수 둘째 자리로 반올림합니다. 기본 비교는 행 집합이며, 정답 SQL 최상위에 ORDER BY와 LIMIT/FETCH가 함께 있으면 행 순서도 비교합니다. 100행 초과 결과는 통과시키지 않습니다.

strict는 컬럼 수와 위치도 일치해야 합니다. relaxed는 정답 각 컬럼을 생성 결과의 서로 다른 컬럼에 대응시킨 뒤 투영한 전체 행을 동일한 행 비교 규칙으로 비교합니다. 보조 컬럼과 컬럼 순서는 허용하지만, 값을 고르거나 행 사이의 컬럼 관계를 바꾸지는 않습니다.

API 실패도 두 정확도 분모에 포함합니다. usage 없는 호출의 토큰은 집계하지 않으며, 이런 모델의 품질·비용은 이 결과만으로 비교할 수 없습니다.

## 카테고리별 정확도

| 카테고리 | strict | relaxed |
|---|---:|---:|
| 단순 집계 | 1/4 (25.0%) | 4/4 (100.0%) |
| top-N | 3/3 (100.0%) | 3/3 (100.0%) |
| 시간 필터 | 0/3 (0.0%) | 3/3 (100.0%) |
| 날씨 | 2/3 (66.7%) | 3/3 (100.0%) |
| 지하철 시간대 | 2/3 (66.7%) | 3/3 (100.0%) |
| 부족 전환 | 3/3 (100.0%) | 3/3 (100.0%) |
| SCD 버전 | 1/3 (33.3%) | 2/3 (66.7%) |
| 조인 | 1/3 (33.3%) | 3/3 (100.0%) |

## 문항별 결과

| id | 카테고리 | strict | relaxed | LLM ms | prompt | completion |
|---|---|---|---|---:|---:|---:|
| agg01 | 단순 집계 | 결과 불일치 | 일치 | 1795 | 3264 | 73 |
| agg02 | 단순 집계 | 결과 불일치 | 일치 | 1263 | 3272 | 79 |
| agg03 | 단순 집계 | 결과 불일치 | 일치 | 1911 | 3272 | 142 |
| agg04 | 단순 집계 | 일치 | 일치 | 1609 | 3269 | 71 |
| top01 | top-N | 일치 | 일치 | 2018 | 3316 | 191 |
| top02 | top-N | 일치 | 일치 | 1147 | 3292 | 100 |
| top03 | top-N | 일치 | 일치 | 1505 | 3298 | 167 |
| time01 | 시간 필터 | 결과 불일치 | 일치 | 1804 | 3276 | 181 |
| time02 | 시간 필터 | 결과 불일치 | 일치 | 1714 | 3276 | 188 |
| time03 | 시간 필터 | 결과 불일치 | 일치 | 1518 | 3283 | 117 |
| weather01 | 날씨 | 일치 | 일치 | 1573 | 3284 | 160 |
| weather02 | 날씨 | 일치 | 일치 | 1201 | 3278 | 100 |
| weather03 | 날씨 | 결과 불일치 | 일치 | 1466 | 3273 | 128 |
| subway01 | 지하철 시간대 | 일치 | 일치 | 1710 | 3306 | 142 |
| subway02 | 지하철 시간대 | 결과 불일치 | 일치 | 1389 | 3277 | 120 |
| subway03 | 지하철 시간대 | 일치 | 일치 | 1322 | 3275 | 117 |
| transition01 | 부족 전환 | 일치 | 일치 | 1418 | 3315 | 151 |
| transition02 | 부족 전환 | 일치 | 일치 | 1726 | 3312 | 158 |
| transition03 | 부족 전환 | 일치 | 일치 | 1368 | 3289 | 124 |
| scd01 | SCD 버전 | 결과 불일치 | 일치 | 1317 | 3268 | 81 |
| scd02 | SCD 버전 | 일치 | 일치 | 1316 | 3311 | 124 |
| scd03 | SCD 버전 | 결과 불일치 | 결과 불일치 | 1721 | 3303 | 134 |
| join01 | 조인 | 결과 불일치 | 일치 | 1510 | 3282 | 127 |
| join02 | 조인 | 결과 불일치 | 일치 | 1825 | 3283 | 174 |
| join03 | 조인 | 일치 | 일치 | 1841 | 3319 | 178 |

## 검증 Lineage

결과 행은 보관하지 않고 각 비교 시점의 원시 결과 해시와 데이터 기준 시각만 남깁니다. 해시는 결과 순서·숫자 표기에도 영향을 받으며 정확도 판정에는 위 정규화 규칙을 사용합니다.

| id | 정답 결과 SHA-256 | 생성 결과 SHA-256 | 데이터 기준 시각(KST) |
|---|---|---|---|
| agg01 | dc1cd32f19b2660b5cea6ce6d5a9f745e9d22a2b8937f2b2bdeadd1b163422c1 | d9ebd90cee74264543c23281e74a0aa4c2e8466fa4d284d679275217d1361c7f | 2026-11-08T01:50:02 |
| agg02 | f65731e33222dbbb248617986c87a85738588de788bfb7a6e2a45fc76ad97a75 | 8743bb06b1f9e6ac99795cb3e8f6333478ad4ec161adeecf8b68382bbc4b63a1 | 2026-11-08T01:50:02 |
| agg03 | 5b41f8e5d32e0f9db9e434e88f64991a51a43d1ec628156a0dc5e6f1d47a153b | 0144c096eea2865c6e4ce35914010666fbbb6423e6d5742f8d4d6bfbb46309d3 | 2026-11-08T01:50:02 |
| agg04 | 0271736ab681cbb040c202794b0e74a01887e33ed1af4d8452b40ceaf120a339 | 0271736ab681cbb040c202794b0e74a01887e33ed1af4d8452b40ceaf120a339 | 2026-11-08T01:50:02 |
| top01 | e56430d75e12af6bc09e109e7228b1dd032743e664b09bb9dd803544a5dfc814 | e56430d75e12af6bc09e109e7228b1dd032743e664b09bb9dd803544a5dfc814 | 2026-11-08T01:50:02 |
| top02 | 6fe1822b178575c70b6b5543b2293d15a543e1e867815bebe5f42f3ef992f3ab | 6fe1822b178575c70b6b5543b2293d15a543e1e867815bebe5f42f3ef992f3ab | 2026-11-08T01:50:02 |
| top03 | 2f5779f18cef285fb3677e77a8fc8b632e70319c326ed36cc7464add7a75660d | 2f5779f18cef285fb3677e77a8fc8b632e70319c326ed36cc7464add7a75660d | 2026-11-08T01:50:02 |
| time01 | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | 9b347830ba125f419e071cafb91a07d0400fd961f18b6d2557013c0d0cdbc011 | 2026-11-08T01:50:02 |
| time02 | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | 2b3c90e46ae3b3c2ba722562a651f6c970a9bff16c0a8a119ce20a5eed35d053 | 2026-11-08T01:50:02 |
| time03 | 1219f18433f40f7b82d39daa65b703b82642843f81419948623c171bbedd3496 | 860a3f00091745e506d5c8eb22533edd1ba35606a90c694eda5df02031403cde | collected_at 없음/실행 실패 |
| weather01 | 83d073249f03cbd1c9070f9c69c2f61ed4a4ce42f260194c7f48ad1f71455a2d | 83d073249f03cbd1c9070f9c69c2f61ed4a4ce42f260194c7f48ad1f71455a2d | 2026-11-08T01:50:02 |
| weather02 | 4c8230e6e95045b85f619f6de4560030220c7ea1d3145b343f19022d629fd8ca | 4c8230e6e95045b85f619f6de4560030220c7ea1d3145b343f19022d629fd8ca | 2026-11-08T01:50:02 |
| weather03 | f65731e33222dbbb248617986c87a85738588de788bfb7a6e2a45fc76ad97a75 | 5c4f131764d901e0cca66ffc74844b639bf0960ef3e17765efa00399764c1f78 | 2026-11-08T01:50:02 |
| subway01 | ce953bfed9e4880a800ad3fa622a58748c51f2eea79ca4dc91989d9e1ddf7762 | ce953bfed9e4880a800ad3fa622a58748c51f2eea79ca4dc91989d9e1ddf7762 | collected_at 없음/실행 실패 |
| subway02 | 16cd2ad382a446a49ff10bf8a3a26739275b0c3702c70cbc83a98e250bfdc9b8 | 4ee6e0d379953fd551cce368f8fdf80f5a7f08180ccbd5c70ba370b6e8da5436 | collected_at 없음/실행 실패 |
| subway03 | b092013fa76354677e373ebb04a189e85129370794f4e83e4c03bff59744ea5c | b092013fa76354677e373ebb04a189e85129370794f4e83e4c03bff59744ea5c | collected_at 없음/실행 실패 |
| transition01 | c00ab9aba43d1c99bf2786e95559bd0776efccca076cb0f4250d661e9b85b96f | c00ab9aba43d1c99bf2786e95559bd0776efccca076cb0f4250d661e9b85b96f | collected_at 없음/실행 실패 |
| transition02 | 31bf252179e689d4efaeaa8ea71b5f5c95f73be51a92b92e19b5eb863cedd8fd | 31bf252179e689d4efaeaa8ea71b5f5c95f73be51a92b92e19b5eb863cedd8fd | collected_at 없음/실행 실패 |
| transition03 | eb91a955f1e651e7625fe2f2df850596a4a829157501b59e103ccdab4d22a8f4 | eb91a955f1e651e7625fe2f2df850596a4a829157501b59e103ccdab4d22a8f4 | collected_at 없음/실행 실패 |
| scd01 | a07f313fe389722744c2ae0bce79f0b07e99de7bec953968d2ef08054d47b7ba | b6348f450edba5ec356dfad68cf2d471b128a46f3ea586e1213c23c96e7615e3 | collected_at 없음/실행 실패 |
| scd02 | fd2b4eb9b72ed63b8652a8e62b2aef87d45cb9143c694d3c4afb14551f320376 | fd2b4eb9b72ed63b8652a8e62b2aef87d45cb9143c694d3c4afb14551f320376 | collected_at 없음/실행 실패 |
| scd03 | ea2d021e1b0bcac3ba04ad99111fc3d5526943ecdc142d7be9b4c6ea348f45b1 | 5f3638d753b33db42ebb722cea47a1871a6f20a1de24364f8389f5bbe83d8827 | collected_at 없음/실행 실패 |
| join01 | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | c36abb1a50a95a52efce8e0d60587a2ebbff6b9cb0536c7c6470eb3506f38a83 | 2026-11-08T02:00:02 |
| join02 | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | 91d506ad048963dcb9e5fc198c9822553ba0a0df1e838d163b893035de2180d2 | 2026-11-08T02:00:02 |
| join03 | 4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945 | 4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945 | 2026-11-08T02:00:02 |

## 실패 SQL

### agg01: strict=결과 불일치, relaxed=일치

```sql
SELECT count(*) AS target_count, count(*) AS total_count FROM public.mart_station_snapshot;
```

### agg02: strict=결과 불일치, relaxed=일치

```sql
SELECT count(*) FILTER (WHERE shared < 10) AS target_count, count(*) AS total_count FROM public.mart_station_snapshot;
```

### agg03: strict=결과 불일치, relaxed=일치

```sql
SELECT count(*) FILTER (WHERE shared > 90) AS target_count, count(*) AS total_count FROM public.mart_congestion_alert WHERE collected_at >= TIMESTAMP '2026-10-19 00:00:00' AND collected_at < TIMESTAMP '2026-10-20 00:00:00';
```

### time01: strict=결과 불일치, relaxed=일치

```sql
SELECT count(*) FILTER (WHERE collected_at >= timestamp '2026-10-19 07:00:00' AND collected_at < timestamp '2026-10-19 08:00:00') AS target_count, count(*) AS total_count FROM public.mart_depletion_alert WHERE collected_at >= timestamp '2026-10-19 00:00:00' AND collected_at < timestamp '2026-10-20 00:00:00';
```

### time02: strict=결과 불일치, relaxed=일치

```sql
SELECT COUNT(*) FILTER (WHERE collected_at >= TIMESTAMP '2026-10-19 09:00:00' AND collected_at < TIMESTAMP '2026-10-19 10:00:00') AS target_count, COUNT(*) AS total_count FROM public.mart_congestion_alert WHERE collected_at >= TIMESTAMP '2026-10-19 00:00:00' AND collected_at < TIMESTAMP '2026-10-20 00:00:00';
```

### time03: strict=결과 불일치, relaxed=일치

```sql
SELECT COUNT(*) FILTER (WHERE is_transition) AS target_count, COUNT(*) AS total_count FROM public.mart_depletion_transition WHERE transition_date = DATE '2026-10-19';
```

### weather03: strict=결과 불일치, relaxed=일치

```sql
SELECT COUNT(*) FILTER (WHERE precip_type = 0) AS target_count, COUNT(*) AS total_count FROM public.mart_depletion_with_weather WHERE collected_at = (SELECT MAX(collected_at) FROM public.mart_depletion_with_weather);
```

### subway02: strict=결과 불일치, relaxed=일치

```sql
SELECT COUNT(*) FILTER (WHERE depletion_rate > 50) AS target_count, COUNT(*) AS total_count FROM public.mart_subway_rush_depletion WHERE hour_of_day = 9;
```

### scd01: strict=결과 불일치, relaxed=일치

```sql
SELECT count(*) FILTER (WHERE is_current = 1) AS target_count, count(*) AS total_count FROM public.mart_station_version_usage;
```

### scd03: strict=결과 불일치, relaxed=결과 불일치

```sql
WITH version_counts AS (SELECT rntls_id, COUNT(DISTINCT addr1) AS version_count FROM public.mart_station_version_usage GROUP BY rntls_id HAVING COUNT(DISTINCT addr1) >= 2) SELECT rntls_id, version_count FROM version_counts ORDER BY version_count DESC, rntls_id ASC LIMIT 5;
```

### join01: strict=결과 불일치, relaxed=일치

```sql
SELECT count(*) AS target_count, (SELECT count(*) FROM public.mart_station_snapshot) AS total_count FROM public.mart_station_snapshot s INNER JOIN public.mart_depletion_with_weather w ON s.station_id = w.station_id AND s.run_id = w.run_id;
```

### join02: strict=결과 불일치, relaxed=일치

```sql
SELECT COUNT(*) AS target_count, COUNT(*) AS total_count FROM public.mart_station_snapshot s JOIN public.mart_congestion_alert c ON s.station_id = c.station_id AND s.run_id = c.run_id WHERE s.collected_at = (SELECT MAX(collected_at) FROM public.mart_station_snapshot) AND c.collected_at = (SELECT MAX(collected_at) FROM public.mart_congestion_alert)
```
