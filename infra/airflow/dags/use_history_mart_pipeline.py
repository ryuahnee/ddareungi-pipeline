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
    dag_id="use_history_mart_pipeline",
    default_args=default_args,
    schedule_interval="0 1 5 * *",
    start_date=datetime(2026, 7, 7),
    catchup=False,
    max_active_runs=1,
    tags=["mart", "use"],
) as dag:

    # 과거 이용정보 mart 증분 (파라미터 없으면 전월~당월). 원장에 새 달 없으면 0건 멱등
    use_history_mart_sync = BashOperator(
        task_id="martUseHistorySync",
        bash_command=f"java -jar {JAR} --job=martUseHistorySync --run-id={{{{ run_id }}}}",
    )
