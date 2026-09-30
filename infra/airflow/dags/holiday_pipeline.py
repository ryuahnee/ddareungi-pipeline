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
    dag_id="holiday_pipeline",
    default_args=default_args,
    schedule_interval="0 0 15 * *",
    start_date=datetime(2026, 6, 15),
    catchup=False,
    max_active_runs=1,
    tags=["holiday"],
) as dag:

    holiday_collect = BashOperator(
        task_id="holidayCollect",
        bash_command=(
            f"java -jar {JAR} --job=holidayCollect --run-id={{{{ run_id }}}}"
            " --year={{ (data_interval_end + macros.dateutil.relativedelta.relativedelta(months=1)).strftime('%Y') }}"
            " --month={{ (data_interval_end + macros.dateutil.relativedelta.relativedelta(months=1)).strftime('%m') }}"
        ),
    )
