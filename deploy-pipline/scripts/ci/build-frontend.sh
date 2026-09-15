#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd); ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd); REPO_ROOT=$(CDPATH= cd -- "$ROOT/.." && pwd)
docker run --rm --user "$(id -u):$(id -g)" -e HOME=/tmp -v peach-jenkins-data:/var/jenkins_home -w "$REPO_ROOT/peach-cloud-front" node:22-alpine sh -c 'npm ci && npm run build'
rm -rf "$ROOT/runtime/generated/front-dist"; mkdir -p "$ROOT/runtime/generated/front-dist"; cp -R "$REPO_ROOT/peach-cloud-front/dist/." "$ROOT/runtime/generated/front-dist/"
