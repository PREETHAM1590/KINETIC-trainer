# KINETIC Trainer 🏆

> The trainer companion app — manage clients, assign workouts, chat securely, and track member progress.

![Android](https://img.shields.io/badge/Android-Kotlin-green?logo=android)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue?logo=jetpackcompose)
![Firebase](https://img.shields.io/badge/Backend-Firebase-orange?logo=firebase)
![E2E Encrypted](https://img.shields.io/badge/Chat-E2E%20Encrypted-purple?logo=lock)

---

## ✨ Features

### 👥 Client Management
- Real-time client list with health stats (streak, injuries, missed sessions)
- Client detail view with workout history and progress
- AI-powered insights (missed workouts, plateaus, achievements)

### 📋 Workout Assignment
- Create custom workout plans from 300+ exercise library
- Assign workouts to individual members
- Template library for quick assignment
- Members get push notification on new assignment

### 💬 Secure Chat
- End-to-end encrypted messaging (AES-256-GCM)
- Key exchange via Cloud Function
- Real-time message delivery
- Notification deep-linking (tap → opens chat)

### 🔔 Smart Notifications
- New message alerts
- Client missed session warnings
- Personal best celebrations
- Android 13+ runtime permission handling

---

## 🏗️ Architecture

```
app/
├── data/
│   ├── models/          # ClientSummary, ClientDetail, ChatMessage, etc.
│   ├── repository/      # FirebaseTrainerRepository, AuthRepository
│   ├── ChatEncryption.kt  # AES-256-GCM E2E encryption
│   └── SessionManager.kt  # Trainer session state
├── domain/
│   ├── TrainerInsightEngine.kt  # Rule-based client insights
│   └── errors/          # DomainError hierarchy
├── ui/
│   ├── screens/         # Home, ClientDetail, Chat, WorkoutAssignment
│   ├── viewmodels/      # MVVM ViewModels
│   ├── components/      # Reusable UI components
│   ├── navigation/      # NavGraph with deep-link support
│   └── theme/           # Dark theme
├── service/
│   └── TrainerMessagingService.kt  # FCM with retry logic
└── MainActivity.kt
```

**Tech Stack:**
- Kotlin + Jetpack Compose
- MVVM + Repository Pattern
- Hilt (DI)
- Firebase Auth, Firestore, Cloud Functions, FCM
- AES-256-GCM encryption
- Timber (logging)

---

## 🚀 Getting Started

```bash
git clone https://github.com/PREETHAM1590/KINETIC-trainer.git
cd KINETIC-trainer
```

1. Place `google-services.json` in `app/` (or set `GOOGLE_SERVICES_JSON_PATH` env var)
2. Build:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 🔒 Security

- Chat messages encrypted client-side before Firestore write
- ProGuard obfuscation enabled in release builds
- Firebase App Check (Play Integrity) in production
- No plaintext fallback — failed decryption shows `[Unable to decrypt]`

---

## 🔗 Related Repos

| Repo | Description |
|------|-------------|
| [KINETIC](https://github.com/PREETHAM1590/KINETIC) | Member fitness app |
| [KINETIC-admin](https://github.com/PREETHAM1590/KINETIC-admin) | Admin dashboard |

---

## 📄 License

Private — All rights reserved.
