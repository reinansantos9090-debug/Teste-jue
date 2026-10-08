#!/bin/sh
set -eu

GRADLE_VERSION="9.3.1"
ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-$GRADLE_VERSION-bin"
DIST_DIR="$CACHE_DIR/gradle-$GRADLE_VERSION"
ZIP_FILE="$CACHE_DIR/gradle-$GRADLE_VERSION-bin.zip"
URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

mkdir -p "$CACHE_DIR"
if [ ! -x "$DIST_DIR/bin/gradle" ]; then
  tmp="$ZIP_FILE.tmp"
  echo "Downloading Gradle $GRADLE_VERSION..."
  curl -L --fail --retry 3 -o "$tmp" "$URL"
  mv "$tmp" "$ZIP_FILE"
  rm -rf "$DIST_DIR"
  unzip -q "$ZIP_FILE" -d "$CACHE_DIR"
fi

exec "$DIST_DIR/bin/gradle" "$@"
