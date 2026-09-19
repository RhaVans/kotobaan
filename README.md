# KOTOBAAN

Offline Japanese vocabulary and kanji learning application for Indonesian learners.

<p align="center">
  <img src="docs/assets/app_icon.png" width="120" height="120" alt="KOTOBA Application Icon" />
</p>

## Overview

KOTOBAAN is an offline-first Japanese language acquisition tool designed primarily for Indonesian students and technical trainees. It combines structured curriculum vocabulary, extensive kanji references, active-recall flashcards, and phonetically synchronized Japanese text-to-speech pronunciation.

The application operates completely offline without network access, third-party user accounts, or external API dependencies. All datasets, progress tracking, and scheduling algorithms execute locally on-device.

## Features

- **Structured Curriculum Vocabulary**: 863 vocabulary entries organized across 25 progressive chapters (Bab 1–25) with grammatical classification (nouns, verbs, adjectives, particles, expressions).
- **Comprehensive Kanji Collection**: 613 canonical JFT-Basic A2 / Irodori kanji and 2,119 supplementary kanji entries organized into sequential 50-character batches.
- **Three-Way Card Presentation**: Toggle between Kanji front, Hiragana/Katakana reading front, or Indonesian meaning front.
- **Active-Recall Cycle Engine**: Binary Ingat (Remembered) and Lupa (Forgotten) evaluation with a dedicated recovery queue that re-tests unlearned items until full session mastery.
- **Synchronized Audio Pronunciation**: Pronunciation synthesis powered by system `TextToSpeech` (`Locale.JAPANESE`), guaranteeing phonetic fidelity between front and back card faces.
- **Bidirectional Navigation**: Immediate review of previous flashcards without interrupting session state or queue order.
- **Pustaka Vocabulary Browser**: Searchable lexicon organized by chapter, part of speech, and kanji groupings with a structured 3-row layout.
- **Theming**: High-contrast Obsidian Violet dark theme and warm paper canvas light theme.

## Platforms and Builds

| Build Target | Platform | Binary Size | Runtime Audio Engine | Status |
|---|---|---|---|---|
| **KOTOBAAN (Release)** | Android (API 26+) | ~837 KB | System TextToSpeech | Stable (v2.0.0) |
| **Kotoba iOS** | iOS (iOS 16.0+) | Shared SQLite contract | System AVSpeechSynthesizer | Experimental port |

The primary release artifact is **`KOTOBAAN.apk`**, a self-contained package with zero external binary dependencies.

## Installation

### Android

1. Download `KOTOBAAN.apk` from the [GitHub Releases](https://github.com/RhaVans/kotobaan/releases) page.
2. Transfer the file to your Android device (Android 8.0 Oreo / API 26 or newer).
3. Open your device file manager, locate the downloaded APK, and tap to install.
4. If prompted, grant permission to install from unknown sources for your file manager.
5. Ensure that a Japanese text-to-speech engine (such as Google Speech Services) is enabled in your device system settings (`Settings → System → Languages & input → Text-to-speech output`).

## Development

### Prerequisites

- Linux or macOS development environment
- Java Development Kit (JDK) 8 or 17
- Python 3.8+ (for dataset pipeline and database compilation)
- Android SDK build tools (`aapt`, `d8`, `zipalign`, `apksigner`, and `android.jar` for API 26+)

### Project Structure

```text
kotobaan/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/              # Seed JSON datasets and pre-compiled SQLite database
│       ├── java/com/kotoba/app/ # Android UI, audio, database, and SRS engines
│       └── res/                 # Layouts, themes, drawables, and vector assets
├── content/                     # Dataset generators, validators, and SQLite compiler
├── docs/                        # Technical architecture notes and user guide
├── ios/                         # Native SwiftUI iOS client port
├── releases/                    # Release artifacts and checksums
├── tests/                       # Java unit tests and iOS database contract tests
└── tools/                       # Build scripts and test runners
```

### Running Tests

Execute the automated test suite covering SRS scheduling, active recall recovery, layout contracts, and phonetic synchronization:

```bash
bash tests/run_java_tests.sh
```

To run the iOS SQLite database contract and structure tests:

```bash
python3 tests/test_ios_db_contract.py
python3 tests/validate_xcode_project.py
```

### Building KOTOBAAN.apk

To compile, assemble, align, and sign the lightweight production APK:

```bash
bash tools/build_kotobaan.sh
```

The output package will be placed at the project root as `KOTOBAAN.apk`.

## Data Sources

The learning material in KOTOBAAN is assembled and normalized from established Japanese language education sources:

- **Curriculum Vocabulary (Bab 1–25)**: Derived from the standard IM Japan training curriculum for technical interns.
- **Kanji (613 Canonical)**: Based on the JFT-Basic A2 and Irodori Japanese language course frameworks established by the Japan Foundation.
- **Additional Kanji (2,119)**: Extracted and normalized from open dictionary sources including Kanjidic and JMDict.
- **Translations**: Indonesian translations and linguistic classifications curated and reviewed specifically for Indonesian native speakers.

## Status

KOTOBAAN is actively maintained. Version `2.0.0` represents a stable milestone featuring repaired kanji reading synchronization, non-destructive flashcard furigana preservation, and the 3-row vocabulary browser card layout.

## License

Copyright (c) 2026 RhaVans.

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
