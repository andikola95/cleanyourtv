#!/usr/bin/env bash
# Re-enable everything examples/tcl-g10-4k-gb/debloat.txt disabled and give Home back to the stock
# Google TV launcher. Packages that shipped disabled (Prime Video, YouTube Music) stay disabled.
# Usage: TV_HOST=10.0.10.27 examples/tcl-g10-4k-gb/restore.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
export TV_HOST="${TV_HOST:-10.0.10.27}"
exec tools/restore.sh examples/tcl-g10-4k-gb/debloat.txt com.google.android.apps.tv.launcherx
