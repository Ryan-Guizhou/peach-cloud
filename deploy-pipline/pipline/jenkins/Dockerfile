FROM jenkins/jenkins:lts-jdk21

USER root

# ============================================================
# 换国内 APT 源（阿里云）
#
# Jenkins 官方镜像基于 Debian。
# 同时兼容新版 DEB822 格式和传统 sources.list。
# ============================================================
RUN set -eux; \
    if [ -f /etc/apt/sources.list.d/debian.sources ]; then \
        sed -i 's|http://deb.debian.org|https://mirrors.aliyun.com|g' \
            /etc/apt/sources.list.d/debian.sources; \
        sed -i 's|https://deb.debian.org|https://mirrors.aliyun.com|g' \
            /etc/apt/sources.list.d/debian.sources; \
        sed -i 's|http://security.debian.org|https://mirrors.aliyun.com/debian-security|g' \
            /etc/apt/sources.list.d/debian.sources; \
        sed -i 's|https://security.debian.org|https://mirrors.aliyun.com/debian-security|g' \
            /etc/apt/sources.list.d/debian.sources; \
    fi; \
    if [ -f /etc/apt/sources.list ]; then \
        sed -i 's|http://deb.debian.org|https://mirrors.aliyun.com|g' \
            /etc/apt/sources.list; \
        sed -i 's|https://deb.debian.org|https://mirrors.aliyun.com|g' \
            /etc/apt/sources.list; \
        sed -i 's|http://security.debian.org|https://mirrors.aliyun.com/debian-security|g' \
            /etc/apt/sources.list; \
        sed -i 's|https://security.debian.org|https://mirrors.aliyun.com/debian-security|g' \
            /etc/apt/sources.list; \
    fi; \
    apt-get update


# ============================================================
# 基础构建工具
#
# 包含：
# - Git
# - Maven
# - curl / wget
# - zip / unzip
# - jq
# - make / gcc / g++
#
# build-essential 用于后续 Python、Node、Go 项目中可能出现的
# Native 扩展或 CGO 编译。
# ============================================================
RUN set -eux; \
    apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        wget \
        gnupg \
        git \
        maven \
        jq \
        zip \
        unzip \
        tar \
        gzip \
        xz-utils \
        build-essential; \
    apt-get clean; \
    rm -rf /var/lib/apt/lists/*


# ============================================================
# Node.js
#
# 用于：
# - peach-cloud scripts/check-utf8.mjs
# - 前端项目构建
# - npm / npx 工具
#
# Node.js 和 npm 使用 Debian 官方软件包。
# ============================================================
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        nodejs \
        npm; \
    node --version; \
    npm --version; \
    apt-get clean; \
    rm -rf /var/lib/apt/lists/*


# ============================================================
# Python 3
#
# 用于：
# - CI 脚本
# - 自动化工具
# - Python 项目
#
# python3-venv:
#   推荐 Pipeline 使用虚拟环境安装项目依赖。
#
# python3-pip:
#   Python 包管理器。
#
# python-is-python3:
#   允许直接使用 python 命令，实际指向 Python 3。
# ============================================================
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        python3 \
        python3-pip \
        python3-venv \
        python3-dev \
        python-is-python3; \
    python3 --version; \
    python --version; \
    pip3 --version; \
    apt-get clean; \
    rm -rf /var/lib/apt/lists/*


# ============================================================
# Go
#
# 用于：
# - Go 服务构建
# - DevOps CLI 工具构建
# - Go Test
#
# 使用 Debian golang-go 包。
# ============================================================
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        golang-go; \
    go version; \
    apt-get clean; \
    rm -rf /var/lib/apt/lists/*


# ============================================================
# Docker CLI
#
# Jenkins 本身不运行 Docker Daemon。
#
# Docker CLI:
#     Jenkins Container
#            |
#            | DOCKER_HOST=tcp://docker:2376
#            v
#     Jenkins Docker DinD
#
# 安装：
# - docker-ce-cli
# - docker buildx
# - docker compose
# ============================================================
RUN set -eux; \
    install -m 0755 -d /etc/apt/keyrings; \
    curl -fsSL https://download.docker.com/linux/debian/gpg \
        -o /etc/apt/keyrings/docker.asc; \
    chmod a+r /etc/apt/keyrings/docker.asc; \
    echo \
        "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "${VERSION_CODENAME}") stable" \
        > /etc/apt/sources.list.d/docker.list; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        docker-ce-cli \
        docker-buildx-plugin \
        docker-compose-plugin; \
    docker --version; \
    docker buildx version; \
    docker compose version; \
    apt-get clean; \
    rm -rf /var/lib/apt/lists/*


# ============================================================
# Jenkins Plugins
#
# 插件在镜像构建阶段安装，避免每次启动重新下载。
# ============================================================
USER jenkins

RUN jenkins-plugin-cli --plugins \
    workflow-aggregator \
    git \
    credentials-binding \
    gitlab-plugin \
    pipeline-stage-view \
    junit


# ============================================================
# Runtime User
# ============================================================
USER jenkins