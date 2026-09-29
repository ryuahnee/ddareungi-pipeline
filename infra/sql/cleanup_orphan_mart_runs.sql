-- mart 고아 run 정리 (일회성)
--
-- 배경: DuckDB → MySQL 원장 전환 이전(2026-06-06~06-24)에 Postgres로 직접 적재된 잔여분이
--       누적형 경보 mart에 남아 있었다. 이 run들은 원장 bike_status(2026-06-29 시작)에
--       대응 데이터가 없어 원천-mart 대조가 불가능한 고아 run이다.
--
-- 대상 (2026-09-29 실행 기준):
--   mart_depletion_alert  : 312 run / 112,071 행
--   mart_congestion_alert : 312 run / 349,667 행
--   (전체 교체형 snapshot·weather mart는 최신 run만 보관해 고아 없음)
--
-- 원장 bike_status는 절대 건드리지 않는다. anti-join으로 원장에 없는 run만 삭제.
-- 멱등: 재실행해도 원장에 없는 run이 없으면 0건 삭제.

-- 원장 run 목록을 임시 테이블로 적재 (호스트에서: mysql ... "SELECT DISTINCT run_id FROM bike_status" > src_runs.txt)
CREATE TEMP TABLE src_runs (run_id text);
\copy src_runs FROM '/tmp/src_runs.txt'
CREATE INDEX ON src_runs (run_id);

BEGIN;
DELETE FROM mart_depletion_alert  m WHERE NOT EXISTS (SELECT 1 FROM src_runs s WHERE s.run_id = m.run_id);
DELETE FROM mart_congestion_alert m WHERE NOT EXISTS (SELECT 1 FROM src_runs s WHERE s.run_id = m.run_id);
COMMIT;
