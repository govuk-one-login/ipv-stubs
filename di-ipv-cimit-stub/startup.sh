#!/usr/bin/env bash
set -eu

echo "Building di-ipv-cimit-stub"
./gradlew clean build

echo "Starting di-ipv-cimit-stub"
./gradlew run