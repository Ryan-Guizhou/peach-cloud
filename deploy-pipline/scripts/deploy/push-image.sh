#!/usr/bin/env sh
set -eu
service=${1:?Usage: push-image.sh <service> <image-tag>}; tag=${2:?Usage: push-image.sh <service> <image-tag>}; IMAGE_PREFIX=${IMAGE_PREFIX:-localhost:5000/peach-cloud}; docker push "$IMAGE_PREFIX/$service:$tag"
