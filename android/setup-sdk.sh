#!/usr/bin/env bash
# Downloads the two Android SDK pieces build.sh needs (no Android Studio, no Gradle).
# Usage: ./setup-sdk.sh            installs into $ANDROID_SDK (default ~/android-sdk)
# Needs network access to dl.google.com.
set -euo pipefail
SDK=${ANDROID_SDK:-$HOME/android-sdk}
mkdir -p "$SDK/build-tools" "$SDK/platforms"
tmp=$(mktemp -d)
if [ ! -x "$SDK/build-tools/35.0.0/aapt2" ]; then
  curl -sSfL https://dl.google.com/android/repository/build-tools_r35_linux.zip -o "$tmp/bt.zip"
  unzip -q "$tmp/bt.zip" -d "$tmp/bt"
  mv "$tmp/bt/android-15" "$SDK/build-tools/35.0.0"
fi
if [ ! -f "$SDK/platforms/android-35/android.jar" ]; then
  curl -sSfL https://dl.google.com/android/repository/platform-35_r02.zip -o "$tmp/pl.zip"
  unzip -q "$tmp/pl.zip" -d "$tmp/pl"
  mv "$tmp/pl/android-35" "$SDK/platforms/android-35"
fi
rm -rf "$tmp"
echo "Android SDK ready in $SDK"
