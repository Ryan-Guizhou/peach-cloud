FROM eclipse-temurin:21-jre

ARG JAR_FILE
ARG IMAGE_TITLE
ARG IMAGE_DESCRIPTION
ARG IMAGE_AUTHORS
ARG IMAGE_VENDOR
ARG IMAGE_VERSION
ARG IMAGE_REVISION

LABEL org.opencontainers.image.title="${IMAGE_TITLE}" \
      org.opencontainers.image.description="${IMAGE_DESCRIPTION}" \
      org.opencontainers.image.authors="${IMAGE_AUTHORS}" \
      org.opencontainers.image.vendor="${IMAGE_VENDOR}" \
      org.opencontainers.image.version="${IMAGE_VERSION}" \
      org.opencontainers.image.revision="${IMAGE_REVISION}"

WORKDIR /app
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
