from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.bash import BashOperator

JAR = "/opt/jakdang/ddareungi-batch.jar"

default_args = {
    "owner": "ahn",
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}

# DuckDB 폐기 후: 날씨 실시간 수집을 MySQL(weather_asos)로 이전.
# 초단기실황(최근 1일치만 제공, DuckDB행) 대신 ASOS 최신 관측을 매시간 upsert.
# asosBackfill 잡이 해당일(00~23시) 전체를 idempotent(observed_at,stn)로 적재 →
# 매시간 실행 시 그날 데이터가 점진적으로 채워지고 누락도 자가복구.
# (기존 DuckDB 의존 마트 태스크는 제거 — 추후 Postgres 재배선에서 재구성)
with DAG(
    dag_id="weather_pipeline",
    default_args=default_args,
    schedule_interval="0 * * * *",
    start_date=datetime(2026, 6, 9),
    catchup=False,
    max_active_runs=1,
    tags=["weather"],
) as dag:

    asos_collect = BashOperator(
        task_id="asosCollect",
        # KST 기준 오늘 날짜의 ASOS 시간자료를 weather_asos에 적재 (서울 stn=108)
        bash_command=(
            f"java -jar {JAR} --job=asosBackfill "
            f"--date=$(TZ=Asia/Seoul date +%Y%m%d) --stn=108 "
            f"--run-id={{{{ run_id }}}}"
        ),
    )
