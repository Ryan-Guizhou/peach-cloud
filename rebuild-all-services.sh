#!/bin/bash

# Script to rebuild all Peach Cloud services

set -e  # Exit on any error

SERVICES=("peach-gateway" "peach-auth" "peach-monitor" "peach-fileservice" "peach-message" "peach-setting")

echo "Services to rebuild: ${SERVICES[@]}"

for service in "${SERVICES[@]}"; do
    echo
    echo "========================================"
    echo "Processing service: $service"
    echo "========================================"

    echo "Stopping $service service..."
    docker-compose stop "$service" || echo "Warning: $service service was not running"

    echo "Removing $service image..."
    docker rmi -f "peach-cloud/$service:latest" || echo "Warning: $service image did not exist"

    # Determine service directory from service name
    case $service in
        "peach-gateway")
            SERVICEDIR="peach-gateway/peach-gateway-launch"
            ;;
        "peach-auth")
            SERVICEDIR="peach-auth/peach-auth-launch"
            ;;
        "peach-monitor")
            SERVICEDIR="peach-monitor/peach-monitor-launch"
            ;;
        "peach-fileservice")
            SERVICEDIR="peach-fileservice/peach-fileservice-launch"
            ;;
        "peach-message")
            SERVICEDIR="peach-message/peach-message-launch"
            ;;
        "peach-setting")
            SERVICEDIR="peach-setting/peach-setting-launch"
            ;;
    esac

    echo "Building $service image..."
    cd "$SERVICEDIR"

    if [ -f "Dockerfile" ]; then
        if ! docker build -f Dockerfile -t "peach-cloud/$service:latest" .; then
            echo "Error: Failed to build $service image!"
            cd ../..
            exit 1
        fi
    else
        echo "Error: Dockerfile not found in $service/$service-launch"
        cd ../..
        exit 1
    fi

    cd ../..
    echo "Starting $service service..."
    docker-compose up -d "$service"

    echo "Service $service has been rebuilt and restarted."
done

echo
echo "========================================"
echo "All services have been rebuilt and restarted successfully!"
echo "========================================"