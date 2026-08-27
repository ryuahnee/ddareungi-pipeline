from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.bash import BashOperator

JAR = "/opt/jakdang/ddareungi-batch.jar"

default_args = {
    "owner": "ahn",
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}

# 지하철역 마스터는 거의 변하지 않으므로 매월 1일 1회 실행.
# subwayStationSync: 역 수집 + 따릉이 대여소-역 반경 500m 매핑 테이블 재계산
with DAG(
    dag_id="subway_pipeline",
    default_args=default_args,
    schedule_interval="0 3 1 * *",  # 매월 1일 03:00
    start_date=datetime(2026, 8, 1),
    catchup=False,
    max_active_runs=1,
    tags=["subway"],
) as dag:

    subway_station_sync = BashOperator(
        task_id="subwayStationSync",
        bash_command=f"java -jar {JAR} --job=subwayStationSync --run-id={{{{ run_id }}}}",
    )
