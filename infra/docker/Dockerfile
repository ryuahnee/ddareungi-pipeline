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
