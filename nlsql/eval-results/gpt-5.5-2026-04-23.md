# gpt-5.5-2026-04-23 평가

- 실행 시각(UTC): 2026-11-07T16:12:18.252001Z ~ 2026-11-07T16:14:03.928399Z
- 코드 기준 커밋: `821d2ee09f56973ed473420e2e7a6fdb24c799d9`
- 평가셋 SHA-256: `c4b959915e815cd91c92ac3db5876fd61adf0ae0b637270bc419d5db6a201fec`
- 용어 사전 SHA-256: `2595d126aee53b2ca20c065cdb283dafb361f1ad983a943b893d7f1b9f671437`
- 스키마 SHA-256: `ff0034796ba1e89c2e3b73f36422be321c642d2e601953b2d1f2f631ed4c0701`

전체 정확도: 22/25 (88.0%)
평균 LLM 지연(실패 시도 포함): 3711.0 ms
총 토큰: 72497 (prompt=65867, completion=6630, usage 수신=25/25)

정답 SQL은 모두 검증기를 통과했고 nl_reader로 실행해 1~100행을 반환했습니다. 문항마다 REPEATABLE READ 트랜잭션 안에서 정답과 생성 SQL을 비교했습니다. 문항·모델 사이에는 실시간 mart 갱신이 있을 수 있습니다.

컬럼명은 무시하고 숫자 타입 값은 HALF_UP 소수 둘째 자리로 반올림합니다. 기본 비교는 행 집합이며, 정답 SQL 최상위에 ORDER BY와 LIMIT/FETCH가 함께 있으면 행 순서도 비교합니다. 100행 초과 결과는 통과시키지 않습니다.

API 실패도 전체 정확도 분모에 포함합니다. usage 없는 호출의 토큰은 집계하지 않으며, 이런 모델의 품질·비용은 이 결과만으로 비교할 수 없습니다.

## 카테고리별 정확도

| 카테고리 | 일치/전체 | 정확도 |
|---|---:|---:|
| 단순 집계 | 3/4 | 75.0% |
| top-N | 3/3 | 100.0% |
| 시간 필터 | 3/3 | 100.0% |
| 날씨 | 2/3 | 66.7% |
| 지하철 시간대 | 2/3 | 66.7% |
| 부족 전환 | 3/3 | 100.0% |
| SCD 버전 | 3/3 | 100.0% |
| 조인 | 3/3 | 100.0% |

## 문항별 결과

| id | 카테고리 | 결과 | LLM ms | prompt | completion |
|---|---|---|---:|---:|---:|
| agg01 | 단순 집계 | 결과 불일치 | 7096 | 2619 | 643 |
| agg02 | 단순 집계 | 일치 | 4150 | 2627 | 317 |
| agg03 | 단순 집계 | 일치 | 2730 | 2618 | 177 |
| agg04 | 단순 집계 | 일치 | 3158 | 2624 | 220 |
| top01 | top-N | 일치 | 3231 | 2643 | 225 |
| top02 | top-N | 일치 | 6499 | 2647 | 590 |
| top03 | top-N | 일치 | 3859 | 2644 | 241 |
| time01 | 시간 필터 | 일치 | 3325 | 2622 | 203 |
| time02 | 시간 필터 | 일치 | 2657 | 2622 | 193 |
| time03 | 시간 필터 | 일치 | 3081 | 2629 | 147 |
| weather01 | 날씨 | 일치 | 4198 | 2631 | 333 |
| weather02 | 날씨 | 결과 불일치 | 4468 | 2625 | 281 |
| weather03 | 날씨 | 일치 | 2918 | 2620 | 187 |
| subway01 | 지하철 시간대 | 일치 | 2736 | 2646 | 168 |
| subway02 | 지하철 시간대 | 일치 | 2843 | 2623 | 169 |
| subway03 | 지하철 시간대 | 결과 불일치 | 3867 | 2621 | 251 |
| transition01 | 부족 전환 | 일치 | 2319 | 2631 | 132 |
| transition02 | 부족 전환 | 일치 | 3758 | 2658 | 252 |
| transition03 | 부족 전환 | 일치 | 2428 | 2635 | 172 |
| scd01 | SCD 버전 | 일치 | 2240 | 2614 | 122 |
| scd02 | SCD 버전 | 일치 | 3140 | 2657 | 212 |
| scd03 | SCD 버전 | 일치 | 3481 | 2649 | 170 |
| join01 | 조인 | 일치 | 4994 | 2643 | 432 |
| join02 | 조인 | 일치 | 3923 | 2642 | 346 |
| join03 | 조인 | 일치 | 5675 | 2677 | 447 |

## 검증 Lineage

결과 행은 보관하지 않고 각 비교 시점의 원시 결과 해시와 데이터 기준 시각만 남깁니다. 해시는 결과 순서·숫자 표기에도 영향을 받으며 정확도 판정에는 위 정규화 규칙을 사용합니다.

| id | 정답 결과 SHA-256 | 생성 결과 SHA-256 | 데이터 기준 시각(KST) |
|---|---|---|---|
| agg01 | dc1cd32f19b2660b5cea6ce6d5a9f745e9d22a2b8937f2b2bdeadd1b163422c1 | 20a7f57b96ea604c6a3186e18cbaaa2828481affae99da7b7715e819d848c8b1 | 2026-11-08T01:10:02 |
| agg02 | f01ae0ccbbba34725f8b2a48c95d311a89ec092eea3df1005e53e6a3be5a643c | f01ae0ccbbba34725f8b2a48c95d311a89ec092eea3df1005e53e6a3be5a643c | 2026-11-08T01:10:02 |
| agg03 | 5b41f8e5d32e0f9db9e434e88f64991a51a43d1ec628156a0dc5e6f1d47a153b | 5b41f8e5d32e0f9db9e434e88f64991a51a43d1ec628156a0dc5e6f1d47a153b | 2026-11-08T01:10:02 |
| agg04 | 07f8fc8c87161eeec59fc44726e42a4147c084dd639a55da3a6ccfcc3e462058 | 07f8fc8c87161eeec59fc44726e42a4147c084dd639a55da3a6ccfcc3e462058 | 2026-11-08T01:10:02 |
| top01 | 9d73c30af6733c3a0d250ea766fe2e914f88713e417371936b07d5e2d38c60ba | 9d73c30af6733c3a0d250ea766fe2e914f88713e417371936b07d5e2d38c60ba | 2026-11-08T01:10:02 |
| top02 | daf00e8f854dea7e296babc9e27f146ad43661239a88fa5a7db35833e51ab880 | daf00e8f854dea7e296babc9e27f146ad43661239a88fa5a7db35833e51ab880 | 2026-11-08T01:10:02 |
| top03 | 2f5779f18cef285fb3677e77a8fc8b632e70319c326ed36cc7464add7a75660d | 2f5779f18cef285fb3677e77a8fc8b632e70319c326ed36cc7464add7a75660d | 2026-11-08T01:10:02 |
| time01 | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | 2026-11-08T01:10:02 |
| time02 | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | aa6885079e7ed8ca6ff962f5c1f6d44f5dd0ba9e476e9158ae65d99a02203c3b | 2026-11-08T01:10:02 |
| time03 | 1219f18433f40f7b82d39daa65b703b82642843f81419948623c171bbedd3496 | 1219f18433f40f7b82d39daa65b703b82642843f81419948623c171bbedd3496 | collected_at 없음/실행 실패 |
| weather01 | 7c19e7cd12a63c14f614a6b730c24c79be387c1a4758117a71e901c7394f64aa | 7c19e7cd12a63c14f614a6b730c24c79be387c1a4758117a71e901c7394f64aa | 2026-11-08T01:10:02 |
| weather02 | c22dbe18ae262c237c10540d4496f61988283ba63e3b268cd941252f7514b04e | 6c06a1b7b1ec692d44908e4d0b3f80b9f45f19f9a1d19253aa48cc385311a913 | 2026-11-08T01:10:02 |
| weather03 | f01ae0ccbbba34725f8b2a48c95d311a89ec092eea3df1005e53e6a3be5a643c | f01ae0ccbbba34725f8b2a48c95d311a89ec092eea3df1005e53e6a3be5a643c | 2026-11-08T01:10:02 |
| subway01 | c6800c11c54b6164018fb0cca8faa9c068c537a9682edf07eba911b2bd53202e | c6800c11c54b6164018fb0cca8faa9c068c537a9682edf07eba911b2bd53202e | collected_at 없음/실행 실패 |
| subway02 | 16cd2ad382a446a49ff10bf8a3a26739275b0c3702c70cbc83a98e250bfdc9b8 | 16cd2ad382a446a49ff10bf8a3a26739275b0c3702c70cbc83a98e250bfdc9b8 | collected_at 없음/실행 실패 |
| subway03 | b092013fa76354677e373ebb04a189e85129370794f4e83e4c03bff59744ea5c | b5f2e7625de50b4510adb7dd73312bd0a72c740b6c6230a8d1df525e9804b72d | collected_at 없음/실행 실패 |
| transition01 | f4c7b28586db06b84cfb8595a90c29fc40809598f990ee70259d5bf08919a958 | f4c7b28586db06b84cfb8595a90c29fc40809598f990ee70259d5bf08919a958 | collected_at 없음/실행 실패 |
| transition02 | 31bf252179e689d4efaeaa8ea71b5f5c95f73be51a92b92e19b5eb863cedd8fd | 31bf252179e689d4efaeaa8ea71b5f5c95f73be51a92b92e19b5eb863cedd8fd | collected_at 없음/실행 실패 |
| transition03 | eb91a955f1e651e7625fe2f2df850596a4a829157501b59e103ccdab4d22a8f4 | eb91a955f1e651e7625fe2f2df850596a4a829157501b59e103ccdab4d22a8f4 | collected_at 없음/실행 실패 |
| scd01 | a07f313fe389722744c2ae0bce79f0b07e99de7bec953968d2ef08054d47b7ba | a07f313fe389722744c2ae0bce79f0b07e99de7bec953968d2ef08054d47b7ba | collected_at 없음/실행 실패 |
| scd02 | fd2b4eb9b72ed63b8652a8e62b2aef87d45cb9143c694d3c4afb14551f320376 | fd2b4eb9b72ed63b8652a8e62b2aef87d45cb9143c694d3c4afb14551f320376 | collected_at 없음/실행 실패 |
| scd03 | ea2d021e1b0bcac3ba04ad99111fc3d5526943ecdc142d7be9b4c6ea348f45b1 | ea2d021e1b0bcac3ba04ad99111fc3d5526943ecdc142d7be9b4c6ea348f45b1 | collected_at 없음/실행 실패 |
| join01 | f01ae0ccbbba34725f8b2a48c95d311a89ec092eea3df1005e53e6a3be5a643c | f01ae0ccbbba34725f8b2a48c95d311a89ec092eea3df1005e53e6a3be5a643c | 2026-11-08T01:10:02 |
| join02 | bafaf64e74288e9c046ad77ff2c072b00fb0ca2357585e81003b5f5b70db06d1 | bafaf64e74288e9c046ad77ff2c072b00fb0ca2357585e81003b5f5b70db06d1 | 2026-11-08T01:10:02 |
| join03 | 9e098e9bd3790ce0cbffa0486b7df42d70529951087992d5c8204e95696bc4ff | 9e098e9bd3790ce0cbffa0486b7df42d70529951087992d5c8204e95696bc4ff | 2026-11-08T01:10:02 |

## 실패 SQL

### agg01: 결과 불일치

```sql
SELECT collected_at, count(*) AS total_stations FROM public.mart_station_snapshot WHERE collected_at >= timestamp '2026-11-08 00:00:00' AND collected_at < timestamp '2026-11-09 00:00:00' GROUP BY collected_at ORDER BY collected_at LIMIT 100;
```

### weather02: 결과 불일치

```sql
SELECT round(avg(shared), 2) AS avg_shared, count(*) AS observed_rows FROM public.mart_depletion_with_weather WHERE collected_at >= TIMESTAMP '2026-11-08 00:00:00' AND collected_at < TIMESTAMP '2026-11-09 00:00:00';
```

### subway03: 결과 불일치

```sql
SELECT round(avg(depletion_rate)::numeric, 2) AS avg_depletion_rate, count(*) AS observed_rows FROM public.mart_subway_rush_depletion WHERE hour_of_day = 7;
```

