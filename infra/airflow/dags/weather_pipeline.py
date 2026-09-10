from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.bash import BashOperator

JAR = "/opt/jakdang/ddareungi-batch.jar"

default_args = {
    "owner": "ahn",
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}

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
        bash_command=(
            f"java -jar {JAR} --job=asosBackfill "
            f"--date=$(TZ=Asia/Seoul date +%Y%m%d) --stn=108 "
            f"--run-id={{{{ run_id }}}}"
        ),
    )
