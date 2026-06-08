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

    staging = BashOperator(
        task_id="stagingLoad",
        bash_command=f"java -jar {JAR} --job=stagingLoad --run-id={{{{ run_id }}}}",
    )

    mart_snapshot = BashOperator(
        task_id="martSnapshot",
        bash_command=f"java -jar {JAR} --job=martSnapshot --run-id={{{{ run_id }}}}",
    )

    mart_depletion = BashOperator(
        task_id="martDepletionAlert",
        bash_command=f"java -jar {JAR} --job=martDepletionAlert --run-id={{{{ run_id }}}}",
    )

    mart_congestion = BashOperator(
        task_id="martCongestionAlert",
        bash_command=f"java -jar {JAR} --job=martCongestionAlert --run-id={{{{ run_id }}}}",
    )

    mart_sync = BashOperator(
        task_id="martSync",
        bash_command=f"java -jar {JAR} --job=martSync --run-id={{{{ run_id }}}}",
    )

    collect >> staging >> mart_snapshot >> mart_depletion >> mart_congestion >> mart_sync
