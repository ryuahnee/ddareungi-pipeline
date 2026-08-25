from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.bash import BashOperator


JAR = "/opt/jakdang/ddareungi-batch.jar"

default_args = {
    "owner": "ahn",
    "retries": 2,
    "retry_delay": timedelta(minutes=3),
}

with DAG(
    dag_id="ddareungi_pipeline",
    default_args=default_args,
    schedule_interval="*/10 * * * *",
    start_date=datetime(2026, 6, 8),
    catchup=False,
    max_active_runs=1,
    tags=["ddareungi"],
) as dag:

    collect = BashOperator(
        task_id="ddareungiRealtimeSync",
        bash_command=f"java -jar {JAR} --job=ddareungiRealtimeSync --run-id={{{{ run_id }}}}",
    )

    mart_realtime = BashOperator(
        task_id="martRealtimeSync",
        bash_command=f"java -jar {JAR} --job=martRealtimeSync --run-id={{{{ run_id }}}}",
    )

    collect >> mart_realtime
