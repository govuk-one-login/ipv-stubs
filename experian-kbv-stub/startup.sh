#!/usr/bin/env bash
set -eu
docker build -t experian-kbv-stub .
docker run -p 8090:8090 experian-kbv-stub
