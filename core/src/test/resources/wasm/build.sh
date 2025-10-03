#!/bin/bash
set -euo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

name=$1

# make the root library available in a subfolder
rm -f ${SCRIPT_DIR}/tinygo4j/*.mod
rm -f ${SCRIPT_DIR}/tinygo4j/*.go

cp ${SCRIPT_DIR}/../../../../../*.mod ./tinygo4j
cp ${SCRIPT_DIR}/../../../../../*.go ./tinygo4j

docker run --rm \
    -v ${SCRIPT_DIR}/${name}:/src \
    -v ${SCRIPT_DIR}/tinygo4j:/tinygo4j \
    -e GO111MODULE=on \
    -w /src tinygo/tinygo bash \
    -c "tinygo build --no-debug -target=wasip1 -o /tmp/tmp.wasm . && cat /tmp/tmp.wasm" > \
    ${SCRIPT_DIR}/compiled/${name}-wasi.wasm

# docker run --rm \
#     -v ${SCRIPT_DIR}/${name}:/src \
#     -v ${SCRIPT_DIR}/tinygo4j:/tinygo4j \
#     -e GO111MODULE=on \
#     -w /src tinygo/tinygo bash \
#     -c "tinygo build --no-debug -target=wasm-unknown -o /tmp/tmp.wasm . && cat /tmp/tmp.wasm" > \
#     ${SCRIPT_DIR}/compiled/${name}-wasm-unknown.wasm
