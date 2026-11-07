-- 목적: Text-to-SQL 전용 계정에 public.mart_* 테이블 읽기 권한만 부여한다.
-- 실행: infra/docker/.env의 NL_READER_PASSWORD를 환경변수로 읽은 뒤 실행한다.
-- docker exec -i docker-grafana-db-1 psql -U grafana -d grafana \
--   -v nl_password="$NL_READER_PASSWORD" < infra/sql/create_nl_readonly_role.sql
\set ON_ERROR_STOP on

BEGIN;
SELECT format('CREATE ROLE nl_reader LOGIN')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'nl_reader')
\gexec
SELECT format('ALTER ROLE nl_reader LOGIN PASSWORD %L', :'nl_password')
\gexec

GRANT CONNECT ON DATABASE grafana TO nl_reader;
GRANT USAGE ON SCHEMA public TO nl_reader;

DO $$
DECLARE
    mart_table record;
BEGIN
    FOR mart_table IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename LIKE 'mart\_%' ESCAPE '\'
    LOOP
        EXECUTE format('GRANT SELECT ON TABLE public.%I TO nl_reader', mart_table.tablename);
    END LOOP;
END
$$;

ALTER ROLE nl_reader SET default_transaction_read_only = on;
ALTER ROLE nl_reader SET statement_timeout = '5s';
COMMIT;
