FROM node:22-bookworm AS node

FROM maven:3.9.11-eclipse-temurin-21

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    MAVEN_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=en -Duser.country=US" \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=en -Duser.country=US"

COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules

RUN set -eux; \
    ln -sf /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm; \
    ln -sf /usr/local/lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx; \
    node --version; \
    npm --version; \
    mvn --version

