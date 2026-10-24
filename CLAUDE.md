# 100 Days Hard — notes for Claude

A gym tracker for a 100-day challenge, built for the owner and shared with friends. One HTML file (`app.html`) is the whole app. It ships three ways:

1. **Installed Android app (main one the owner uses).** `android/` wraps `app.html` in a WebView APK. Data lives on the phone. Samsung Health numbers arrive through Health Connect.
3. **iPhone home-screen web app** for friends, at https://xxytham.github.io/hundred-days-hard/ (GitHub Pages, built by `web/build.sh`). Data lives in that phone's browser storage. The friends' download page is `/get/`.
2. **Claude artifact (web).** https://claude.ai/artifact/XAhfUhWMsa4JCgrTkEJTEE, published from `app.html`. Data lives in the artifact's database under the owner's private `data/users/<id>/` path.

The owner is not a programmer and talks to you from a phone. Keep explanations plain, test before shipping, and say what you could not test.

## Ground rules

- **No personal data in code.** Friends install the same app, so defaults stay generic (no goals, generic tasks, start date = today). The owner's own goals and settings live in their saved data, not in `defaultGoals()`/`defaultSettings()`.
- **Never lose the owner's data.** Every change to the stored format needs a migration in `normalizeDay()` (or the matching loader), and old backups must still restore.
- **Never make a new signing key.** The APK is signed with the key in the `KEYSTORE_B64` Actions secret. A different key means the phone refuses the update unless the app is uninstalled, which wipes its data. `android/build.sh` refuses to sign without the real key; use `TEST=1` for local test builds.
- **Bump `android/VERSION` (`code` +1, `name`) for every APK release** and add a `## v<name>` section to `CHANGELOG.md`. Pushing a VERSION change to `main` makes GitHub Actions build and publish the signed APK as a Release.
- After changing `app.html`, also republish the artifact (see below) so both versions stay the same.

## File map

| Path | What it is |
|---|---|
| `app.html` | The app: CSS tokens at top, then markup, then one script. Written as an artifact page (no `<html>`/`<head>`; the artifact host and `build.sh` add them). |
| `android/build.sh` | Builds the APK with plain SDK tools (aapt2, javac, d8, apksigner). No Gradle. Downloads the SDK via `setup-sdk.sh` if missing (needs `dl.google.com` allowed). |
| `android/src/.../MainActivity.java` | WebView host, insets, back button, `AndroidBridge` (backup file, restore picker, Health Connect). |
| `android/src/.../HealthReader.java` | Reads Nutrition, Weight, Steps, Hydration and Sleep records from Health Connect (Android 14+ framework API, no Jetpack), each only if its permission was granted. Prefers Samsung Health's records over other apps' to avoid double counting. Sleep counts on the day it ends, minus awake stages. |
| `android/src/.../PrivacyActivity.java` | Rationale screen Health Connect requires. |
| `android/assets/fonts*` | Bundled Google Fonts so the APK works offline. `assets/index.html` is generated, not committed. |
| `tools/wrap_page.py` | Wraps `app.html` into a full document with bundled fonts. Used by both builds; `--pwa` adds the home-screen tags and service worker. |
| `web/` | iPhone web app pieces: `manifest.webmanifest`, `sw.js` (offline cache), `icons/`, `get/index.html` (download page for friends), `build.sh` (outputs `dist/web`). |
| `.github/workflows/build-apk.yml` | On a change to VERSION, the workflow or `web/`: `build` job signs the APK and makes a Release (assets `100-days-hard-v<name>.apk` and a fixed-name `100-days-hard.apk`, so `releases/latest/download/100-days-hard.apk` is always newest), and the `web` job deploys `dist/web` to GitHub Pages. |

## How the app works

Tabs: **Today**, **Goals**, **Progress**. Settings opens from the day-counter plate in the top corner.

- **First-time setup:** shown while `settings.setupDone` is false (fresh installs only). Steps: welcome or restore → day → calorie/protein/unit → daily tasks (`TASK_PRESETS` plus custom) → Samsung Health (APK only) → done. Anyone with saved data from before v0.4 is marked `setupDone` on load.
- **Streak:** `streakInfo()` counts consecutive days with every daily task done. Today counts once it's complete; until then the streak runs through yesterday. It's shown on Today with the last 7 days, and the Progress tab uses the same number.
- **Backup dot:** next to the date. `backupState()` is green if `settings.lastExport` (last copy sent off the phone) is within 7 days, yellow within 14, and red otherwise or if there's never been one. It's always green in the Claude artifact once synced. Tapping it opens Settings.
- **Updates:** the APK checks `api.github.com/repos/Xxytham/hundred-days-hard/releases/latest` on launch and shows an "Update" banner if the tag is newer than `AndroidBridge.appVersion()`. The web app updates itself (network-first page in `sw.js`, with the cache name bumped by version).
- **Sharing:** Settings has "Share with friends": the link `https://xxytham.github.io/hundred-days-hard/get/`, a QR code (qrcode-generator from cdnjs, loaded on demand), plus share/copy buttons. Releases and Pages only work for friends while the repo is **public**.

- **Challenge length:** `settings.totalDays` (default 100) drives the plate, the grid, "Day N of X" and the streak range. When the last day is done (or passed), Today shows a finish card with round stats. "Start another round" appends `{startDate, totalDays}` to `settings.rounds` and makes today day 1. History is never deleted.
- **Tasks that tick themselves:** `METRICS` covers `water`, `steps` and `sleep` (task ids). Names come from `settings.waterTarget`/`stepsTarget`/`sleepTarget`. Synced values live in `day.health` and tick the task when they reach the target (never unticking). People who set up before v0.5 get a one-time "Add them" card (`settings.autoOffered`).
- **Tasks count from `addedOn`:** a task with `addedOn` only counts for days on or after it (`tasksFor(date)`), so adding a task never breaks past perfect days or a streak.
- **Trends:** Progress has one 14-day bar chart (`TRENDS`: calories, sleep, steps, water) with chips, shown only for measures that have data.
- **Apple Health:** not possible from the iPhone web app. It would need a native iOS app and an Apple developer account.
- **Day counter:** `settings.startDate` is day 1 of the current round.
- **Food:** one daily total for calories and protein (the owner logs food in Samsung Health). In the APK these fill in from Health Connect on open/resume. A value the owner types sets `kcalSrc: "manual"`, and synced data never overwrites it.
- **Daily tasks:** checklist from `settings.habits`. `protein` auto-checks when the protein goal is met, and `workout` checks when a workout is saved.
- **Workouts (Push / Pull / Legs):** starting a type copies the exercises, order, weights, reps and sets from the most recent *saved* session of that type. −/+ steps weight by 5 lb (2.5 kg). Exercises with "cable" in the name get an lb/kg toggle that converts the value. A workout autosaves as a draft (`saved: false`) until "Save workout".
- **Strength (Progress tab):** per-exercise "stock ticker". Change is measured on estimated 1RM, `w × (1 + reps/30)` (Epley), so 45×12 → 50×7 reads as a small drop. Bodyweight exercises (weight 0) compare reps.
- **Goals:** ladders the owner wrote. Steps are stored easiest → hardest and shown hardest at the top. "Next up" is the first step not yet done.
- **Backup:** APK writes `Download/100 Days Hard/100-days-hard-backup.json` about 1.5 s after any change (skipped while the app has no data, so a fresh install can't overwrite a good backup). "Send a copy" opens the share sheet. Restore merges by `updatedAt` per document, newest wins. The web version saves to the account automatically and can save/restore the same file.

### Data shape (localStorage key `hundred-days-hard.v1`, and one db doc per key)

```
settings: { startDate, totalDays, calorieGoal, proteinGoal, waterTarget, stepsTarget, sleepTarget, unit: "lb"|"kg",
            habits: [{id, name, addedOn?}], setupDone, autoOffered?, finishDismissed?, rounds?: [{startDate, totalDays}], lastExport?, updatedAt }
goals:    { updatedAt, list: [{ id, name, focus, steps: [{ id, text, done, doneDay }] }] }
days["YYYY-MM-DD"]: {
  kcal, protein, kcalSrc: null|"manual"|"health", weight, habits: {habitId: bool},
  health: null | { steps, waterL, sleepMin },
  workout: null | { type: "push"|"pull"|"legs"|"other", saved, savedAt,
                    exercises: [{ id, name, weight, reps, sets, unit }] },
  updatedAt
}
```
Backup file: `{ app: "100-days-hard", format: 1, exportedAt, settings, goals, days }`.

## Seeing the owner's progress

- **Web version:** `ArtifactData` `list` on collection `data/users/me` for the artifact URL above. Documents are `settings`, `goals` and one per date.
- **APK:** ask the owner to send a backup (Settings → "Send a copy to Drive or email") and attach the JSON to the chat.

Treat that data as the owner's private health info. Read it to help them, and never paste it into the repo or the published page.

## Shipping a change

1. Edit `app.html` (and `android/` if native code changes).
2. Regenerate test pages with `python3 tools/wrap_page.py android/assets/index.html` and `web/build.sh` (serve `dist/web` with `python3 -m http.server`). Test in Chromium with Playwright at 412×915 (the owner has a Samsung S25 Ultra). To exercise APK-only paths, inject a fake `window.AndroidBridge` with an init script (see the methods in `MainActivity.Bridge`). Check for page errors, and take screenshots for anything visual.
3. Optionally `TEST=1 android/build.sh` to check it compiles. Don't hand that APK to the owner; it won't install over theirs.
4. Bump `android/VERSION`, add the changelog section, commit, push to `main`. That triggers the "Build APK" workflow, which publishes the signed APK to the repo's Releases page. The owner installs it over the old one.
   - Claude sessions can push `main`, but can't push or delete tags, view Actions runs, create releases, or set secrets (the session's GitHub proxy blocks those). Reading releases does work, so check that a build landed with `gh api repos/Xxytham/hundred-days-hard/releases`. Download the asset with `gh api -H "Accept: application/octet-stream" repos/Xxytham/hundred-days-hard/releases/assets/<id>`, and confirm its signer SHA-256 is `9851d428…fb26b` with `apksigner verify --print-certs`. If a build is missing, ask the owner to check the **Actions** tab, or the **Releases** page at https://github.com/Xxytham/hundred-days-hard/releases, and to use **Run workflow** on "Build APK" if a build needs re-running.
5. Republish the web version: `Artifact` read of the URL first, then publish `app.html` with `url` set to it. Keep capabilities `{db: {}, user: {}, downloads: true}`.

## Design

Bumper-plate palette (red 25 kg, blue 20 kg, yellow 15 kg, green 10 kg) over cool steel neutrals, with light and dark themes as CSS tokens on `:root`. Saira Condensed for display, Figtree for body. The owner likes the look, so keep new UI in the same system.
