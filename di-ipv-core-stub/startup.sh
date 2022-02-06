#!/usr/bin/env bash
set -eu

CONFIG_DIR="../../di-ipv-config/stubs/di-ipv-core-stub"

clean_up () {
    rm -r config
}
trap clean_up EXIT

if [ ! -d "$CONFIG_DIR" ]; then
  { echo "🛑 config dir '$CONFIG_DIR' does not exist in expected location"; exit 1; }
fi

cp -r $CONFIG_DIR config

docker build -t ipv-core-stub .
docker run -p 8085:8085 --env-file ${CONFIG_DIR}/.env ipv-core-stub
