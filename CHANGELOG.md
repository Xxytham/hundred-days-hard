# Changelog

## v0.22.1 · Red 45s on the loader
- The coach's loading barbell now has three red 45 lb plates a side. They slide smoothly onto the bar one at a time, then slide back off, so it looks like a real bar instead of a toy.

## v0.22 · Meet your coach
- **A hello first:** the first time you use the coach, it introduces itself in its own words, a little different for everyone. It tells you it runs on your phone's own processor, so your numbers never leave the phone, and asks if you're ready. Tap **I'm ready** and it goes straight into its first look at your data, starting with something natural like "Well then, let's have a look at your charts."
- **New loading animation:** while the coach writes, a small barbell gets loaded plate by plate in the app's plate colors, with a line about what it's reading ("Checking calories and protein", "Looking at your lifts"). It replaces the pulsing bars.
- **Smoother text:** the note now inks in smoothly, character by character, instead of jumping in chunks.
- **No em dashes:** the coach never uses them.
- Also includes everything from v0.21 (plateau spotting, changes that only count from today, trend-colored body stats). That release didn't get published because of a GitHub server error, so it arrives with this one.

## v0.21 · A coach that spots plateaus, and changes that only count from today
- **Plateaus:** if your weight has barely moved for about three weeks while your calories were on target, the coach says so and explains why it might be. Maybe eaten and burned are nearly the same, or your resting burn has dropped as you got lighter. It then suggests a fix, like lowering the calorie goal by about 150 or adding about 2,000 steps a day. When your weight is trending the right way, that's the first thing it celebrates.
- **Changes count from today:** changing a target (calories, protein, steps, sleep, workouts a day) or a task now applies from today on. Past days keep the targets and tasks they had, so a streak or a perfect day can't change after the fact. Before this, raising your step goal could untick past days. Also, a task you delete still counts on the days it existed, and a task you add in Settings counts from today, not from day 1.
- **Stats colors:** the Body charts are colored by the overall trend, so one odd reading can't flip them. A flat trend is neutral. Resting burn (BMR) going down is green while you're losing fat, and going up is green when you're building muscle.

## v0.20 · Easier workout logging and a friendlier coach
**Workouts**
- No more Log set button. Tap the exercise you're doing and it starts at set 1, with the numbers from last time. Change whatever was different.
- Sets start at 1 and you add them as you go. **+ Set** and **Done** are at the bottom, by your thumb. Each new set starts from what you did on that set last time, moved by any change you made, so going 5 lb heavier carries on.
- **Done** folds the exercise up, so you can pick the next one.
- The open exercise has a clear outline and space around it, so it's easy to see where it starts and ends.
- Weight and reps each get a full-width row, so three digits (or 102.5) always fit. Tapping a box clears it so you just type the new number. Leave it empty and the old one comes back.
- **Remove** deletes a set. Removing the only set means you're not doing that exercise today, with Undo.
- Saving changes to a workout you already saved no longer asks again about keeping changes for next time.

**Coach**
- It knows how close you are. Every goal comes with how far off you are, and anything within a few percent counts as on target. 219 g against a 220 g protein goal is a win, not a miss.
- It has values: lead with what's going well, close counts, progress over perfection, one small next step. It also gets to choose what's most worth saying, so its notes should feel more like a coach in your corner.
- New look: a softly glowing card with a small avatar, and the numbers in bold.
- The note appears word by word as it's written, so the "Writing…" placeholder only shows for a moment. The model also loads ahead of time when the app opens.
- Fixed: the minimized coach could stretch the page wider than the screen.

## v0.19.2 · Each exercise in one place
- On the Progress tab, each exercise now shows under one tab only, by its muscle group. Chest, triceps and shoulders go under Push; back, biceps, forearms and abs under Pull; legs under Legs. Push-ups stay under Push even when you do them on a pull day, and it says "also on Pull" under the name.
- In a workout, an exercise from another day's group gets that group's heading. For example, push-ups on a pull day show under Chest instead of Back.
- Adding an exercise puts it where you've had it before, or where its name says it goes, whichever "+ Add" you tapped, and tells you where it went.
- For brand-new exercises the coach picks from the day's own groups first, and from the other groups only if the exercise clearly belongs there. It never makes up new groups.
- Exercises that had landed in Chest or Back only because nothing else fit were moved once to where their names say.

## v0.19.1 · Workout calories counted more honestly
- Watches tend to overestimate workout calories, especially lifting and "Other workout", where your heart rate stays up while you rest between sets. Calories burned now counts workout calories 20% lower for cardio and 35% lower for lifting. Your resting burn and everyday walking aren't changed.

## v0.19 · Whole-day calories and a cheer for every goal
- Calories burned on the Stats tab is now your whole day: resting burn, plus workouts, plus walking outside them. Samsung Health only shares workout calories with other apps, so before this it showed only your workouts, and eaten − burned looked like a big surplus.
- Hitting your protein, calorie, steps or sleep goal now gets a congratulations. A notification comes when the app is closed, and a message when you're in it. Each goal is celebrated once a day.
- Goal notifications are on by default now (Settings → Notifications & widget turns them off). Android asks once to allow them.

## v0.18.3 · Progress sorted by muscle group
- The Strength list on the Progress tab is now split into the same groups as your workouts (Chest, Triceps, Shoulders, Back, Biceps, Forearms, Abs). Each heading shows how that group moved since last time. Moving an exercise to another group in a workout, or the coach sorting it, moves it here too.

## v0.18.2 · Workouts start from your last numbers
- Every planned exercise starts at the weight and reps from the last time you did it, also when you log an earlier day. Before, logging an earlier day showed everything as new at 0 lb × 10.
- Going heavier: when your top set was the same weight and reps two times in a row, the third time suggests 5 lb more (one more rep for bodyweight moves).
- Removed Swap, and the list of exercise names that drew over the screen while typing.

## v0.18.1 · Coach sorts new exercises
- The first time you add an exercise you've never done, the coach checks which group it belongs in and moves it there if needed, for example a Zottman curl into Forearms. A message says what it did, with Undo. It only does this once per exercise, never moves something you've moved yourself, and doesn't run if the coach is off.

## v0.18 · A smarter coach you can tuck away
- The coach now reads your daily tasks and goals, as well as your food, sleep, steps, training and weight. It still runs only on your phone.
- It works out more before it writes: this week against last week, which task you miss most, how far over or under you are on missed days, new bests, lifts that have stalled, how fast your weight is moving, and how long since you moved up a goal.
- Its notes are a bit longer and more specific: what's going best, one pattern, and one thing to do that names your task, goal or exercise.
- New ⋯ menu on the coach: write a new note, minimize it to one line, or turn it off. Settings → Coach has the same On / Minimized / Off switch.

## v0.17 · Workouts follow your plan
- Starting a workout no longer looks already filled in. Your usual exercises and sets show in grey as the plan, and you tap **Log set** as you do each one. Change the weight or reps first if they went differently.
- Push and Pull days are split into groups: Chest, Triceps, Shoulders / Back, Biceps, Forearms, plus Abs on every day. The app sorts your exercises by name; drag one into another group in "Reorder or remove".
- Only the exercise you're on is open. The rest are one line each, and the next one opens when you finish an exercise.
- When you matched or beat your reps at the same weight two times running, it suggests going 5 lb heavier. It's only filled in if you tap it.
- Swap an exercise for today (machine taken), add one to any group, or skip one.
- Finish saves only the sets you logged. If you added or skipped exercises, it asks whether to keep the changes for next time.

## v0.16.5 · Calories and protein to go
- The line above Food is back: how many calories and grams of protein you still need today. It no longer mentions daily tasks.

## v0.16.4 · No summary line
- Removed the line of text above Food on Today. The bars and tasks turning green already show where you are.

## v0.16.3 · Shorter summary line
- The line under the coach no longer says how many daily tasks are done (the Daily tasks card shows that). It just says what's left for calories and protein.

## v0.16.2 · Cleaner streak
- Removed the small line of text under the streak counter ("Best: …").

## v0.16.1 · Icon tilted the other way
- The plate on the app icon now leans clockwise.

## v0.16 · Widget, background sync and goal alerts
- **Home-screen widget:** today's challenge day, your streak, calories and protein bars against your goals, and your next goal step. Touch and hold your home screen, tap Widgets, then 100 Days Hard. Tap it to open the app.
- **Background sync:** about once an hour, the app reads today's Samsung Health numbers while it's closed, so the widget stays current. Allow "background updates" when Samsung Health asks.
- **Goal alerts:** turn them on in Settings → Notifications & widget. You get a notification when Samsung Health shows you've hit your protein, calories, steps or sleep goal while the app is closed. Once per goal per day, and never while you're in the app.

## v0.15 · New icon, AI coach and one-tap updates
- New app icon: a red bumper plate with grip handles, a steel center and "100", tilted like it's leaning on a rack. It also comes in a single-color version for Android's themed icons, and the app opens on it.
- **Coach** under your streak on Today: a short note on what's going well and one thing to focus on, written on your phone by Gemini Nano from numbers the app works out (your last 7 days, strength, weight, resting heart rate, goals). Nothing leaves your phone. Tap Refresh for a new one.
- **Updates happen inside the app.** Tap Update, watch it download in the banner, then confirm on Android's "Update this app?" prompt. The first time, Android asks you to allow updates from this app. No more browser, repeated downloads or digging through notifications. (This applies from the next update on.)
- Stats charts and Samsung Health data now run from day 1 to today, not just the last 14 to 30 days. Allow "older history" when the app asks so it can read back to day 1.

## v0.14 · Stats in sections, more from your watch
- Stats has its own row of tabs: **Strength · Body · Heart · Sleep · Energy**. A section only appears once it has data. Each one is a grid of tiles; tap a tile for its chart, and tap again to close it.
- **Heart:** resting heart rate, worked out from your watch's heart rate while you sleep (Samsung Health doesn't share its own), blood oxygen, VO2 max and HRV when available.
- **Sleep:** total sleep against your goal, plus deep, REM and light sleep.
- **Energy:** calories eaten, protein, calories burned, eaten minus burned (your daily deficit or surplus), steps, and workout minutes for the week.
- **Body:** weight, body fat and BMR (how much you burn at rest).
- Tap **Connect** on Stats to allow heart rate, blood oxygen and BMR. "Not now" hides the card.

## v0.13 · Four tabs, less scrolling
- Four tabs: **Today**, **Stats**, **Progress** (your exercises and the challenge calendar) and **Goals** (back in its own tab).
- Progress opens on the type of your last workout. Tap Push, Pull or Legs again to fold the list away.
- Body stats on Stats are small tiles, two to a row. Tap one to open its chart, and tap again to close it.
- Perfect days, streak and average calories sit inside the calendar panel instead of above it.

## v0.12 · New Stats tab and watch body stats
- New **Stats** tab where Goals used to be. It has your overall strength as one number and chart, with Push, Pull and Legs lines beside it, plus body charts: weight, body fat, resting heart rate, heart rate variability and VO2 max, and the last 14 days of calories, sleep and steps.
- Body fat, resting heart rate, HRV and VO2 max now come in from Samsung Health. Tap **Connect** on the Stats tab to allow them. They're also saved in your backup file.
- **Goals** moved into Progress, under your workouts, as one panel. "+ New goal" opens the form when you need it.
- Anything you open closes the same way. Tap an exercise, its chart, or a goal's row again to close it, or press Back.
- More buzzes: every tap while logging a workout, opening an exercise's progress, switching tabs, and opening Settings.
- Removed "pull down to refresh" and "Today counted" from the screen. Pulling down still refreshes.

## v0.11 · Feels more like an app
- Light buzzes: ticking a task or goal step, saving a workout, hitting your calorie or protein goal, and picking up, passing and dropping an exercise while you drag it.
- Tabs slide in from the side they sit on.
- Pull down at the top of Today to refresh from Samsung Health. The arrow turns green when you've pulled far enough, then spins until it's done. The "Check now" link is gone.
- Little notices at the bottom fit on one line.
- No more text selection, long-press menus or pinch-zoom anywhere except the boxes you type in.
- Long-press the app icon for Push day, Pull day and Legs day shortcuts. Each one opens the app straight into that workout (or today's workout, if you've already started one).

## v0.10 · Drag to reorder, rename exercises, focused progress
- "Reorder or remove" is cleaner: hold an exercise (or grab its dots) and drag it where you want. The list scrolls if you drag near the edge. A normal swipe still just scrolls.
- Tap an exercise's name to rename it. If it's in other workouts too, you choose: rename it everywhere, or just in this workout.
- Renaming to a name you've used before joins the two, so their progress shows as one. If the same exercise ends up twice in one workout, the two merge, keeping every set.
- Removing an exercise shows Undo.
- Progress: an exercise you do on more than one day (like cable crunches on Push and Pull) shows under each of those days with its whole history, marked "also on Pull". Its session list says which day each one was.
- The exercise you open in Progress gets a green or red outline and a soft glow behind it and its chart.

## v0.9.4 · Food bars that count up
- When your totals change, from Samsung Health or after you type them, the number counts up to the new value while the bar grows and its color shifts with it.
- While you're typing, the bar dims and waits, and your number shows plainly.
- The bars start dim and brighten as they fill. Calories fades from blue to green over the last stretch to your goal, and red if you go over.

## v0.9.3 · Better food bars
- Calories is one big bar across the card, with Protein as a slimmer bar under it.
- The number starts in white and turns dark where the bar fills in behind it (the other way round in light mode), so it's always easy to read.

## v0.9.2 · Simpler strength chart and food bars
- Calories and Protein fill with solid color: blue on the way, green when you hit it, red if you go over. No outline, stripe or range marks.
- Strength chart: green and red line to match the shading, round numbers on the side, and one short est. 1RM line ("Best yet. Up 10 lb since day 40").

## v0.9.1 · Cleaner strength chart
- The strength line is green where you went up or held and red where you dropped, matching the shading. No more blue.
- Round numbers on the side (55, 60, 65) instead of odd decimals.
- One short line above the chart, marked est. 1RM: "Best yet. Up 10 lb since day 40", or your best to aim for. The long explanation is gone.

## v0.9 · Workout button, food bars and one-tap goals
- Log a workout from the red **+** in the bottom corner of Today. It opens Push, Pull and Legs (with when you last did each); once a workout is started, + opens it. The Push / Pull / Legs tile is gone, and the workout card only shows once there's a workout.
- The Calories and Protein boxes fill up like bars: blue on the way, green when you hit it, red if you go over. The bars underneath are gone, so there's more room for tasks and weight.
- Adding an exercise: **Next** on the keyboard goes from name to weight to reps, and the last one adds it. While the keyboard is open, the sheet keeps the whole form (with the Add button) in view above it, and Save steps aside until you're done typing.
- Fixed: typing calories and then tapping protein (or weight, then reps, in a workout) sometimes ignored the tap.
- Goals show three steps side by side: the one before (grey), the one you're on, and the next one. Tap the next one to tick it, right from the list; the steps slide over, and Undo appears in case it was a mistake. A tap that only stops a scroll never ticks anything.
- The strength chart is shaded under the line: green where it went up or held, red where it dropped.
- Update notices now say what's new as well as the version number (starting with the update after this one).

## v0.8.1
- The exercise name box no longer shows a dropdown of other days' exercises or a little arrow. Typing an exercise you've done before still fills in last time's weight and reps.

## v0.8
- Log sets one at a time. Every exercise starts at Set 1; + moves to the next set (copying the weight and reps), and the −/+ on weight and reps change only that set. Tap a set chip to go back to it, or remove it. Works the same for new and preloaded exercises.
- Next time, each exercise loads every set from last time, starting at set 1.
- Only the first exercise of a workout shows an example name ("Cable row") in the box.
- Settings puts Goals and Daily tasks first, and Backup last. The backup dot still jumps straight to it.
- The setup preview button is gone.

## v0.7.2
- Walks only count as cardio from 1 mile up, using the distance Samsung Health recorded, or about 2,000 steps per mile if it didn't record one. Counted walks show their distance, like "Walking 1.4 mi".

## v0.7.1
- "2 workouts" now means one Push, Pull or Legs saved in the app plus one cardio session from Samsung Health. "Other workout" and other lifting sessions in Samsung Health don't count on their own; their minutes show next to the workout you saved.
- The task row says what's still missing, like "1 / 2 · Push day 62 min, still need cardio".

## v0.7
- Workouts from Samsung Health (runs, rides, swims, rowing, racquet sports, skiing, hikes, walks, and "Other workout") count toward your workout task. Set how many a day under Goals in Settings. Each shows in the task row, like "2 / 2 · Push day 62 min, Running 32 min".
- A strength workout logged in Samsung Health on the same day as one saved here counts once, not twice.
- Tasks the app ticks itself come off again if you raise the target; ticks you make by hand stay.
- Sleeker day counter in the corner, with the settings gear built into the ring.
- Goals tab: an overall progress bar, goals grouped into Strength, Cardio and Body, and one compact row per goal with its own bar. Tap a goal to open its ladder.
- In setup, tapping the big numbers clears them so you can type a new one.
- The setup suggestion says which goal it's for ("Suggested for losing fat").

## v0.6
- New setup: "Welcome to [100] Days Hard" (tap the number to set your length), then "Today is day [1]" with your last day shown big.
- Calorie goal is either **Lose fat** (a 200-calorie range to land in) or **Build muscle** (a number to reach). Today, Progress and Settings all follow it.
- "Suggest for me" asks for your activity level and, if you know it, your body fat, and says exactly what's still missing.

## v0.5.1
- Setup's targets step has two modes: **Suggest for me** works out calories and protein from a few questions, or **I know my numbers** lets you type them in.
- A temporary **Preview first-time setup** button in Settings → Testing, to walk through setup without changing anything.
- The update check now runs every time the app comes back on screen, so it no longer says you're up to date when you aren't.
- Water is no longer read from Samsung Health. "Drink 3 L of water" is a tick-it-yourself task again.

## v0.5
- Tasks that tick themselves: water, steps and sleep, filled in from Samsung Health. Each shows today's number right in its row, like "7 h 35 m last night".
- Set your targets in Settings (defaults: 3 L of water, 10,000 steps, 9 hours of sleep).
- New tasks count from the day you add them, so adding one never breaks your streak.
- Progress shows the last 14 days of calories, sleep, steps or water in one chart, with averages.
- Choose how long your challenge is (30, 75, 100 or any number of days), in setup or Settings.
- A finish screen when your challenge ends, with your stats and a button to start another round.

## v0.4.1
- Adding an exercise: Weight, Reps and Sets sit in one row, with a small lb/kg badge in the weight box.
- Settings has an **App updates** section with a button that downloads the newest version.
- The sharing section is worded for anyone who's sharing the app: "Share this app with your friends".

## v0.4
- Streak counter on Today: days in a row with every daily task done, your last 7 days, and your best streak.
- Backup dot next to the date: green when a copy was saved off the phone in the last week, yellow after a week, red after two.
- First-time setup for new installs: challenge day, calorie and protein targets, daily tasks, Samsung Health.
- Share with friends from Settings: link, QR code, and a download page for Android and iPhone. Friends start fresh.
- The app tells you when a new version is ready.
- Goal ideas to start from on the Goals tab.

## v0.3
- Push / Pull / Legs workouts that load your last session's exercises, order, weights, reps and sets.
- Quick −/+ buttons (5 lb), and an lb/kg toggle on cable exercises.
- Strength list on the Progress tab: each exercise's trend, latest weight and reps, and whether it's up or down.
- Settings moved to the day counter in the corner.

## v0.2
- Daily calorie and protein totals instead of meal-by-meal logging.
- Samsung Health sync through Health Connect.
- Automatic backup file in Download/100 Days Hard, plus send and restore.
- Goals tab with your milestone ladders.

## v0.1
- First installable version.
