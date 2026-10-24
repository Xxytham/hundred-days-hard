# 100 Days Hard — notes for Claude

A personal gym tracker for a 100-day challenge. One HTML file (`app.html`) is the whole app. It ships two ways:

1. **Installed Android app (main one the owner uses).** `android/` wraps `app.html` in a WebView APK. Data lives on the phone. Samsung Health numbers arrive through Health Connect.
2. **Claude artifact (web).** https://claude.ai/artifact/XAhfUhWMsa4JCgrTkEJTEE, published from `app.html`. Data lives in the artifact's database under the owner's private `data/users/<id>/` path.

The owner is not a programmer and talks to you from a phone. Keep explanations plain, test before shipping, and say what you could not test.

## Ground rules

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
| `android/src/.../HealthReader.java` | Reads Nutrition and Weight records from Health Connect (Android 14+ framework API, no Jetpack). |
| `android/src/.../PrivacyActivity.java` | Rationale screen Health Connect requires. |
| `android/assets/fonts*` | Bundled Google Fonts so the APK works offline. `assets/index.html` is generated, not committed. |
| `.github/workflows/build-apk.yml` | Signed release build on VERSION change. |

## How the app works

Tabs: **Today**, **Goals**, **Progress**. Settings opens from the day-counter plate in the top corner.

- **Day counter:** `settings.startDate` is day 1 (2026-09-17, so 2026-10-23 was day 37).
- **Food:** one daily total for calories and protein (the owner logs food in Samsung Health). In the APK these fill in from Health Connect on open/resume. A value the owner types sets `kcalSrc: "manual"`, and synced data never overwrites it.
- **Daily tasks:** checklist from `settings.habits`. `protein` auto-checks when the protein goal is met, and `workout` checks when a workout is saved.
- **Workouts (Push / Pull / Legs):** starting a type copies the exercises, order, weights, reps and sets from the most recent *saved* session of that type. −/+ steps weight by 5 lb (2.5 kg). Exercises with "cable" in the name get an lb/kg toggle that converts the value. A workout autosaves as a draft (`saved: false`) until "Save workout".
- **Strength (Progress tab):** per-exercise "stock ticker". Change is measured on estimated 1RM, `w × (1 + reps/30)` (Epley), so 45×12 → 50×7 reads as a small drop. Bodyweight exercises (weight 0) compare reps.
- **Goals:** ladders the owner wrote. Steps are stored easiest → hardest and shown hardest at the top. "Next up" is the first step not yet done.
- **Backup:** APK writes `Download/100 Days Hard/100-days-hard-backup.json` about 1.5 s after any change (skipped while the app has no data, so a fresh install can't overwrite a good backup). "Send a copy" opens the share sheet. Restore merges by `updatedAt` per document, newest wins. The web version saves to the account automatically and can save/restore the same file.

### Data shape (localStorage key `hundred-days-hard.v1`, and one db doc per key)

```
settings: { startDate, calorieGoal, proteinGoal, unit: "lb"|"kg", habits: [{id, name}], lastExport?, updatedAt }
goals:    { updatedAt, list: [{ id, name, focus, steps: [{ id, text, done, doneDay }] }] }
days["YYYY-MM-DD"]: {
  kcal, protein, kcalSrc: null|"manual"|"health", weight, habits: {habitId: bool},
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
2. Test in Chromium with Playwright at 412×915 (the owner has a Samsung S25 Ultra). To exercise APK-only paths, inject a fake `window.AndroidBridge` with an init script (see the methods in `MainActivity.Bridge`). Check for page errors, and take screenshots for anything visual.
3. Optionally `TEST=1 android/build.sh` to check it compiles. Don't hand that APK to the owner; it won't install over theirs.
4. Bump `android/VERSION`, add the changelog section, commit, push to `main`. That triggers the "Build APK" workflow, which publishes the signed APK to the repo's Releases page. The owner installs it over the old one.
   - Claude sessions can push, but can't view Actions runs, create releases, or set secrets (the session's GitHub proxy blocks those). Ask the owner to check the **Actions** tab, or the **Releases** page at https://github.com/Xxytham/hundred-days-hard/releases, and to use **Run workflow** on "Build APK" if a build needs re-running.
5. Republish the web version: `Artifact` read of the URL first, then publish `app.html` with `url` set to it. Keep capabilities `{db: {}, user: {}, downloads: true}`.

## Design

Bumper-plate palette (red 25 kg, blue 20 kg, yellow 15 kg, green 10 kg) over cool steel neutrals, with light and dark themes as CSS tokens on `:root`. Saira Condensed for display, Figtree for body. The owner likes the look, so keep new UI in the same system.
