#!/usr/bin/env bash
set -eu

echo "Building di-ipv-orchestrator-stub"
./gradlew clean build

echo "Starting di-ipv-orchestrator-stub"
./gradlew run