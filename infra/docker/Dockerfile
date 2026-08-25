FROM apache/airflow:2.10.3
USER root
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget gnupg apt-transport-https && \
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /usr/share/keyrings/adoptium-keyring.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/adoptium-keyring.gpg] https://packages.adoptium.net/artifactory/deb bookworm main" > /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends temurin-21-jre && \
    apt-get clean && rm -rf /var/lib/apt/lists/*
USER airflow
# 대여소 군집화(clustering_pipeline) 의존성. duckdb는 JDBC 드라이버(1.1.3)와 포맷 호환 위해 버전 고정
RUN pip install --no-cache-dir "duckdb==1.1.3" "pandas" "scikit-learn"
