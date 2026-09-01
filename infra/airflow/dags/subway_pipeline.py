from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.bash import BashOperator

JAR = "/opt/jakdang/ddareungi-batch.jar"

default_args = {
    "owner": "ahn",
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}

# 지하철역 마스터: 매월 1일 역 수집 + 매핑 재계산
# martSubwayRushDepletion: bike_status 4백만건 JOIN으로 ~6분 소요 → 매시 실행 불가, 일 1회로 분리
with DAG(
    dag_id="subway_pipeline",
    default_args=default_args,
    schedule_interval="0 4 * * *",  # 매일 04:00 (역 마스터는 월 1일, mart는 매일)
    start_date=datetime(2026, 8, 1),
    catchup=False,
    max_active_runs=1,
    tags=["subway"],
) as dag:

    subway_station_sync = BashOperator(
        task_id="subwayStationSync",
        bash_command=f"java -jar {JAR} --job=subwayStationSync --run-id={{{{ run_id }}}}",
    )

    mart_subway = BashOperator(
        task_id="martSubwayRushDepletion",
        bash_command=f"java -jar {JAR} --job=martSubwayRushDepletion --run-id={{{{ run_id }}}}",
    )

    subway_station_sync >> mart_subway
