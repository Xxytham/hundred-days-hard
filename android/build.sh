#!/usr/bin/env bash
# Builds 100 Days Hard as a sideloadable APK from ../app.html.
#
# Usage: ./build.sh            version from android/VERSION, signed with the real key
#        TEST=1 ./build.sh     no real key needed; signs with a throwaway key (for testing only,
#                              Android will refuse to install it over the real app)
#
# The real signing key is never generated here. A new key would make every phone refuse the
# update without uninstalling first, which wipes the app's data.
set -euo pipefail
cd "$(dirname "$0")"
SDK=${ANDROID_SDK:-${ANDROID_HOME:-$HOME/android-sdk}}
BT=$SDK/build-tools/35.0.0
JAR=$SDK/platforms/android-35/android.jar
[ -x "$BT/aapt2" ] && [ -f "$JAR" ] || ANDROID_SDK=$SDK ./setup-sdk.sh

VC=$(sed -n 's/^code=//p' VERSION); VN=$(sed -n 's/^name=//p' VERSION)
KS=${KEYSTORE:-keystore/hundred-days-hard.jks}
PASS=${KEYSTORE_PASS:-hundreddays}
SUFFIX=""
if [ "${TEST:-0}" = "1" ]; then
  KS=build/test.jks; PASS=testtest; SUFFIX="-TEST"
elif [ ! -f "$KS" ]; then
  echo "Signing key not found at android/$KS." >&2
  echo "Real releases are built by GitHub Actions, which has the key. For a local test build run: TEST=1 ./build.sh" >&2
  exit 1
fi

OUT=build; rm -rf $OUT/gen $OUT/classes $OUT/dex $OUT/*.apk; mkdir -p $OUT/gen $OUT/classes $OUT/dex

# Page: wrap the artifact HTML in a document and swap Google Fonts for the bundled copies.
python3 - <<'PY'
import re
src = open('../app.html').read()
src = re.sub(r'<link rel="preconnect"[^>]*>\n', '', src)
src = re.sub(r'<link rel="stylesheet" href="https://fonts.googleapis.com[^"]*">', '<link rel="stylesheet" href="fonts.css">', src)
head = ('<!doctype html><html><head><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">'
        '<style>:root{color-scheme:light}body{margin:0;font:14px system-ui,sans-serif}img{max-width:100%}[hidden]{display:none!important}</style>'
        '</head><body>')
open('assets/index.html', 'w').write(head + src + '</body></html>')
PY

$BT/aapt2 compile --dir res -o $OUT/res.zip
$BT/aapt2 link -I $JAR --manifest AndroidManifest.xml -A assets --java $OUT/gen \
  --min-sdk-version 30 --target-sdk-version 35 --version-code "$VC" --version-name "$VN" \
  -o $OUT/unsigned.apk $OUT/res.zip
javac -nowarn --release 11 -classpath $JAR -d $OUT/classes $(find src $OUT/gen -name '*.java')
$BT/d8 --release --lib $JAR --min-api 30 --output $OUT/dex $(find $OUT/classes -name '*.class')
(cd $OUT/dex && zip -q ../unsigned.apk classes.dex)
$BT/zipalign -f -p 4 $OUT/unsigned.apk $OUT/aligned.apk

if [ "${TEST:-0}" = "1" ] && [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias app -keyalg RSA -keysize 2048 -validity 365 \
    -storepass $PASS -keypass $PASS -dname "CN=100 Days Hard test" >/dev/null 2>&1
fi
APK="100-days-hard-v$VN$SUFFIX.apk"
$BT/apksigner sign --ks "$KS" --ks-pass pass:$PASS --ks-key-alias app --out "$APK" $OUT/aligned.apk
$BT/apksigner verify "$APK"
echo "Built android/$APK (versionCode $VC)"
