from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.bash import BashOperator

JAR = "/opt/jakdang/ddareungi-batch.jar"

default_args = {
    "owner": "ahn",
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}

# 부족 전환 mart는 하루 1회면 충분해 10분 파이프라인과 분리(불필요한 반복·실패 혼입 방지).
# 0 16 UTC = 01:00 KST. 전날(KST) 07·09시 스냅샷을 집계하므로 하루가 끝난 뒤 실행.
with DAG(
    dag_id="depletion_transition_pipeline",
    default_args=default_args,
    schedule_interval="0 16 * * *",
    start_date=datetime(2026, 6, 9),
    catchup=False,
    max_active_runs=1,
    tags=["ddareungi"],
) as dag:

    depletion_transition = BashOperator(
        task_id="martDepletionTransition",
        bash_command=(
            f"java -jar {JAR} --job=martDepletionTransition "
            f"--date=$(TZ=Asia/Seoul date -d yesterday +%Y-%m-%d) "
            f"--run-id={{{{ run_id }}}}"
        ),
    )
