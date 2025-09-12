# OceanGPT海水水质监测系统 Docker镜像

# 多阶段构建 - 构建阶段
FROM ubuntu:22.04 as builder

# 设置环境变量
ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

# 安装系统依赖和构建工具
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    python3 \
    python3-pip \
    python3-venv \
    python3-dev \
    build-essential \
    gcc \
    g++ \
    gfortran \
    libopenblas-dev \
    liblapack-dev \
    pkg-config \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 创建Python虚拟环境
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# 升级pip和安装构建工具
RUN pip install --upgrade pip setuptools wheel

# 先安装PyTorch（CPU版本）
RUN pip install --no-cache-dir \
    torch==2.2.2+cpu \
    torchvision==0.17.2+cpu \
    --index-url https://download.pytorch.org/whl/cpu

# 安装其他Python依赖（使用更新的版本）
RUN pip install --no-cache-dir \
    numpy==1.26.4 \
    scikit-learn==1.4.2 \
    pandas==2.2.2 \
    transformers==4.36.0 \
    flask==3.0.0 \
    flask-cors==4.0.0 \
    requests==2.31.0

# 运行时阶段
FROM ubuntu:22.04 as runtime

# 设置环境变量
ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

# 安装运行时依赖
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    python3 \
    python3-venv \
    libopenblas0 \
    liblapack3 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 从构建阶段复制虚拟环境
COPY --from=builder /opt/venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# 复制JAR文件
COPY target/*.jar app.jar

# 复制前端静态文件
COPY oceangpt-frontend/ /app/static/

# 复制Python脚本和模型文件
COPY model_predictor.py /app/
COPY models/ /app/models/

# 创建必要的目录
RUN mkdir -p /app/logs /app/data

# 设置环境变量
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV PYTHON_PATH="/opt/venv/bin/python"
ENV MODEL_PATH="/app/models"
ENV STATIC_PATH="/app/static"
ENV LOG_PATH="/app/logs"

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# 启动命令
CMD ["java", "-jar", "app.jar"]
