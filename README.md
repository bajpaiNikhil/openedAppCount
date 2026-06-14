# openedAppCount

> A **Daily Awareness Dashboard** for Android — see how many times you unlock your phone, which apps eat your attention, and which ones you open out of pure reflex. All computed live from the OS. No database. No backend. Nothing leaves the device.

Built with Kotlin and Jetpack Compose. Every number on screen is derived from Android's own `UsageStatsManager` event log at the moment you open the app — there is no persistence layer to leak, sync, or age out.

<p>
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-2026.02-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="minSdk 24" src="https://img.shields.io/badge/minSdk-24-3DDC84?logo=android&logoColor=white">
  <img alt="targetSdk 36" src="https://img.shields.io/badge/targetSdk-36-3DDC84?logo=android&logoColor=white">
  <img alt="No backend" src="https://img.shields.io/badge/data-100%25%20on--device-34D399">
</p>

---

## Screenshots

> _Add screenshots here once the v1.x UI redesign lands._

| Dashboard | Unlock timeline | Reflex check |
|---|---|---|
| _coming soon_ | _coming soon_ | _coming soon_ |

---

## What it does

openedAppCount turns Android's raw usage event stream into a single-screen awareness dashboard.

- **Unlock count** — how many times you've picked up the phone today, this week, or on a daily average.
- **Hourly unlock timeline** — a 24-bar view of *when* you reach for your phone, with tap-to-inspect on any hour and your most-active window surfaced automatically.
- **App usage leaderboard** — top apps by screen time, switchable between Today / Week / Average, with per-app accent colors and share-of-total percentages.
- **App-opens leaderboard** — ranks apps by how *often* you open them, independent of the screen-time view.
- **Reflex check** — fuses open counts with average session length to flag apps you open compulsively (many opens, tiny sessions) versus apps you actually sit with. This is an awareness signal, not a judgment.
- **Session breakdown** — splits your day into quick pickups (&lt;3 min), medium sessions (3–10 min), and long sessions (≥10 min), plus your average session length.
- **Streaks** — discipline and night-owl streaks tracked over a rolling 7-day window.
- **Home screen widget** — a Glance widget that shows today's unlock count and refreshes on every unlock.

---

## The interesting part: no database

Most usage-tracker apps log events into Room and slowly build their own history. openedAppCount deliberately does **not**.

Every metric is re-derived on each `onResume` straight from `UsageStatsManager` — the same event log Android's own Digital Wellbeing reads. The consequences are intentional:

- **Privacy by architecture.** There is no stored history to exfiltrate or back up. The app holds nothing the OS doesn't already hold.
- **Always accurate.** No drift between what the app recorded and what actually happened — the source of truth *is* the source.
- **Trivially auditable.** The entire data layer is a handful of pure queries in one file.

The trade-off is that the app can only show what the OS event log retains, and heavier views (week/average, sessions, streaks) run on a separate, slower query path so the unlock count and today's usage stay instant.

A couple of details that took real digging to get right:

- **System-app filtering.** Every list is gated on `pm.getLaunchIntentForPackage(pkg) != null` so the launcher, system services, and background packages don't pollute your leaderboards — while still counting the home screen itself, which never registers its own launcher intent.
- **`QUERY_ALL_PACKAGES`.** On Android 11+, `getLaunchIntentForPackage()` silently returns `null` for apps not declared in `<queries>`, which would make WhatsApp, YouTube, Chrome, etc. look like system apps and vanish from your usage. The permission is required to read your *own* usage honestly.

---

## Architecture

MVVM, single screen, zero persistence.

```
MainActivity
  └─ WellbeingViewModel (AndroidViewModel)
       └─ UsageStatsRepository
            └─ UsageStatsManager (Android system API)

Background:
  UnlockMonitorService  ──broadcast──▶  UnlockWidgetReceiver (Glance widget)
```

**Data flow**

1. `MainActivity.onCreate` starts the unlock-monitor service, schedules the widget refresh, and sets `WellbeingScreen` as content.
2. `onResume` runs the **fast path** — unlock count + today's usage + the hourly timeline.
3. The ViewModel runs the **extended path** once — leaderboard, sessions, streaks, and week data — so the screen paints immediately and fills in the heavier sections after.
4. `WellbeingScreen` reads ViewModel state and renders every section as an `item {}` inside one `LazyColumn`.

| Layer | File | Responsibility |
|---|---|---|
| Data | `UsageStatsRepository.kt` | All OS queries; shared `midnightOf(dayOffset)` date arithmetic; the system-app filter. |
| State | `WellbeingViewModel.kt` | `globalPeriod` drives hero cards + top apps; `leaderboardPeriod` drives app-opens independently; fast vs. extended refresh paths. |
| UI | `WellbeingScreen.kt` | Hosts the screen `LazyColumn` and layout primitives. |
| UI | `DashboardSections.kt` | The four feature sections: timeline, app-opens leaderboard, sessions, streaks. |
| Widget | `UnlockMonitorService.kt` / `UnlockWidgetReceiver.kt` | Foreground service + Glance widget that updates on every unlock. |

---

## Tech stack

- **Language:** Kotlin 2.2.10
- **UI:** Jetpack Compose (BOM 2026.02.01), Material 3
- **Widget:** Glance for AppWidget 1.1.1
- **Async:** Kotlin Coroutines
- **System APIs:** `UsageStatsManager`, `AppWidgetProvider`, foreground service
- **Min / Target SDK:** 24 / 36
- **Persistence:** none (by design)

---

## Permissions

| Permission | Why |
|---|---|
| `PACKAGE_USAGE_STATS` | Read the system usage event log — the entire data source. Granted manually via Settings ▸ Special app access ▸ Usage access. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | Keep the unlock-monitor service alive so the widget stays current. |
| `QUERY_ALL_PACKAGES` | Resolve launch intents on Android 11+ so real user apps aren't misclassified as system apps (see above). |

The app requests **no network permission.** It cannot phone home because it has no way to.

---

## Build & run

```bash
git clone https://github.com/bajpaiNikhil/openedAppCount.git
cd openedAppCount

# Debug build
./gradlew assembleDebug          # → app/build/outputs/apk/debug/

# Type-check only (fast)
./gradlew compileDebugKotlin
```

On first launch, grant **Usage access** when prompted (the system has no runtime dialog for it — it opens the Settings screen).

### Release builds

Release signing reads from a gitignored `local.properties`:

```properties
KEYSTORE_PATH=/path/to/release.jks
KEYSTORE_PASSWORD=********
KEY_ALIAS=********
KEY_PASSWORD=********
```

```bash
./gradlew assembleRelease        # → app/build/outputs/apk/release/
```

`local.properties` and `*.jks` are gitignored — keep them that way.

---

## Roadmap

- [x] Hourly unlock timeline
- [x] App usage & app-opens leaderboards
- [x] Session estimation
- [x] Streak tracking
- [x] Home screen unlock widget
- [ ] Custom Compose theme — committed dark color scheme + Space Grotesk / Space Mono type pair (replacing the Material baseline)
- [ ] Animated entry: staggered reveals, count-up numbers, timeline bars growing from zero
- [ ] Screenshots & Play Store listing

---

## Related reading

A companion deep-dive on the API this app is built on — querying `UsageStatsManager`, permission handling via `AppOpsManager`, and `KEYGUARD_HIDDEN` / `MOVE_TO_FOREGROUND` event filtering — is on Medium [@nikhil.cse16](https://medium.com/@nikhil.cse16).

---

## Author

**Nikhil Bajpai** — Senior Mobile Engineer
[GitHub](https://github.com/bajpaiNikhil) · [Medium](https://medium.com/@nikhil.cse16)

---

> Built local-first, on purpose. Awareness is the whole feature — the app shows you your habits and then gets out of the way.
