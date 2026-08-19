FROM eclipse-temurin:21-jre-alpine



WORKDIR /app

RUN apk add --no-cache tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo 'Asia/Shanghai' > /etc/timezone

COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

COPY target/peach-scheduled-launch-1.0.0-SNAPSHOT.jar /app/app.jar

VOLUME ["/app/logs"]

EXPOSE 8088

ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["sh", "/app/start.sh"]
