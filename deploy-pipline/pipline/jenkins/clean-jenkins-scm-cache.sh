#!/bin/sh

set -eu

JENKINS_HOME="${JENKINS_HOME:-/var/jenkins_home}"
WORKSPACE_ROOT="$JENKINS_HOME/workspace"
CACHE_DIR="$JENKINS_HOME/caches"

usage() {
  echo "Usage: clean-jenkins-scm-cache.sh <job-name>" >&2
  exit 1
}

[ "$#" -eq 1 ] || usage

job_name="$1"

case "$job_name" in
  ""|*..*|*/*)
    echo "Invalid job name: $job_name" >&2
    exit 1
    ;;
esac

clean_dir() {
  target="$1"
  case "$target" in
    "$WORKSPACE_ROOT"/*|"$CACHE_DIR")
      rm -rf "$target"
      ;;
    *)
      echo "Refusing to delete unexpected path: $target" >&2
      exit 1
      ;;
  esac
}

if [ -d "$CACHE_DIR" ]; then
  find "$CACHE_DIR" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
fi

clean_dir "$WORKSPACE_ROOT/$job_name"
clean_dir "$WORKSPACE_ROOT/$job_name@script"
clean_dir "$WORKSPACE_ROOT/$job_name@tmp"

echo "Cleared Jenkins SCM caches and workspace for job: $job_name"
