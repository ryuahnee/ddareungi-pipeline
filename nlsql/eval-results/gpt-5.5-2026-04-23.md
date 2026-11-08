# gpt-5.5-2026-04-23 평가

- 실행 시각(UTC): 2026-11-07T16:25:36.074546Z ~ 2026-11-07T16:27:22.428863Z
- 코드 기준 커밋: `a99a5167c58e62ab09e407f6b086192ec28b9c7b`
- 평가셋 SHA-256: `3d00e653073a0a5e2ecb8ebea4129db499ca14de82a7eed9845873f42b8d5361`
- 용어 사전 SHA-256: `2595d126aee53b2ca20c065cdb283dafb361f1ad983a943b893d7f1b9f671437`
- 스키마 SHA-256: `ff0034796ba1e89c2e3b73f36422be321c642d2e601953b2d1f2f631ed4c0701`

strict 정확도: 21/25 (84.0%)
relaxed 정확도: 25/25 (100.0%)
평균 LLM 지연(실패 시도 포함): 3910.4 ms
총 토큰: 72721 (prompt=65843, completion=6878, usage 수신=25/25)

정답 SQL은 모두 검증기를 통과했고 nl_reader로 실행해 1~100행을 반환했습니다. 문항마다 REPEATABLE READ 트랜잭션 안에서 정답과 생성 SQL을 비교했습니다. 문항·모델 사이에는 실시간 mart 갱신이 있을 수 있습니다.

컬럼명은 무시하고 숫자 타입 값은 HALF_UP 소수 둘째 자리로 반올림합니다. 기본 비교는 행 집합이며, 정답 SQL 최상위에 ORDER BY와 LIMIT/FETCH가 함께 있으면 행 순서도 비교합니다. 100행 초과 결과는 통과시키지 않습니다.

strict는 컬럼 수와 위치도 일치해야 합니다. relaxed는 정답 각 컬럼을 생성 결과의 서로 다른 컬럼에 대응시킨 뒤 투영한 전체 행을 동일한 행 비교 규칙으로 비교합니다. 보조 컬럼과 컬럼 순서는 허용하지만, 값을 고르거나 행 사이의 컬럼 관계를 바꾸지는 않습니다.

API 실패도 두 정확도 분모에 포함합니다. usage 없는 호출의 토큰은 집계하지 않으며, 이런 모델의 품질·비용은 이 결과만으로 비교할 수 없습니다.

## 카테고리별 정확도

| 카테고리 | strict | relaxed |
|---|---:|---:|
| 단순 집계 | 4/4 (100.0%) | 4/4 (100.0%) |
| top-N | 3/3 (100.0%) | 3/3 (100.0%) |
| 시간 필터 | 3/3 (100.0%) | 3/3 (100.0%) |
| 날씨 | 1/3 (33.3%) | 3/3 (100.0%) |
| 지하철 시간대 | 2/3 (66.7%) | 3/3 (100.0%) |
| 부족 전환 | 3/3 (100.0%) | 3/3 (100.0%) |
| SCD 버전 | 3/3 (100.0%) | 3/3 (100.0%) |
| 조인 | 2/3 (66.7%) | 3/3 (100.0%) |

## 문항별 결과

| id | 카테고리 | strict | relaxed | LLM ms | prompt | completion |
|---|---|---|---|---:|---:|---:|
| agg01 | 단순 집계 | 일치 | 일치 | 2861 | 2610 | 98 |
| agg02 | 단순 집계 | 일치 | 일치 | 2013 | 2618 | 99 |
| agg03 | 단순 집계 | 일치 | 일치 | 2942 | 2618 | 187 |
| agg04 | 단순 집계 | 일치 | 일치 | 3118 | 2615 | 162 |
| top01 | top-N | 일치 | 일치 | 4325 | 2662 | 319 |
| top02 | top-N | 일치 | 일치 | 2946 | 2638 | 156 |
| top03 | top-N | 일치 | 일치 | 4030 | 2644 | 296 |
| time01 | 시간 필터 | 일치 | 일치 | 3072 | 2622 | 189 |
| time02 | 시간 필터 | 일치 | 일치 | 2915 | 2622 | 167 |
| time03 | 시간 필터 | 일치 | 일치 | 2297 | 2629 | 125 |
| weather01 | 날씨 | 일치 | 일치 | 5195 | 2630 | 398 |
| weather02 | 날씨 | 결과 불일치 | 일치 | 7307 | 2624 | 630 |
| weather03 | 날씨 | 결과 불일치 | 일치 | 6422 | 2619 | 496 |
| subway01 | 지하철 시간대 | 일치 | 일치 | 2454 | 2652 | 184 |
| subway02 | 지하철 시간대 | 일치 | 일치 | 2525 | 2623 | 141 |
| subway03 | 지하철 시간대 | 결과 불일치 | 일치 | 3286 | 2621 | 213 |
| transition01 | 부족 전환 | 일치 | 일치 | 2538 | 2661 | 180 |
| transition02 | 부족 전환 | 일치 | 일치 | 3643 | 2658 | 238 |
| transition03 | 부족 전환 | 일치 | 일치 | 2800 | 2635 | 171 |
| scd01 | SCD 버전 | 일치 | 일치 | 2518 | 2614 | 120 |
| scd02 | SCD 버전 | 일치 | 일치 | 3915 | 2657 | 247 |
| scd03 | SCD 버전 | 일치 | 일치 | 2690 | 2649 | 171 |
| join01 | 조인 | 일치 | 일치 | 4594 | 2628 | 341 |
| join02 | 조인 | 결과 불일치 | 일치 | 13068 | 2629 | 1204 |
| join03 | 조인 | 일치 | 일치 | 4287 | 2665 | 346 |

## 검증 Lineage

결과 행은 보관하지 않고 각 비교 시점의 원시 결과 해시와 데이터 기준 시각만 남깁니다. 해시는 결과 순서·숫자 표기에도 영향을 받으며 정확도 판정에는 위 정규화 규칙을 사용합니다.

| id | 정답 결과 SHA-256 | 생성 결과 SHA-256 | 데이터 기준 시각(KST) |
|---|---|---|---|
| agg01 | dc1cd32f19b2660b5cea6ce6d5a9f745e9d22a2b8937f2b2bdeadd1b163422c1 | dc1cd32f19b2660b5cea6ce6d5a9f745e9d22a2b8937f2b2bdeadd1b163422c1 | 2026-11-08T01:20:01 |
| agg02 | 0147418d12ed86713631ad02a7c36c96804611d713e04d88d68b9f7bf9ac8932 | 0147418d12ed86713631ad02a7c36c96804611d713e04d88d68b9f7bf9ac8932 | 2026-11-08T01:20:01 |
| agg03 | 5b41f8e5d32e0f9db9e434e88f64991a51a43d1ec628156a0dc5e6f1d47a153b | 5b41f8e5d32e0f9db9e434e88f64991a51a43d1ec628156a0dc5e6f1d47a153b | 2026-11-08T01:20:01 |
| agg04 | 50770e647bfd881a1d20aabb7c2a88da2ca70bdd87d494ae73c96f90272ff092 | 50770e647bfd881a1d20aabb7c2a88da2ca70bdd87d494ae73c96f90272ff092 | 2026-11-08T01:20:01 |
| top01 | e56430d75e12af6bc09e109e7228b1dd032743e664b09bb9dd803544a5dfc814 | e56430d75e12af6bc09e109e7228b1dd032743e664b09bb9dd803544a5dfc814 | 2026-11-08T01:20:01 |
| top02 | e0d3e80bea3c487a95b2097b186e5ed9eb665f53502fa12322925bbc119ccc68 | e0d3e80bea3c487a95b2097b186e5ed9eb665f53502fa12322925bbc119ccc68 | 2026-11-08T01:20:01 |
| top03 | 2f5779f18cef285fb3677e77a8fc8b632e70319c326ed36cc7464add7a75660d | 2f5779f18cef285fb3677e77a8fc8b632e70319c326ed36cc7464add7a75660d | 2026-11-08T01:20:01 |
| time01 | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | 2026-11-08T01:20:01 |
| time02 | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | 2026-11-08T01:20:01 |
| time03 | 1219f18433f40f7b82d39daa65b703b82642843f81419948623c171bbedd3496 | 1219f18433f40f7b82d39daa65b703b82642843f81419948623c171bbedd3496 | collected_at 없음/실행 실패 |
| weather01 | 0f4c3b72d4678c0bd6bc03e2d29ebcda18b493e29f8186c21ec122233c90fe70 | 0f4c3b72d4678c0bd6bc03e2d29ebcda18b493e29f8186c21ec122233c90fe70 | 2026-11-08T01:20:01 |
| weather02 | 6ca322c038b651dd7f7df8c9ebff4dc1f00de8e6bf72c51db37550e2a88c3cd6 | 092d29a41ce5c38bccfb2ee8d482ce6d1292ca135c47dc05a27cffecea427a07 | 2026-11-08T01:20:01 |
| weather03 | 0147418d12ed86713631ad02a7c36c96804611d713e04d88d68b9f7bf9ac8932 | 1d9535423d5b865790e4f22cdfe7ae93a1857c0b62315939740776f1ee1ea5d5 | 2026-11-08T01:20:01 |
| subway01 | ce953bfed9e4880a800ad3fa622a58748c51f2eea79ca4dc91989d9e1ddf7762 | ce953bfed9e4880a800ad3fa622a58748c51f2eea79ca4dc91989d9e1ddf7762 | collected_at 없음/실행 실패 |
| subway02 | 16cd2ad382a446a49ff10bf8a3a26739275b0c3702c70cbc83a98e250bfdc9b8 | 16cd2ad382a446a49ff10bf8a3a26739275b0c3702c70cbc83a98e250bfdc9b8 | collected_at 없음/실행 실패 |
| subway03 | b092013fa76354677e373ebb04a189e85129370794f4e83e4c03bff59744ea5c | b5f2e7625de50b4510adb7dd73312bd0a72c740b6c6230a8d1df525e9804b72d | collected_at 없음/실행 실패 |
| transition01 | c00ab9aba43d1c99bf2786e95559bd0776efccca076cb0f4250d661e9b85b96f | c00ab9aba43d1c99bf2786e95559bd0776efccca076cb0f4250d661e9b85b96f | collected_at 없음/실행 실패 |
| transition02 | 31bf252179e689d4efaeaa8ea71b5f5c95f73be51a92b92e19b5eb863cedd8fd | 31bf252179e689d4efaeaa8ea71b5f5c95f73be51a92b92e19b5eb863cedd8fd | collected_at 없음/실행 실패 |
| transition03 | eb91a955f1e651e7625fe2f2df850596a4a829157501b59e103ccdab4d22a8f4 | eb91a955f1e651e7625fe2f2df850596a4a829157501b59e103ccdab4d22a8f4 | collected_at 없음/실행 실패 |
| scd01 | a07f313fe389722744c2ae0bce79f0b07e99de7bec953968d2ef08054d47b7ba | a07f313fe389722744c2ae0bce79f0b07e99de7bec953968d2ef08054d47b7ba | collected_at 없음/실행 실패 |
| scd02 | fd2b4eb9b72ed63b8652a8e62b2aef87d45cb9143c694d3c4afb14551f320376 | fd2b4eb9b72ed63b8652a8e62b2aef87d45cb9143c694d3c4afb14551f320376 | collected_at 없음/실행 실패 |
| scd03 | ea2d021e1b0bcac3ba04ad99111fc3d5526943ecdc142d7be9b4c6ea348f45b1 | ea2d021e1b0bcac3ba04ad99111fc3d5526943ecdc142d7be9b4c6ea348f45b1 | collected_at 없음/실행 실패 |
| join01 | 0147418d12ed86713631ad02a7c36c96804611d713e04d88d68b9f7bf9ac8932 | 0147418d12ed86713631ad02a7c36c96804611d713e04d88d68b9f7bf9ac8932 | 2026-11-08T01:20:01 |
| join02 | 04ab8d68c27178fc76fe7545109995769cfc43ef412995093e3288e281542b23 | 1298e6c4210c980b8e9833e67fce535d95896319068082a51ca339f0ee763dd1 | 2026-11-08T01:20:01 |
| join03 | dd39ed4a242be10c2c35873ad8dec3de981d65d18c066fb0a7902c336cc78682 | dd39ed4a242be10c2c35873ad8dec3de981d65d18c066fb0a7902c336cc78682 | 2026-11-08T01:20:01 |

## 실패 SQL

### weather02: strict=결과 불일치, relaxed=일치

```sql
SELECT max(collected_at) AS snapshot_collected_at, round(avg(shared)::numeric, 2) AS avg_shared, count(*) AS observed_depleted_stations FROM public.mart_depletion_with_weather WHERE collected_at = (SELECT max(collected_at) FROM public.mart_depletion_with_weather);
```

### weather03: strict=결과 불일치, relaxed=일치

```sql
WITH latest AS (SELECT max(collected_at) AS collected_at FROM public.mart_depletion_with_weather) SELECT l.collected_at AS snapshot_collected_at, count(d.*) AS rows_precip_type_0 FROM latest l LEFT JOIN public.mart_depletion_with_weather d ON d.collected_at = l.collected_at AND d.precip_type = 0 GROUP BY l.collected_at;
```

### subway03: strict=결과 불일치, relaxed=일치

```sql
SELECT round(avg(depletion_rate)::numeric, 2) AS avg_depletion_rate, count(*) AS observed_rows FROM public.mart_subway_rush_depletion WHERE hour_of_day = 7;
```

### join02: strict=결과 불일치, relaxed=일치

```sql
WITH current_run AS (SELECT run_id FROM public.mart_station_snapshot WHERE run_id IS NOT NULL ORDER BY collected_at DESC NULLS LAST, run_id DESC LIMIT 1), matched AS (SELECT 1 FROM public.mart_station_snapshot s JOIN current_run r ON s.run_id = r.run_id JOIN public.mart_congestion_alert c ON c.station_id = s.station_id AND c.run_id = s.run_id) SELECT (SELECT run_id FROM current_run) AS current_run_id, count(*) AS matched_rows FROM matched;
```
