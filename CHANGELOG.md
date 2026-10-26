# Changelog

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
