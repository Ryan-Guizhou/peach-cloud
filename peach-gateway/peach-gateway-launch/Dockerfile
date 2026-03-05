FROM maven:3.9.9-eclipse-temurin-8 AS builder

WORKDIR /workspace
COPY . .

RUN mvn -pl peach-gateway/peach-gateway-launch -am clean package -DskipTests -Pdevelopment \
    && cp "$(find peach-gateway/peach-gateway-launch/target -maxdepth 1 -type f -name '*.jar' ! -name '*original*' | sed -n '1p')" /workspace/app.jar

FROM openjdk:8-jdk-alpine

WORKDIR /app
# 设置时区（Docker 镜像默认是 UTC 时间，改为北京时间以保证日志时间戳正确）
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone
COPY --from=builder /workspace/app.jar /app/app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m"

RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/logs \
    && chown -R app:app /app

VOLUME ["/app/logs"]

EXPOSE 18080

USER app
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
