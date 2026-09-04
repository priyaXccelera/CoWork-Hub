#!/usr/bin/env bash
set -e

SERVER_PORT="${SERVER_PORT:-26986}"

./gradlew bootJar -q

java -jar build/libs/*.jar --server.port="$SERVER_PORT"
