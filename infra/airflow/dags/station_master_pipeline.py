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
    dag_id="station_master_pipeline",
    default_args=default_args,
    schedule_interval="0 0 * * *",
    start_date=datetime(2026, 7, 6),
    catchup=False,
    max_active_runs=1,
    tags=["station"],
) as dag:

    station_master_sync = BashOperator(
        task_id="stationMasterSync",
        bash_command=f"java -jar {JAR} --job=stationMasterSync --run-id={{{{ run_id }}}}",
    )

    # SCD 갱신 직후 버전별 거치율 mart 재계산 (MySQL → Postgres)
    mart_station_scd_sync = BashOperator(
        task_id="martStationScdSync",
        bash_command=f"java -jar {JAR} --job=martStationScdSync --run-id={{{{ run_id }}}}",
    )

    station_master_sync >> mart_station_scd_sync
