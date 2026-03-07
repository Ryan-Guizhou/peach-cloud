FROM eclipse-temurin:8-jdk-alpine

WORKDIR /app

# 设置时区
RUN apk add --no-cache tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo 'Asia/Shanghai' > /etc/timezone

# 复制启动脚本
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

# 从宿主机复制 JAR 文件
COPY target/peach-gateway-launch-1.0.0-SNAPSHOT.jar /app/app.jar

VOLUME ["/app/logs"]

EXPOSE 18080

ENTRYPOINT ["sh", "/app/start.sh"]