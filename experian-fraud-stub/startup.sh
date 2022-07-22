#!/usr/bin/env bash
set -eu
docker build -t experian-fraud-stub .
docker run -p 8080:8090 experian-fraud-stub
