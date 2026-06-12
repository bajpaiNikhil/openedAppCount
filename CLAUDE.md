# openedAppCount — CLAUDE.md

## Project overview
Android app (Kotlin + Jetpack Compose) that tracks phone unlocks and app screen time using the system `UsageStatsManager` API. No backend, no Room database — all data is queried live from the OS event log.

Current release: **v1.1 — Daily Awareness Dashboard**

---

## Search before read

**Always grep before opening a file.** The codebase is small but the rule prevents wasted reads.

```bash
# Where is a symbol defined?
grep -rn "fun midnightOf"        app/src
grep -rn "class WellbeingViewModel" app/src

# Which files touch a concept?
grep -rl "KEYGUARD_HIDDEN"       app/src
grep -rl "LeaderboardPeriod"     app/src

# Where is a composable called?
grep -rn "UnlockTimelineSection" app/src
grep -rn "AppUsageRow"           app/src

# Where is a color token used?
grep -rn "BlueAccent"            app/src
grep -rn "ScreenBg"              app/src

# What state variables exist in the ViewModel?
grep -n "mutableStateOf\|mutableIntStateOf" app/src/main/java/com/example/openedappcount/WellbeingViewModel.kt

# Which files import a given class?
grep -rl "import com.example.openedappcount.UsageStatsRepository" app/src
```

Never `Read` a file just to find where something is — grep first, then read only the specific file/range you need.

---

## Architecture

```
MainActivity
  └─ WellbeingViewModel (AndroidViewModel)
       └─ UsageStatsRepository
            └─ UsageStatsManager (Android system API)

Background:
  UnlockMonitorService  ─── broadcasts → UnlockWidgetReceiver
```

**MVVM, single screen, no persistence.** All data lives in OS event logs; the app re-queries on every resume.

### Data flow
1. `MainActivity.onCreate` → starts `UnlockMonitorService`, schedules widget alarm, sets `WellbeingScreen` as content.
2. `MainActivity.onResume` → calls `vm.refresh()` (fast: unlock count + today's usage) and `vm.refreshTimeline()` (timeline bars).
3. `WellbeingViewModel.init` → also calls `refreshExtended()` once (leaderboard, sessions, streaks, week data).
4. `WellbeingScreen` reads ViewModel state; all sections are items inside a single `LazyColumn`.

---

## Key files

| File | Purpose |
|---|---|
| `UsageStatsRepository.kt` | All OS queries. `midnightOf(dayOffset)` is the shared date-arithmetic helper. `getLaunchIntentForPackage` filter strips system/launcher apps from every list. |
| `WellbeingViewModel.kt` | `globalPeriod` drives hero cards + top apps (Today/Week/Avg). `leaderboardPeriod` drives app-opens section independently. Two refresh paths: `refresh()` fast, `refreshExtended()` heavy. |
| `WellbeingScreen.kt` | Hosts the full screen `LazyColumn`. `ListCard {}`, `SectionLabel()`, `HeroCard()`, `ExtendedLoadingCard()` are private layout primitives defined here. |
| `AppUsageRow.kt` | Top-apps row. Per-app accent color via `AppAccentColors[abs(name.hashCode()) % size]`. Also owns `formatDuration()`. |
| `DashboardSections.kt` | Four composable sections: `UnlockTimelineSection`, `AppOpenLeaderboardSection`, `SessionSection`, `StreakSection`. Timeline has local `selectedBin` state for tap-to-inspect. |
| `ui/theme/Color.kt` | Single source of truth for all color tokens. `AppAccentColors` list cycles per-app. `BarPeak/High/Mid/Empty` for timeline tiers. |
| `ui/theme/Theme.kt` | ⚠️ Currently uses `dynamicColor = true` — violates compose_aesthetics. Must be replaced with a committed dark-only `ColorScheme`. |
| `ui/theme/Type.kt` | ⚠️ Currently `FontFamily.Default` (Roboto) — violates compose_aesthetics. Must be replaced with a custom font pair. |
| `UnlockMonitorService.kt` | Foreground service listening for `ACTION_USER_PRESENT`, triggers widget refresh on every unlock. |
| `UnlockWidgetReceiver.kt` | `AppWidgetProvider`. Schedules 15-min refresh alarms; also fires on `ACTION_USER_PRESENT` and device reboot. |

---

## Data model

No Room DB. All data classes are plain Kotlin:

| Class | Key fields |
|---|---|
| `AppUsageInfo` | `packageName`, `appName`, `totalTimeInMillis` |
| `AppOpenCount` | `packageName`, `appName`, `count: Int` |
| `UnlockTimeline` | `hourlyData: List<Int>` (24 buckets), `mostActiveHour`, `morningCount`, `nightCount` |
| `SessionSummary` | `quickPickups`, `mediumSessions`, `longSessions`, `avgSessionMinutes` |
| `StreakInfo` | `title`, `streakDays`, `isActive`, `last7Days: List<Boolean>`, `type: StreakType` |

---

## Repository query patterns

```kotlin
// All time-range queries share midnightOf():
private fun midnightOf(dayOffset: Int): Long   // 0=today, 1=yesterday

// System-app filter applied everywhere:
pm.getLaunchIntentForPackage(pkg) != null       // keep only user-launchable apps

// The parametric usage query (used for today/week/avg):
repository.getAppUsageInRange(startMs, endMs)
repository.getAppUsageStats()                   // convenience wrapper: midnight(0)→now
```

---

## Build commands

```bash
./gradlew assembleDebug          # debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease        # signed release APK → app/build/outputs/apk/release/
./gradlew compileDebugKotlin     # type-check only, faster
```

Signing credentials live in `local.properties` (gitignored). Never commit that file.
The keystore `release.jks` is also gitignored.

---

## Compose aesthetics

<compose_aesthetics>
You tend to converge on generic, "default Material" output — the look every
Android app ships with. Avoid this. Build a UI with a deliberate point of view.

TYPOGRAPHY
- Never ship default Roboto or the bare MaterialTheme.typography.
- Define a custom FontFamily. Pull a distinctive face via the Google Fonts
  provider (androidx.compose.ui.text.googlefonts) or bundle a .ttf in res/font.
- Pair a characterful display/heading font with a clean text or monospace face.
  A monospace for numbers (unlock counts, timestamps) reads as "instrumentation"
  and fits a tracking app.
- Use weight and size extremes: 200 vs 800, 3x size jumps — not 400 vs 600.
- State the font choice and why before writing code.

COLOR & THEME
- Do NOT default to dynamicColorScheme(context). Dynamic color is exactly what
  makes apps interchangeable. Commit to a custom brand ColorScheme via
  lightColorScheme()/darkColorScheme().
- Centralize tokens. For anything Material doesn't cover (chart series, gradient
  stops, streak states) expose a custom token object through
  staticCompositionLocalOf — this is the Compose answer to CSS variables.
- One dominant color + one sharp accent beats a timid, evenly-spread palette.
- Support a real dark theme, not an inverted light one.

MOTION
- Orchestrate one high-impact moment over scattered micro-interactions.
- On screen entry, stagger reveals: per-item delay keyed on index, with
  AnimatedVisibility (fadeIn + slideInVertically) or an Animatable timeline.
- Animate data: count-up numbers (animateIntAsState), bars/timelines that grow
  from zero on first composition, color transitions for state.
- Use rememberInfiniteTransition sparingly for ambient life (a subtle pulse on
  an active streak), never everywhere.

BACKGROUNDS & DEPTH
- No flat solid fills. Layer Brush.linearGradient / radialGradient, draw
  geometric texture with Modifier.drawBehind { } on a Canvas, use graphicsLayer
  blur for atmosphere.
- Build depth with surface layering and tone, not just default Card elevation.

AVOID (these are the "AI slop" tells on Android)
- dynamicColor + baseline purple Material theme
- A vertical scroll of identical filled Cards
- Default CenterAlignedTopAppBar with no identity
- Roboto everywhere, 400/600 weights only, flat backgrounds

Make unexpected, context-specific choices. Think outside the box — it is
critical you don't fall back to the safe default.
</compose_aesthetics>

### Current violations to fix (tracked)
- `ui/theme/Theme.kt` — `dynamicColor = true` and baseline `Purple80/Purple40` scheme. Replace with a committed dark-only `darkColorScheme()` using the design tokens already in `Color.kt` (`ScreenBg`, `BlueAccent`, `CardBg`, etc.).
- `ui/theme/Type.kt` — `FontFamily.Default` everywhere. Add the Google Fonts dependency and define a custom font pair. Suggested: **Space Grotesk** (headings, weights 300–800) + **Space Mono** (numbers/data). Both fit the instrumentation aesthetic of a phone-tracking app and are already visually implied by the current `FontFamily.Monospace` usage on counts.

---

## Git workflow

- **Never push to remote unless explicitly told to.** `git add` and `git commit` are fine during normal work. `git push` (and `git push --force`) require an explicit instruction like "push this" or "publish these changes".
- **Never commit signing credentials.** `local.properties` and `*.jks` are gitignored — keep them that way.

---

## Conventions

- **System app filtering** — always gate data queries with `pm.getLaunchIntentForPackage(pkg) != null`. Never remove this; it's what keeps the system launcher off every list.
- **Date arithmetic** — use `midnightOf(dayOffset)` in `UsageStatsRepository`; never compute epoch offsets manually.
- **Color tokens** — all colors go in `ui/theme/Color.kt`. Never hardcode `Color(0xFF...)` inline in composables.
- **No Room DB** — do not add persistence unless a feature explicitly requires data the OS event log cannot provide.
- **Single-screen layout** — all sections are `item {}` blocks in `WellbeingScreen`'s `LazyColumn`. Avoid introducing navigation until there is a clear need for a second screen.
