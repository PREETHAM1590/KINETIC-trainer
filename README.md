# KINETIC Trainer

Android trainer app for the KINETIC platform.

## Overview
KINETIC Trainer is used by gym trainers to:
- authenticate into trainer workflows
- view assigned clients
- inspect client details
- assign workouts
- send secure chat messages to clients

## Tech Stack
- Kotlin, Jetpack Compose, Material 3
- Hilt (dependency injection)
- Firebase Auth, Firestore, Firebase Messaging
- DataStore + Android Security Crypto
- Signal Protocol Android library for encrypted chat payload handling
- Min SDK 26, Target SDK 35, Java 17

## Prerequisites
- Android Studio (latest stable recommended)
- JDK 17
- Android SDK 35
- Firebase project configured for Android + FCM
- Firebase config provided via `GOOGLE_SERVICES_JSON_PATH` or local `app/google-services.json`

## Setup

```powershell
cd kinetic-trainer
```

1. Preferred: set `GOOGLE_SERVICES_JSON_PATH` to a real Firebase config file path.
2. Alternative: copy your Firebase file to `app/google-services.json`.
3. For debug-only local runs without real credentials, copy `app/google-services.json.template` to `app/google-services.json`.
4. Sync Gradle in Android Studio or run from CLI.

Example (PowerShell):

```powershell
$env:GOOGLE_SERVICES_JSON_PATH = "D:/secrets/kinetic-trainer-google-services.json"
```

Release builds fail fast if template values are detected.

## Run And Test

```powershell
cd kinetic-trainer
.\gradlew assembleDebug
.\gradlew installDebug
.\gradlew testDebugUnitTest
.\gradlew :app:assembleRelease
```

Optional instrumentation tests:

```powershell
.\gradlew connectedDebugAndroidTest
```

## Firebase Config Hygiene

- `app/google-services.json` is intentionally ignored by git.
- CI should inject config with `GOOGLE_SERVICES_JSON_PATH` using a secret-backed file.
- Verify ignore behavior:

```powershell
git check-ignore -v app/google-services.json
```

## Navigation Flow
- Login
- Home (client list)
- Client Detail
- Workout Assignment
- Chat

## Project Structure

```text
kinetic-trainer/
  app/
    src/main/java/com/kinetic/trainer/
      data/
      di/
      domain/
      ui/navigation/
      ui/screens/
      ui/viewmodels/
    src/test/
  gradle/
  build.gradle.kts
  settings.gradle.kts
```

## Branching
- Default branch: master (current remote setup)
- Use feature branches and raise PRs into master
