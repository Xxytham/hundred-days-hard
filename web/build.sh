#!/usr/bin/env bash
# Builds the home-screen web app (used on iPhone) and the friends' download page into dist/web.
set -euo pipefail
cd "$(dirname "$0")/.."
VN=$(sed -n 's/^name=//p' android/VERSION)
OUT=dist/web
rm -rf "$OUT"; mkdir -p "$OUT/icons" "$OUT/fonts" "$OUT/get"
python3 tools/wrap_page.py "$OUT/index.html" --pwa "$VN"
cp android/assets/fonts.css "$OUT/fonts.css"
cp android/assets/fonts/* "$OUT/fonts/"
cp web/icons/* "$OUT/icons/"
cp web/manifest.webmanifest "$OUT/"
cp web/get/index.html "$OUT/get/index.html"
FONTS=$(cd "$OUT" && ls fonts | sed 's#^#"fonts/#; s#$#"#' | paste -sd, -)
sed "s#__VERSION__#$VN#; s#__FONTS__#$FONTS#" web/sw.js > "$OUT/sw.js"
touch "$OUT/.nojekyll"
echo "Built $OUT (v$VN)"
