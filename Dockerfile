FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-pip python3-venv curl wget && rm -rf /var/lib/apt/lists/*
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
RUN pip install --no-cache-dir numpy==1.26.4 scikit-learn==1.4.2 pandas==2.2.2 requests==2.31.0

COPY --from=builder /app/target/*.jar app.jar
COPY oceangpt-frontend/ /app/static/

COPY model_predictor.py /app/model_predictor.py
COPY models/ /app/models/

RUN mkdir -p /app/logs /app/data /app/temp

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    SPRING_PROFILES_ACTIVE=prod \
    PYTHON_EXECUTABLE=/opt/venv/bin/python \
    PYTHON_SCRIPT_PATH=/app/model_predictor.py \
    MODEL_PATH=/app/models \
    OCEANGPT_MODEL_PATH=/app/models \
    KMP_DUPLICATE_LIB_OK=TRUE \
    STATIC_PATH=/app/static \
    PORT=10000

EXPOSE $PORT

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD sh -c "curl -fsS http://localhost:$PORT/api/actuator/health || curl -fsS http://localhost:$PORT/actuator/health || exit 1"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=$PORT -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -jar app.jar"]
