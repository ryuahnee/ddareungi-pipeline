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
# 0 16 UTC = 01:00 KST. KST D일을 적재하는 run의 data_interval_end는 D일 16:00 UTC(= D+1일 01:00 KST),
# 따라서 data_interval_start는 D-1일 16:00 UTC(= D일 01:00 KST) → KST 변환 후 날짜가 곧 적재 대상일 D.
# 실행 시점(now)이 아닌 논리적 실행일 기준이라 과거 run 재실행 시에도 날짜가 정확하다.
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
            f"--date={{{{ data_interval_start.in_timezone('Asia/Seoul').strftime('%Y-%m-%d') }}}} "
            f"--run-id={{{{ run_id }}}}"
        ),
    )
