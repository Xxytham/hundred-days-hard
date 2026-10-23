# 100 Days Hard

My gym tracker for a 100-day challenge: daily calories and protein from Samsung Health, daily tasks, Push / Pull / Legs workouts, strength trends, body weight, and goal ladders.

## Install or update on my phone
1. On the phone, open this repo's **Releases** page and download the newest `100-days-hard-v….apk`.
2. Tap it and choose **Install** (or **Update**). My data stays, because every release is signed with the same key.

## Keeping my data safe
- The app copies everything to **Download/100 Days Hard/100-days-hard-backup.json** after every change. That file survives uninstalling the app.
- About once a week: **tap the day counter → Send a copy to Drive or email → Google Drive.**
- New phone or reinstall: install the app, tap **Restore from backup**, and pick the newest file.

## Keeping the app safe
- **The code** lives in this private repo.
- **The signing key** (`hundred-days-hard.jks`) is stored as a GitHub secret, so GitHub can build updates. Keep your own copy in Google Drive too. Without it, future updates would need an uninstall, and uninstalling wipes the app's data. (Restoring from your backup file gets the data back.)

## Changing the app with Claude
Start a Claude Code session on the web with this repository selected and describe what you want. Claude reads `CLAUDE.md` first, which explains how everything works, so it doesn't need to remember earlier conversations. When Claude ships a change, a new release shows up here a few minutes later.

To have Claude look at your progress, either open the web version (link in `CLAUDE.md`) or send Claude a backup file.
