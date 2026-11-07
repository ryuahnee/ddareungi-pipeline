# gpt-5.4-mini-2026-03-17 평가

- 실행 시각(UTC): 2026-11-07T16:43:45.130554Z ~ 2026-11-07T16:44:33.696359Z
- 코드 기준 커밋: `6b4e4701c7f36fea88193cf4bd320da50a8291c1`
- 평가셋 SHA-256: `3d00e653073a0a5e2ecb8ebea4129db499ca14de82a7eed9845873f42b8d5361`
- 용어 사전 SHA-256: `2595d126aee53b2ca20c065cdb283dafb361f1ad983a943b893d7f1b9f671437`
- 스키마 SHA-256: `ff0034796ba1e89c2e3b73f36422be321c642d2e601953b2d1f2f631ed4c0701`

strict 정확도: 25/25 (100.0%)
relaxed 정확도: 25/25 (100.0%)
평균 LLM 지연(실패 시도 포함): 1602.4 ms
총 토큰: 72075 (prompt=68568, completion=3507, usage 수신=25/25)

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
| 날씨 | 3/3 (100.0%) | 3/3 (100.0%) |
| 지하철 시간대 | 3/3 (100.0%) | 3/3 (100.0%) |
| 부족 전환 | 3/3 (100.0%) | 3/3 (100.0%) |
| SCD 버전 | 3/3 (100.0%) | 3/3 (100.0%) |
| 조인 | 3/3 (100.0%) | 3/3 (100.0%) |

## 문항별 결과

| id | 카테고리 | strict | relaxed | LLM ms | prompt | completion |
|---|---|---|---|---:|---:|---:|
| agg01 | 단순 집계 | 일치 | 일치 | 2272 | 2719 | 81 |
| agg02 | 단순 집계 | 일치 | 일치 | 1280 | 2727 | 103 |
| agg03 | 단순 집계 | 일치 | 일치 | 2404 | 2727 | 133 |
| agg04 | 단순 집계 | 일치 | 일치 | 1518 | 2724 | 93 |
| top01 | top-N | 일치 | 일치 | 2133 | 2771 | 189 |
| top02 | top-N | 일치 | 일치 | 1277 | 2747 | 119 |
| top03 | top-N | 일치 | 일치 | 2345 | 2753 | 183 |
| time01 | 시간 필터 | 일치 | 일치 | 1343 | 2731 | 138 |
| time02 | 시간 필터 | 일치 | 일치 | 1476 | 2731 | 146 |
| time03 | 시간 필터 | 일치 | 일치 | 1374 | 2738 | 119 |
| weather01 | 날씨 | 일치 | 일치 | 1673 | 2739 | 165 |
| weather02 | 날씨 | 일치 | 일치 | 1555 | 2733 | 137 |
| weather03 | 날씨 | 일치 | 일치 | 1163 | 2728 | 97 |
| subway01 | 지하철 시간대 | 일치 | 일치 | 1568 | 2761 | 175 |
| subway02 | 지하철 시간대 | 일치 | 일치 | 2022 | 2732 | 130 |
| subway03 | 지하철 시간대 | 일치 | 일치 | 1245 | 2730 | 122 |
| transition01 | 부족 전환 | 일치 | 일치 | 1529 | 2770 | 146 |
| transition02 | 부족 전환 | 일치 | 일치 | 1579 | 2767 | 182 |
| transition03 | 부족 전환 | 일치 | 일치 | 1707 | 2744 | 157 |
| scd01 | SCD 버전 | 일치 | 일치 | 1365 | 2723 | 93 |
| scd02 | SCD 버전 | 일치 | 일치 | 1324 | 2766 | 162 |
| scd03 | SCD 버전 | 일치 | 일치 | 1428 | 2758 | 154 |
| join01 | 조인 | 일치 | 일치 | 1337 | 2737 | 124 |
| join02 | 조인 | 일치 | 일치 | 1554 | 2738 | 170 |
| join03 | 조인 | 일치 | 일치 | 1588 | 2774 | 189 |

## 검증 Lineage

결과 행은 보관하지 않고 각 비교 시점의 원시 결과 해시와 데이터 기준 시각만 남깁니다. 해시는 결과 순서·숫자 표기에도 영향을 받으며 정확도 판정에는 위 정규화 규칙을 사용합니다.

| id | 정답 결과 SHA-256 | 생성 결과 SHA-256 | 데이터 기준 시각(KST) |
|---|---|---|---|
| agg01 | dc1cd32f19b2660b5cea6ce6d5a9f745e9d22a2b8937f2b2bdeadd1b163422c1 | dc1cd32f19b2660b5cea6ce6d5a9f745e9d22a2b8937f2b2bdeadd1b163422c1 | 2026-11-08T01:40:01 |
| agg02 | f65731e33222dbbb248617986c87a85738588de788bfb7a6e2a45fc76ad97a75 | f65731e33222dbbb248617986c87a85738588de788bfb7a6e2a45fc76ad97a75 | 2026-11-08T01:40:01 |
| agg03 | 5b41f8e5d32e0f9db9e434e88f64991a51a43d1ec628156a0dc5e6f1d47a153b | 5b41f8e5d32e0f9db9e434e88f64991a51a43d1ec628156a0dc5e6f1d47a153b | 2026-11-08T01:40:01 |
| agg04 | 80504ec9d7152de561a7fc66ea479d9371f181aba823fda822700edaff3e3be3 | 80504ec9d7152de561a7fc66ea479d9371f181aba823fda822700edaff3e3be3 | 2026-11-08T01:40:01 |
| top01 | e56430d75e12af6bc09e109e7228b1dd032743e664b09bb9dd803544a5dfc814 | e56430d75e12af6bc09e109e7228b1dd032743e664b09bb9dd803544a5dfc814 | 2026-11-08T01:40:01 |
| top02 | 78ba7e8d91352678aa2341699693a8774948359bd9d7ddc92458911c0356907d | 78ba7e8d91352678aa2341699693a8774948359bd9d7ddc92458911c0356907d | 2026-11-08T01:40:01 |
| top03 | 2f5779f18cef285fb3677e77a8fc8b632e70319c326ed36cc7464add7a75660d | 2f5779f18cef285fb3677e77a8fc8b632e70319c326ed36cc7464add7a75660d | 2026-11-08T01:40:01 |
| time01 | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | 2026-11-08T01:40:01 |
| time02 | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | 2026-11-08T01:40:01 |
| time03 | 1219f18433f40f7b82d39daa65b703b82642843f81419948623c171bbedd3496 | 1219f18433f40f7b82d39daa65b703b82642843f81419948623c171bbedd3496 | collected_at 없음/실행 실패 |
| weather01 | 83d073249f03cbd1c9070f9c69c2f61ed4a4ce42f260194c7f48ad1f71455a2d | 83d073249f03cbd1c9070f9c69c2f61ed4a4ce42f260194c7f48ad1f71455a2d | 2026-11-08T01:40:01 |
| weather02 | dc2d7c95afb1e1e90dec6f3a295ecce8e7189d55ec6f8feb86cf4072ed21145a | dc2d7c95afb1e1e90dec6f3a295ecce8e7189d55ec6f8feb86cf4072ed21145a | 2026-11-08T01:40:01 |
| weather03 | f65731e33222dbbb248617986c87a85738588de788bfb7a6e2a45fc76ad97a75 | f65731e33222dbbb248617986c87a85738588de788bfb7a6e2a45fc76ad97a75 | 2026-11-08T01:40:01 |
| subway01 | ce953bfed9e4880a800ad3fa622a58748c51f2eea79ca4dc91989d9e1ddf7762 | ce953bfed9e4880a800ad3fa622a58748c51f2eea79ca4dc91989d9e1ddf7762 | collected_at 없음/실행 실패 |
| subway02 | 16cd2ad382a446a49ff10bf8a3a26739275b0c3702c70cbc83a98e250bfdc9b8 | 16cd2ad382a446a49ff10bf8a3a26739275b0c3702c70cbc83a98e250bfdc9b8 | collected_at 없음/실행 실패 |
| subway03 | b092013fa76354677e373ebb04a189e85129370794f4e83e4c03bff59744ea5c | b092013fa76354677e373ebb04a189e85129370794f4e83e4c03bff59744ea5c | collected_at 없음/실행 실패 |
| transition01 | c00ab9aba43d1c99bf2786e95559bd0776efccca076cb0f4250d661e9b85b96f | c00ab9aba43d1c99bf2786e95559bd0776efccca076cb0f4250d661e9b85b96f | collected_at 없음/실행 실패 |
| transition02 | 31bf252179e689d4efaeaa8ea71b5f5c95f73be51a92b92e19b5eb863cedd8fd | 31bf252179e689d4efaeaa8ea71b5f5c95f73be51a92b92e19b5eb863cedd8fd | collected_at 없음/실행 실패 |
| transition03 | eb91a955f1e651e7625fe2f2df850596a4a829157501b59e103ccdab4d22a8f4 | eb91a955f1e651e7625fe2f2df850596a4a829157501b59e103ccdab4d22a8f4 | collected_at 없음/실행 실패 |
| scd01 | a07f313fe389722744c2ae0bce79f0b07e99de7bec953968d2ef08054d47b7ba | a07f313fe389722744c2ae0bce79f0b07e99de7bec953968d2ef08054d47b7ba | collected_at 없음/실행 실패 |
| scd02 | fd2b4eb9b72ed63b8652a8e62b2aef87d45cb9143c694d3c4afb14551f320376 | fd2b4eb9b72ed63b8652a8e62b2aef87d45cb9143c694d3c4afb14551f320376 | collected_at 없음/실행 실패 |
| scd03 | ea2d021e1b0bcac3ba04ad99111fc3d5526943ecdc142d7be9b4c6ea348f45b1 | ea2d021e1b0bcac3ba04ad99111fc3d5526943ecdc142d7be9b4c6ea348f45b1 | collected_at 없음/실행 실패 |
| join01 | f65731e33222dbbb248617986c87a85738588de788bfb7a6e2a45fc76ad97a75 | f65731e33222dbbb248617986c87a85738588de788bfb7a6e2a45fc76ad97a75 | 2026-11-08T01:40:01 |
| join02 | 9ce314b0bf73f400bc33c65d7333b4b9835542207e40e60a143de152709bd162 | 9ce314b0bf73f400bc33c65d7333b4b9835542207e40e60a143de152709bd162 | 2026-11-08T01:40:01 |
| join03 | 3cda277111201a445f400d1af7d4171a124bee5eb9fea624d825d912530aaf7f | 4fb8e471877f750c1ffeb8f88e3313b0aa44a596efa0a62244cada1c1947c6ff | 2026-11-08T01:40:01 |

## 실패 SQL
