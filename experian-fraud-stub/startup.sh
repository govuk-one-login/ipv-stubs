#!/usr/bin/env bash
set -eu
docker build -t experian-fraud-stub \
--build-arg CONTIND=$CONTIND \
--build-arg PEPS=$PEPS.
docker run -p 8080:8080 experian-fraud-stub
