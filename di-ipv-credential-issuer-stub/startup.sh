#!/usr/bin/env bash
set -eu

echo "Building di-ipv-credential-issuer-stub"
./gradlew clean build

echo "Starting di-ipv-credential-issuer-stub"
./gradlew run