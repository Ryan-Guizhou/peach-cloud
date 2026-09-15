#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd); ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd); tag=${1:?Usage: build-frontend-image.sh <image-tag>}; IMAGE_PREFIX=${IMAGE_PREFIX:-localhost:5000/peach-cloud}
docker build --build-arg IMAGE_AUTHORS="${IMAGE_AUTHORS:-Mr Shu}" --build-arg IMAGE_VENDOR="${IMAGE_VENDOR:-Peach Cloud}" --build-arg IMAGE_VERSION="$tag" --build-arg IMAGE_REVISION="$tag" -f "$ROOT/images/frontend/Dockerfile" -t "$IMAGE_PREFIX/peach-front:$tag" "$ROOT"
