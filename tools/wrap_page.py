#!/usr/bin/env python3
"""Turns app.html (written as a Claude artifact page) into a standalone document.

Usage: wrap_page.py <out.html> [--pwa <version>]
Swaps Google Fonts for the bundled copies (fonts.css next to the output).
With --pwa it adds the home-screen app tags and registers the offline service worker.
"""
import re
import sys

out = sys.argv[1]
pwa = "--pwa" in sys.argv
version = sys.argv[sys.argv.index("--pwa") + 1] if pwa else ""
root = __file__.rsplit("/tools/", 1)[0]
src = open(root + "/app.html").read()
src = re.sub(r'<link rel="preconnect"[^>]*>\n', "", src)
src = re.sub(r'<link rel="stylesheet" href="https://fonts.googleapis.com[^"]*">', '<link rel="stylesheet" href="fonts.css">', src)
head = ('<!doctype html><html lang="en"><head><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">')
if pwa:
    head += ('<link rel="manifest" href="manifest.webmanifest">'
             '<link rel="apple-touch-icon" href="icons/apple-touch-icon.png">'
             '<link rel="icon" href="icons/icon-192.png">'
             '<meta name="apple-mobile-web-app-capable" content="yes">'
             '<meta name="mobile-web-app-capable" content="yes">'
             '<meta name="apple-mobile-web-app-status-bar-style" content="default">'
             '<meta name="apple-mobile-web-app-title" content="100 Days Hard">')
head += ('<style>:root{color-scheme:light;padding-top:env(safe-area-inset-top,0px);padding-bottom:env(safe-area-inset-bottom,0px)}'
         'body{margin:0;font:14px system-ui,sans-serif}img{max-width:100%}[hidden]{display:none!important}</style>'
         '</head><body>')
tail = ""
if pwa:
    tail = ('<script>if("serviceWorker" in navigator){addEventListener("load",function(){'
            'navigator.serviceWorker.register("sw.js?v=' + version + '").catch(function(){});});}</script>')
open(out, "w").write(head + src + tail + "</body></html>")
