# Kotoba.app — Architectural Blueprint & System Manual

**Kotoba.app** is a free, local-first Japanese learning application designed primarily for Indonesian learners.

---

## 1. Core Learning Loops

$$\text{Materi Asli} \longrightarrow \text{Kurikulum IM JAPAN} \longrightarrow \text{Learning Object} \longrightarrow \text{Kartu Belajar} \longrightarrow \text{Tinjauan Aktif} \longrightarrow \text{Model Retensi (SRS)} \longrightarrow \text{Analisis Titik Lemah}$$

- **Bab 1–25 Foundation (N5)**: 863 authentic vocabulary items, 613 JFT/JLPT Kanji, and 41 atomic grammar patterns with authentic bilingual examples.
- **Strict Chapter Isolation**: When studying Bab 12, all vocabulary cards originate strictly from Bab 12 (no global fallbacks or random kanji substitutions).

---

## 2. Directory Layout (`/storage/emulated/0/download/PROJECT/KTB`)

```
/storage/emulated/0/download/PROJECT/KTB/
├── Kotoba.apk                                # Production signed APK (v2+v3 scheme)
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml              # Package com.kotoba.app, SDK 26..34
│       ├── assets/
│       │   ├── databases/kotoba.db          # Canonical SQLite pre-compiled curriculum database
│       │   ├── kotoba_dataset.json          # 863 N5 vocabulary items
│       │   └── kanji_dataset.json           # 613 JFT/JLPT Kanji items
│       ├── java/com/kotoba/app/
│       │   ├── MainActivity.java            # 4-destination navigation & activity lifecycle
│       │   ├── audio/
│       │   │   ├── JapaneseSpeechHelper.java # Native Android TextToSpeech wrapper
│       │   │   └── SoundManager.java         # SoundPool tactile UI effects
│       │   ├── data/
│       │   │   ├── KotobaDatabase.java       # SQLite helper & progress persistence
│       │   │   ├── PreferencesManager.java   # SharedPreferences manager
│       │   │   └── model/
│       │   │       ├── Chapter.java          # Bab curriculum entity
│       │   │       ├── LearningObject.java   # Unified Vocab / Kanji / Grammar entity
│       │   │       └── UserProgress.java     # SRS parameters & mastery states
│       │   ├── engine/
│       │   │   ├── ReviewEngine.java         # Question generator with zero answer leaks
│       │   │   ├── SrsScheduler.java         # Dual-state spaced repetition algorithm
│       │   │   └── WeaknessDetector.java     # Latency profiler & weakness diagnosis
│       │   └── ui/
│       │       ├── BabSelectorDialog.java    # Chapter chooser (Bab 1..25)
│       │       ├── home/HomeViewController.java
│       │       ├── learn/LearnViewController.java
│       │       ├── review/ReviewViewController.java
│       │       └── progress/ProgressViewController.java
│       └── res/                              # Layouts, drawables, colors, styles
├── content/
│   ├── build_kotoba_database.py             # Pre-compilation pipeline for kotoba.db
│   └── validate_curriculum.py               # Curriculum integrity test runner
├── tests/
│   ├── SrsSchedulerTest.java
│   ├── WeaknessDetectorTest.java
│   ├── ReviewEngineTest.java
│   └── run_java_tests.sh
└── tools/
    ├── build_apk.sh                         # Standalone Android compilation script
    └── run_all_tests.sh                     # Full test suite runner
```

---

## 3. Review Engine & Anti-Answer-Leak Guarantee

`ReviewEngine.java` partitions each question into:
- **`SafeData`**: Rendered during the active testing state. Contains strictly prompt instructions, target subject, and 4 shuffled choices. **Contains 0 answer fields, 0 hints, 0 reading leaks.**
- **`RevealData`**: Rendered only after answer submission. Contains correct index, explanation, grammar formula, and updated SRS status.

---

## 4. Spaced Repetition (SRS) & Weakness Profiling

- **Latency Categories**:
  - $< 800\text{ ms}$: Impulsive / lucky guess
  - $800 - 4000\text{ ms}$: Fluent recall
  - $4000 - 10000\text{ ms}$: Hesitant recall
  - $> 10000\text{ ms}$: Struggling recall (flags item as weak regardless of correctness)
- **Quarantine Rule**: $\ge 2$ consecutive errors quarantines the item for 4 hours to prevent frustration spirals.
- **Mastery States**: `○ Baru`, `▲ Belajar`, `▼ Lemah`, `● Stabil`, `◆ Terkuasai`.

---

## 5. Design System: Scholarly Mode

- Signature Violet: `#6d28d9` (Light) / `#8b5cf6` (Dark) reserved strictly for primary interactive CTAs.
- Canvas: Warm paper `#faf7f2` (Light) / `#121016` (Dark).
- Typography: High-contrast `serif` for Japanese characters; clean `sans-serif` for Indonesian explanations.
- Footer Attribution: `Made by RhaVans`.

---

## 6. Cross-Platform Architecture: Option C (Shared Core & Data Engine)

```text
                     KOTOBAAN CANONICAL CORE
     (Single Source of Truth: SQLite Schema, Lexicons, SRS Algorithms)
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
     Android Platform                      iOS Platform
     • Java / Android SDK                  • Swift / SwiftUI
     • Android Views & SoundPool           • Native SwiftUI & Haptics
     • android.speech.tts.TextToSpeech     • AVFoundation AVSpeechSynthesizer
     • SQLiteOpenHelper                    • SQLite3 C-API Service
```

### Principles & Guarantees:
1. **One Product, One Canonical Database:**
   Both platforms consume the identical pre-compiled SQLite database (`kotoba.db`, 1,904,640 bytes, 4,363 entries) generated deterministically by `content/build_kotoba_database.py`. Zero data duplication exists between platforms.
2. **Behavioral & Mathematical Parity:**
   - **SRS Intervals:** Leitner 5-box intervals (1, 6, 17, 49+ days) and latency response penalties (<800ms impulsive, 800–4000ms fluent, >10s struggling) are mathematically identical in `SrsScheduler.java` and `SrsScheduler.swift`.
   - **Active-Recall Recovery Loop:** Both `IngatLupaEngine.java` and `IngatLupaEngine.swift` enforce strict session closure: any forgotten item (`LUPA`) is isolated into recovery passes until 100% mastery is achieved.
   - **TTS Audio Symmetry:** Both platforms pass `item.reading` (not raw kanji ideographs) to system speech synthesizers (`TextToSpeech` on Android, `AVSpeechSynthesizer` on iOS), eliminating phonetic guessing discrepancies.
   - **Triple Front-Display Modes:** Both platforms support Kanji front, Hiragana/Katakana front, and Indonesian Meaning front display projections.
3. **Automated Cross-Platform Verification:**
   - Android Java test suite: `tests/run_java_tests.sh` (13 suites)
   - iOS SQLite database contract: `tests/test_ios_db_contract.py` (7 tests)
   - iOS behavioral and algorithm contract: `tests/test_ios_behavior_contract.py` (8 tests)
   - iOS Xcode project integrity: `tests/validate_xcode_project.py` (5 tests)

