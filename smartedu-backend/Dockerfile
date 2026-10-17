# ============================================================
# SmartEdu-Platform 后端 Dockerfile
# ============================================================

# 基于 OpenJDK 17  slim 镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 安装必要的工具（用于健康检查）
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 创建上传目录
RUN mkdir -p /app/uploads

# 复制 Maven 构建的 JAR 文件
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# JVM 参数优化
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# 启动应用（使用环境变量）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
