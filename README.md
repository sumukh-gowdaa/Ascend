# Ascend — Gamified AI LifeOS for Android

**Ascend** is an offline-first, native Android personal productivity application ("LifeOS") built with **Kotlin**, **Jetpack Compose**, and **Firebase AI Logic (`gemini-3.8-flash`)**. It replaces guilt-driven task lists with personalized XP rewards, an Eisenhower Priority Matrix, a Ghost Task Detector for gentle reflection on procrastination, smart recurring reminders, and a context-aware Gemini AI Life Coach with live Google Calendar and Gmail tool integration.

---

## Core Features

- **Personalized Reward Zone & Onboarding Counseling:** Starts with a 4-question counseling flow (`hasCompletedRewardCounseling` via Jetpack DataStore) that captures your recharge preferences and XP budget, using either instant offline curation or **Gemini AI** to generate tailored, guilt-free rewards stored locally in Room.
- **Eisenhower Priority Matrix & Task Management:** Organizes daily commitments into *Do First*, *Schedule*, *Delegate*, and *Eliminate* quadrants with real-time XP progression (`+20 XP` per task).
- **Ghost Task Detector:** Automatically flags tasks postponed multiple times and surfaces them in a dedicated reflection view (*"It's too big"*, *"No time for it"*, *"Not important now"*) rather than marking them overdue.
- **One-Time & Recurring Reminders with Notification Actions:** Uses `AlarmManager` (`setExactAndAllowWhileIdle`), `BOOT_COMPLETED` rescheduling, and Android 13+ notification permissions to deliver exact reminders with interactive **Complete (+20 XP)** and **Snooze 15m** notification buttons.
- **Context-Aware AI Life Coach (Gemini Function Calling):** Integrates `gemini-3.8-flash` via Firebase AI Logic with native read-only OAuth scopes (`calendar.readonly` and `gmail.readonly`), allowing Gemini to invoke `getCalendarEvents` and `getRecentEmails` to answer questions using your live schedule and inbox.

---

## System Architecture

Ascend uses a **3-Layer Offline-First MVVM Architecture** with **Unidirectional Data Flow (UDF)** where the local Room SQLite database serves as the single source of truth:

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                        1. PRESENTATION LAYER (UI)                       │
│   Jetpack Compose Screens (Overview, Priorities, Ghost Tasks, Rewards,  │
│   AI Coach, MCP Connections) + System Notifications (Complete / Snooze) │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ Observes StateFlow / Sends Events
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       2. STATE & LOGIC LAYER (MVVM)                     │
│   ViewModels (Coroutine scopes, XP/Level math, Ghost Task rules,        │
│   Eisenhower quadrant filtering, Counseling questionnaire state)        │
└───────────────┬─────────────────────────────────────────┬───────────────┘
                │ Local Read/Write (Flow)                 │ AI & Cloud Calls
                ▼                                         ▼
┌───────────────────────────────┐     ┌───────────────────────────────────┐
│   3A. LOCAL DATA LAYER        │     │   3B. AI & CLOUD INTEGRATION      │
│ • Room DB (Tasks, Rewards,    │     │ • Firebase Auth (Google Sign-In)  │
│   Chat History)               │     │ • Firebase App Check (Security)   │
│ • DataStore (Onboarding flags)│     │ • Gemini 3.8 Flash (AI Coach)     │
│ • AlarmManager (Reminders)    │     │   └── Function Calling Tools:     │
└───────────────────────────────┘     │       ├─► Google Calendar REST API│
                                      │       └─► Gmail REST API          │
                                      └───────────────────────────────────┘
Tech Stack
Language & Async: Kotlin, Coroutines, StateFlow

UI: Jetpack Compose, Material 3

Local Storage: Room (SQLite), Jetpack DataStore

AI & Cloud: Firebase AI Logic (gemini-3.8-flash), Firebase Authentication, Firebase App Check

Integrations & Background: Google Calendar API v3, Gmail API v1, Android AlarmManager, BroadcastReceiver
Local Setup & Build Instructions
Clone the repository:

Bash
git clone [https://github.com/sumukh-gowdaa/Ascend.git](https://github.com/sumukh-gowdaa/Ascend.git)
cd Ascend
Add Firebase & API Configuration:

Place your Firebase Android configuration file at app/google-services.json (with your local debug.keystore SHA-1 registered in Firebase Console).

Copy .env.example to .env in the project root and add your Gemini API key:

Code snippet
GEMINI_API_KEY=your_gemini_api_key_here
Ensure a debug.keystore is present in the project root (or update signingConfigs in app/build.gradle.kts).

Enable Google APIs & App Check:

Enable Gmail API and Google Calendar API in Google Cloud Console and add your account under OAuth Test Users.

Register your Logcat DebugAppCheckProvider UUID under Firebase Console > App Check.

Build & Run:

Open the project in Android Studio, sync Gradle, and run on an Android device (API 24+).
