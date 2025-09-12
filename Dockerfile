# OceanGPT海水水质监测系统 Docker镜像
# 多阶段构建：构建阶段
FROM maven:3.8.6-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制pom.xml和源代码
COPY pom.xml .
COPY src ./src

# 构建应用
RUN mvn clean package -DskipTests

# 运行阶段 - 使用Ubuntu而不是Alpine
FROM eclipse-temurin:17-jre AS runtime

# 设置工作目录
WORKDIR /app

# 更新包管理器并安装必要的系统依赖
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    python3 \
    python3-pip \
    python3-venv \
    python3-dev \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# 创建Python虚拟环境
RUN python3 -m venv /app/venv
ENV PATH="/app/venv/bin:$PATH"

# 升级pip
RUN pip install --upgrade pip

# 安装PyTorch CPU版本（适合云部署）
RUN pip install --no-cache-dir \
    torch==2.1.2+cpu \
    torchvision==0.16.2+cpu \
    --index-url https://download.pytorch.org/whl/cpu

# 安装其他Python依赖
RUN pip install --no-cache-dir \
    numpy==1.24.3 \
    scikit-learn==1.3.0 \
    pandas==2.0.3

# 从构建阶段复制JAR文件
COPY --from=builder /app/target/OceanGPT-Java-Deployment-*.jar app.jar

# 复制前端静态文件
COPY oceangpt-frontend /app/static

# 复制Python脚本和模型文件
COPY src/main/resources/model_predictor.py /app/
COPY models /app/models

# 创建必要的目录
RUN mkdir -p /app/logs /app/data /app/temp

# 设置环境变量（针对Render免费计划优化）
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE=prod
ENV PYTHON_EXECUTABLE=/app/venv/bin/python3
ENV MODEL_PATH=/app/models
ENV STATIC_PATH=/app/static
ENV PORT=10000

# 暴露端口（Render使用PORT环境变量）
EXPOSE $PORT

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:$PORT/api/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
