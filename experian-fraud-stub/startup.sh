#!/usr/bin/env bash
set -eu
docker build -t experian-fraud-stub \
--build-arg CI1=$CI1 \
--build-arg CI2=$CI2 \
--build-arg CI3=$CI3 \
--build-arg CI4=$CI4 \
--build-arg CI5=$CI5 .
docker run -p 8080:8080 experian-fraud-stub
