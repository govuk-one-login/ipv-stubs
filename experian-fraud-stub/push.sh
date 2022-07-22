#!/usr/bin/env bash
set -eu
./gradlew
cf push
