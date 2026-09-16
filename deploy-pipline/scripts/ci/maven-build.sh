#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd); ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd); REPO_ROOT=$(CDPATH= cd -- "$ROOT/.." && pwd); ENV_FILE=${PEACH_ENV_FILE:-$ROOT/runtime/generated/deploy.env}; CI_IMAGE=${MAVEN_IMAGE:-peach-ci/maven-node:3.9.11-eclipse-temurin-21-node22}; PROJECTS=${MAVEN_PROJECTS:-__ALL__}
[ -s "$ENV_FILE" ] || { echo "Missing deployment env: $ENV_FILE" >&2; exit 1; }; set -a; . "$ENV_FILE"; set +a
if [ -z "$PROJECTS" ]; then echo "No backend Maven projects selected; skipping Maven build."; exit 0; fi
MAVEN_SETTINGS_TEMPLATE=${MAVEN_SETTINGS_TEMPLATE:-$ROOT/config/maven/settings.xml}; MAVEN_SETTINGS_GENERATED=${MAVEN_SETTINGS_GENERATED:-/var/jenkins_home/.m2/settings.generated.xml}; MAVEN_LOCAL_REPOSITORY=${MAVEN_LOCAL_REPOSITORY:-/var/jenkins_home/.m2/repository}; MAVEN_NEXUS_URL=${MAVEN_NEXUS_URL:-http://nexus:8081}; export MAVEN_SETTINGS_TEMPLATE MAVEN_SETTINGS_GENERATED MAVEN_LOCAL_REPOSITORY MAVEN_NEXUS_URL
mkdir -p /var/jenkins_home/.m2; node "$ROOT/scripts/ci/render-maven-settings.mjs"; docker image inspect "$CI_IMAGE" >/dev/null
base_command='mvn -B -Pdocker -s /var/jenkins_home/.m2/settings.generated.xml -Dmaven.repo.local=/var/jenkins_home/.m2/repository -Dpeach.nexus.url=http://nexus:8081'
if [ "$PROJECTS" = "__ALL__" ]; then build_command="$base_command clean deploy"; else case "$PROJECTS" in *[!A-Za-z0-9,._/-]*) echo "Unsafe MAVEN_PROJECTS value: $PROJECTS" >&2; exit 2 ;; esac; build_command="$base_command -pl $PROJECTS -am clean deploy"; fi
echo "Maven build scope: $PROJECTS"
timeout 3600s docker run --rm --pull=never --entrypoint /bin/sh --network peach-devops --user "$(id -u):$(id -g)" -e HOME=/var/jenkins_home -e LANG=C.UTF-8 -e LC_ALL=C.UTF-8 -e MAVEN_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Daether.connector.connectTimeout=10000 -Daether.connector.requestTimeout=60000" -e JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8" -v peach-jenkins-data:/var/jenkins_home -w "$REPO_ROOT" "$CI_IMAGE" -lc "$build_command"
