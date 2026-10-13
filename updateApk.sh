#!/bin/bash
set -euo pipefail
# Download published CI artifacts; keep large deployment files out of Git.
cd "$(dirname "$0")"
python3 scripts/update_local_downloads.py "$@"
