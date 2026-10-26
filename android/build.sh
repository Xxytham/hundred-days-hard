#!/usr/bin/env bash
# Builds 100 Days Hard as a sideloadable APK from ../app.html, with Gradle (see build.gradle).
#
# Usage: ./build.sh            version from android/VERSION, signed with the real key
#        TEST=1 ./build.sh     no real key needed; signs with a throwaway debug key (for testing only,
#                              Android will refuse to install it over the real app)
#
# The real signing key is never generated here. A new key would make every phone refuse the
# update without uninstalling first, which wipes its data.
set -euo pipefail
cd "$(dirname "$0")"
SDK=${ANDROID_SDK:-${ANDROID_HOME:-$HOME/android-sdk}}
[ -x "$SDK/build-tools/35.0.0/aapt2" ] && [ -f "$SDK/platforms/android-35/android.jar" ] || ANDROID_SDK=$SDK ./setup-sdk.sh
export ANDROID_HOME="$SDK"

VN=$(sed -n 's/^name=//p' VERSION)
KS=${KEYSTORE:-keystore/hundred-days-hard.jks}
ARGS=()
SUFFIX=""
if [ "${TEST:-0}" = "1" ]; then
  ARGS+=(-PtestSign); SUFFIX="-TEST"
elif [ ! -f "$KS" ]; then
  echo "Signing key not found at android/$KS." >&2
  echo "Real releases are built by GitHub Actions, which has the key. For a local test build run: TEST=1 ./build.sh" >&2
  exit 1
fi

# Page: wrap the artifact HTML in a document and swap Google Fonts for the bundled copies.
python3 ../tools/wrap_page.py assets/index.html

./gradlew --no-daemon -q assembleRelease "${ARGS[@]}"
APK="100-days-hard-v$VN$SUFFIX.apk"
cp build/outputs/apk/release/app*-release.apk "$APK" 2>/dev/null || cp build/outputs/apk/release/*-release.apk "$APK"
"$SDK/build-tools/35.0.0/apksigner" verify "$APK"
echo "Built android/$APK"
