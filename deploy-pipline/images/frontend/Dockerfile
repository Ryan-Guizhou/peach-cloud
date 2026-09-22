FROM nginx:1.25-alpine

ARG IMAGE_AUTHORS
ARG IMAGE_VENDOR
ARG IMAGE_VERSION
ARG IMAGE_REVISION

LABEL org.opencontainers.image.title="Peach Front" \
      org.opencontainers.image.description="Peach Cloud frontend" \
      org.opencontainers.image.authors="${IMAGE_AUTHORS}" \
      org.opencontainers.image.vendor="${IMAGE_VENDOR}" \
      org.opencontainers.image.version="${IMAGE_VERSION}" \
      org.opencontainers.image.revision="${IMAGE_REVISION}"

COPY config/nginx/peach.conf.template /etc/nginx/templates/default.conf.template
COPY runtime/generated/front-dist/ /usr/share/nginx/html/
