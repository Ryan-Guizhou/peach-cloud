FROM openjdk:8-jre-alpine

LABEL maintainer="peach-cloud"

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.ustc.edu.cn/g' /etc/apk/repositories && \
    apk add --no-cache bash

WORKDIR /app

COPY target/peach-generator-launch-${revision}.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java","-jar","/app/app.jar","--spring.profiles.active=docker"]