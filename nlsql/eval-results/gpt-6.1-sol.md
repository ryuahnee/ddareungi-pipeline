# gpt-6.1-sol 평가

- 실행 시각(UTC): 2026-11-07T16:27:33.203973Z ~ 2026-11-07T16:28:54.523277Z
- 코드 기준 커밋: `a99a5167c58e62ab09e407f6b086192ec28b9c7b`
- 평가셋 SHA-256: `3d00e653073a0a5e2ecb8ebea4129db499ca14de82a7eed9845873f42b8d5361`
- 용어 사전 SHA-256: `2595d126aee53b2ca20c065cdb283dafb361f1ad983a943b893d7f1b9f671437`
- 스키마 SHA-256: `ff0034796ba1e89c2e3b73f36422be321c642d2e601953b2d1f2f631ed4c0701`

strict 정확도: 23/25 (92.0%)
relaxed 정확도: 25/25 (100.0%)
평균 LLM 지연(실패 시도 포함): 2899.2 ms
총 토큰: 69177 (prompt=65843, completion=3334, usage 수신=25/25)

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
| 날씨 | 2/3 (66.7%) | 3/3 (100.0%) |
| 지하철 시간대 | 2/3 (66.7%) | 3/3 (100.0%) |
| 부족 전환 | 3/3 (100.0%) | 3/3 (100.0%) |
| SCD 버전 | 3/3 (100.0%) | 3/3 (100.0%) |
| 조인 | 3/3 (100.0%) | 3/3 (100.0%) |

## 문항별 결과

| id | 카테고리 | strict | relaxed | LLM ms | prompt | completion |
|---|---|---|---|---:|---:|---:|
| agg01 | 단순 집계 | 일치 | 일치 | 2508 | 2610 | 65 |
| agg02 | 단순 집계 | 일치 | 일치 | 2306 | 2618 | 75 |
| agg03 | 단순 집계 | 일치 | 일치 | 2391 | 2618 | 111 |
| agg04 | 단순 집계 | 일치 | 일치 | 2618 | 2615 | 85 |
| top01 | top-N | 일치 | 일치 | 4286 | 2662 | 231 |
| top02 | top-N | 일치 | 일치 | 2033 | 2638 | 92 |
| top03 | top-N | 일치 | 일치 | 2644 | 2644 | 162 |
| time01 | 시간 필터 | 일치 | 일치 | 2579 | 2622 | 113 |
| time02 | 시간 필터 | 일치 | 일치 | 2172 | 2622 | 118 |
| time03 | 시간 필터 | 일치 | 일치 | 1950 | 2629 | 85 |
| weather01 | 날씨 | 일치 | 일치 | 3718 | 2630 | 181 |
| weather02 | 날씨 | 결과 불일치 | 일치 | 7572 | 2624 | 333 |
| weather03 | 날씨 | 일치 | 일치 | 6059 | 2619 | 272 |
| subway01 | 지하철 시간대 | 일치 | 일치 | 2282 | 2652 | 121 |
| subway02 | 지하철 시간대 | 일치 | 일치 | 2380 | 2623 | 87 |
| subway03 | 지하철 시간대 | 결과 불일치 | 일치 | 2375 | 2621 | 105 |
| transition01 | 부족 전환 | 일치 | 일치 | 2536 | 2661 | 127 |
| transition02 | 부족 전환 | 일치 | 일치 | 2547 | 2658 | 143 |
| transition03 | 부족 전환 | 일치 | 일치 | 2621 | 2635 | 128 |
| scd01 | SCD 버전 | 일치 | 일치 | 1850 | 2614 | 73 |
| scd02 | SCD 버전 | 일치 | 일치 | 2297 | 2657 | 124 |
| scd03 | SCD 버전 | 일치 | 일치 | 2176 | 2649 | 108 |
| join01 | 조인 | 일치 | 일치 | 2168 | 2628 | 97 |
| join02 | 조인 | 일치 | 일치 | 3703 | 2629 | 159 |
| join03 | 조인 | 일치 | 일치 | 2709 | 2665 | 139 |

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
| weather02 | 6ca322c038b651dd7f7df8c9ebff4dc1f00de8e6bf72c51db37550e2a88c3cd6 | 9b730662ebc8f8f20b8e34515f6cf649042be217bc135133af3c649ce1f296bc | 2026-11-08T01:20:01 |
| weather03 | 0147418d12ed86713631ad02a7c36c96804611d713e04d88d68b9f7bf9ac8932 | 0147418d12ed86713631ad02a7c36c96804611d713e04d88d68b9f7bf9ac8932 | 2026-11-08T01:20:01 |
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
| join02 | 04ab8d68c27178fc76fe7545109995769cfc43ef412995093e3288e281542b23 | 04ab8d68c27178fc76fe7545109995769cfc43ef412995093e3288e281542b23 | 2026-11-08T01:20:01 |
| join03 | dd39ed4a242be10c2c35873ad8dec3de981d65d18c066fb0a7902c336cc78682 | dd39ed4a242be10c2c35873ad8dec3de981d65d18c066fb0a7902c336cc78682 | 2026-11-08T01:20:01 |

## 실패 SQL

### weather02: strict=결과 불일치, relaxed=일치

```sql
SELECT round(avg(w.shared), 2) AS avg_shared_pct, count(w.shared) AS observed_count, min(w.collected_at) AS first_collected_at_kst, max(w.collected_at) AS last_collected_at_kst FROM public.mart_depletion_with_weather AS w WHERE w.shared < 10 AND EXISTS (SELECT 1 FROM public.mart_station_snapshot AS s WHERE s.station_id = w.station_id AND s.run_id = w.run_id AND s.shared < 10);
```

### subway03: strict=결과 불일치, relaxed=일치

```sql
SELECT round(avg(depletion_rate)::numeric, 2) AS avg_depletion_rate_pct, count(depletion_rate) AS observed_rows FROM public.mart_subway_rush_depletion WHERE hour_of_day = 7;
```
