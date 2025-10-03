#!/bin/bash
set -euo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

find ${SCRIPT_DIR} -type d -print0 | xargs -0 -I{} sh -c 'cd "{}" && go fmt'
