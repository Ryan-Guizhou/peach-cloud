#!/bin/bash

# 启动脚本
export JAVA_OPTS="$JAVA_OPTS -Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=prod