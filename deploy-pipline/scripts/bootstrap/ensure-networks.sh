#!/usr/bin/env sh
set -eu
for network in peach-devops peach-cloud-runtime; do
  if docker network inspect "$network" >/dev/null 2>&1; then
    echo "[network] exists: $network"
  else
    docker network create "$network" >/dev/null
    echo "[network] created: $network"
  fi
done
