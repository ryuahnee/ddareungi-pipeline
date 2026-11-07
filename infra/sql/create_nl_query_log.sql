-- 목적: Text-to-SQL 질문별 생성·검증·실행 지표를 저장한다. 결과 행과 chat_id는 저장하지 않는다.
-- 실행: docker exec -i docker-grafana-db-1 psql -U grafana -d grafana < infra/sql/create_nl_query_log.sql
\set ON_ERROR_STOP on
CREATE TABLE IF NOT EXISTS public.nl_query_log (
    id bigserial PRIMARY KEY,
    asked_at timestamptz DEFAULT now(),
    source text NOT NULL CHECK (source IN ('telegram', 'cli', 'eval')),
    question text NOT NULL,
    model text NOT NULL,
    generated_sql text,
    guard_passed boolean,
    reject_reason text,
    row_count int,
    truncated boolean,
    llm_latency_ms int,
    db_latency_ms int,
    prompt_tokens int,
    completion_tokens int,
    data_as_of timestamp,
    error text
);
-- nl_reader에게 로그 조회/쓰기 권한을 부여하지 않는다.
REVOKE ALL ON public.nl_query_log FROM PUBLIC, nl_reader;
REVOKE ALL ON SEQUENCE public.nl_query_log_id_seq FROM PUBLIC, nl_reader;
