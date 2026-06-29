# Balance — a cozy, offline-first budget app for Android

A warm, tactile personal budget app. Single-user, offline-first, ₹ (INR). Built
with Jetpack Compose, Room (SQLCipher-encrypted), Hilt, and a deterministic
analytics engine with an optional on-device AI layer on top.

> **Status: Phase 1 complete** — project scaffold, encrypted Room schema, Hilt
> setup, the Quick-Add bottom sheet working from **both** the FAB and a deep
> link, and basic save + list. Usable end-to-end. Later phases add the
> analytics dashboard, budgets, reports/charts, auto-import, and the cozy
> agentic polish.

---

## Quick start

1. **Open in Android Studio** (Koala / Ladybug or newer). It will fetch the
   Gradle wrapper and sync automatically.
   - If you build from the command line, generate the wrapper jar once with a
     local Gradle: `gradle wrapper` (then use `./gradlew`). The wrapper
     *properties* are committed; the binary `gradle-wrapper.jar` is not.
2. **Run** the `app` config on a device/emulator (minSdk 34, Android 14+).
   Targeted device: **Samsung S24 Ultra** (One UI, Android 14+).
3. On first launch the default categories are seeded automatically.

No accounts, no network, no API keys required.

## The core loop (Phase 1)

**Add an expense in under two seconds.** A spring-driven bottom sheet with a
pre-focused number pad, one-tap category chips, an optional note, and an
editable date (for backfilling). Save fires a confirm haptic and a checkmark
that morphs in, then the sheet dismisses cleanly.

There are **two entry points into the exact same sheet and the exact same save
path** (no duplicated logic):

| Entry point | How |
|---|---|
| **In-app** | The ＋ FAB on the dashboard opens `QuickAddBottomSheet`. |
| **External** | `QuickAddActivity` — reachable by component launch *or* the deep link `balance://quickadd`. |

### Wiring the double-tap-back gesture (on your phone)

The app intentionally does **not** capture the double-tap-back gesture. Configure
it externally:

1. Install **Good Lock** → **RegiStar** from the Galaxy Store.
2. In RegiStar's *Back-tap* (double-tap the back gesture) action, choose to
   launch either:
   - the **QuickAddActivity** component directly, or
   - the deep link `balance://quickadd`.
3. Optional prefill via deep link (handy for testing or automation):
   ```
   balance://quickadd?amount=149.50&note=Coffee&merchant=Third%20Wave
   ```

Test the deep link with adb:
```
adb shell am start -a android.intent.action.VIEW -d "balance://quickadd?amount=250&note=Lunch"
```

## Architecture

Clean architecture, single Gradle module, layered `data / domain / presentation`,
grouped by feature. Single source of truth via Kotlin `Flow`.

```
core/        theme (cozy dark palette, expressive type, motion), haptics, utils (₹, dates)
data/        local (Room + SQLCipher, DAOs, entities, crypto, seed), repository, di (Hilt)
domain/      models, ai (pluggable provider), analytics (Phase 2)
feature/     quickadd, dashboard, (budgets/reports/history/settings in later phases)
navigation/  NavHost + routes
```

**Principle: deterministic engine, AI on top.** Every number — sums, breakdowns,
trends, projections, safe-to-spend — is plain Kotlin. The AI layer is only ever
asked for language/judgment (categorization hints, summaries, conversational
answers) and is fully optional. **AI is never used for arithmetic.**

### Money

All amounts are stored and computed as `Long` **minor units (paise)** — never
floating point. Formatting to ₹ (Indian digit grouping) happens only at the UI
edge in `core/util/Money.kt`.

### Data model (Room v1)

| Table | Notes |
|---|---|
| `categories` | Seeded defaults; editable/recolorable/archivable. |
| `expenses` | `amount_minor` (paise), `category_id`, `note`, `timestamp` (editable), `created_at`, `source`, `merchant`. |
| `budgets` | `category_id` null = overall budget; **versioned** by `effective_from_ym` so edits don't rewrite history. |
| `recurring` | Rent/subscriptions; feeds safe-to-spend. |

## Security — encryption at rest

The whole Room database is encrypted with **SQLCipher**. The passphrase is a
32-byte random value generated once on first launch and stored in
`EncryptedSharedPreferences`, sealed by a **Keystore-backed master key**
(hardware-backed on the S24). The key never leaves the device and is never
logged. Cloud backup / device-transfer of app data is disabled
(`data_extraction_rules.xml`). See `data/local/crypto/DatabaseKeyProvider.kt`.

## On-device & cloud AI (structure now, implementations later)

`domain/ai/AiProvider.kt` is the pluggable boundary. Phase 1 ships
`NoopAiProvider` (reports unavailable, so everything takes the deterministic
path). Later phases add:

- **On-device:** Gemini Nano via Android AICore / ML Kit GenAI — must detect
  availability at runtime and degrade gracefully if the device/model isn't ready.
- **Cloud fallback (optional, disabled by default):** a pluggable interface for
  the Gemini API free tier or Groq.

**Enabling cloud AI later:** drop your key into `local.properties` (never
committed) and read it via `BuildConfig`; wire a `CloudAiProvider` into
`AiModule`. **Never hardcode API keys.**

## Testing

- **Unit (JVM):** `MoneyTest` (parsing/rounding/format), `QuickAddViewModelTest`
  (number-pad input, validation, the single save path) — run `./gradlew test`.
- **UI (instrumented):** `AddExpenseFlowTest` drives the real Quick Add UI +
  view model against an in-memory Room DB and asserts one expense is persisted —
  run `./gradlew connectedAndroidTest`.

## Design language

Warm, tactile "cozy fintech" — the anti-spreadsheet. Dark-first espresso palette
with a signature warm amber accent; soft organic rounding; expressive display
type for the hero amounts (Bricolage Grotesque via downloadable Google Fonts,
with a system fallback). Motion is core: spring sheet, count-up totals, the save
morph, and S24 haptics throughout.

## Things to verify on your actual device

- **Downloadable fonts:** the Google Fonts certs in `res/values/font_certs.xml`
  are the standard public Play Services certs. If a font ever fails to fetch the
  app falls back to system fonts automatically — verify the display font renders
  on your phone.
- **SQLCipher native lib** loads on the S24 (it ships arm64-v8a).
- **Haptics:** confirm the save/select/tick feedback feels right on One UI.
- **RegiStar deep link / component launch** fires `QuickAddActivity` as expected.

## Roadmap

- **Phase 2:** Dashboard + Budgets + safe-to-spend, deterministic analytics
  engine, full motion/design polish.
- **Phase 3:** Reports + Vico charts + AI summaries (on-device first).
- **Phase 4:** Auto-import via NotificationListenerService (opt-in, notifications
  only — never SMS) + History search/edit.
- **Phase 5:** Conversational queries, forecasting, weekly money story, widget,
  streaks, export (PDF/CSV).
