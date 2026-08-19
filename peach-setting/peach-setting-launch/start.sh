#!/bin/sh
set -eu

APP_JAR="${APP_JAR:-/app/app.jar}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-docker}"
DEFAULT_JAVA_OPTS="${DEFAULT_JAVA_OPTS:--XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom}"
JAVA_OPTS="${JAVA_OPTS:-}"

exec java ${DEFAULT_JAVA_OPTS} ${JAVA_OPTS} -jar "${APP_JAR}" --spring.profiles.active="${SPRING_PROFILES_ACTIVE}"