# OceanGPT海水水质监测系统 Docker镜像

# 多阶段构建：构建阶段（使用 Maven 构建后端 Jar）
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制pom.xml和源代码
COPY pom.xml .
COPY src ./src

# 构建应用（跳过测试以加快构建速度）
RUN mvn -B -DskipTests package

# 运行阶段（使用 Debian 基底，兼容 glibc 与 Python wheels）
FROM eclipse-temurin:17-jre AS runtime

# 设置工作目录
WORKDIR /app

# 安装运行时依赖（含 Python）
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    python3-venv \
    curl \
    wget \
 && rm -rf /var/lib/apt/lists/*

# 创建 Python 虚拟环境
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# 安装轻量级 Python 依赖
RUN pip install --no-cache-dir \
    numpy==1.26.4 \
    scikit-learn==1.4.2 \
    pandas==2.2.2 \
    requests==2.31.0

# 复制后端 Jar
COPY --from=builder /app/target/*.jar app.jar

# 复制前端静态文件
COPY oceangpt-frontend/ /app/static/

# 复制 Python 脚本和模型目录
COPY src/main/resources/model_predictor.py /app/
COPY models/ /app/models/

# 创建必要的目录
RUN mkdir -p /app/logs /app/data /app/temp

# 环境变量（Render 友好）
ENV JAVA_OPTS="-Xmx350m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    SPRING_PROFILES_ACTIVE=prod \
    PYTHON_EXECUTABLE=/opt/venv/bin/python \
    PYTHON_SCRIPT_PATH=/app/model_predictor.py \
    MODEL_PATH=/app/models \
    OCEANGPT_MODEL_PATH=/app/models \
    KMP_DUPLICATE_LIB_OK=TRUE \
    STATIC_PATH=/app/static \
    PORT=10000

# 确保 python 命令可用
RUN ln -sf /opt/venv/bin/python /usr/bin/python

# 暴露端口（Render 使用 PORT 环境变量）
EXPOSE $PORT

# 健康检查：优先检测含有上下文路径的 Actuator，其次回退到无上下文路径
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD sh -c "curl -fsS http://localhost:$PORT/api/actuator/health || curl -fsS http://localhost:$PORT/actuator/health || exit 1"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -jar app.jar"]
