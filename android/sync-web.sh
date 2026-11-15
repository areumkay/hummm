#!/bin/sh
# Copies ../hummm.html into the app as assets/index.html, wrapped in a full
# HTML document. Google Fonts links are dropped since the app has no internet
# access; the page falls back to the phone's system fonts.
# Run this after editing hummm.html, then rebuild.
set -e
here="$(cd "$(dirname "$0")" && pwd)"
src="$here/../hummm.html"
out="$here/app/src/main/assets/index.html"
mkdir -p "$(dirname "$out")"
{
  printf '<!doctype html>\n<html lang="en">\n<head>\n<meta charset="utf-8">\n'
  printf '<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">\n'
  printf '<style>:root{padding-top:env(safe-area-inset-top,0px);padding-bottom:env(safe-area-inset-bottom,0px)}body{margin:0}[hidden]{display:none!important}</style>\n'
  printf '</head>\n<body>\n'
  grep -v 'fonts.googleapis.com' "$src"
  printf '\n</body>\n</html>\n'
} > "$out"
echo "Wrote $out"
