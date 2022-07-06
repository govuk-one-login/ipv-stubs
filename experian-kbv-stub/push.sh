#!/usr/bin/env bash
set -eu
./gradlew
cf push -f ./dev-manifest.yml
