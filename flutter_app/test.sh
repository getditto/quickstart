#! /usr/bin/env bash

set -euo pipefail

ROOT=$(git rev-parse --show-toplevel)
FLUTTER_APP=$ROOT/flutter_app

cd "$FLUTTER_APP"

export LIBDITTOFFI_PATH="$FLUTTER_APP/libdittoffi.so"
SDK_VERSION=$(cat pubspec.yaml | jq .dependencies.ditto_live)

curl "https://software.ditto.live/flutter/ditto/$SDK_VERSION/linux/x86_64/libdittoffi.so" \
  > "$LIBDITTOFFI_PATH"

flutter test
