# Changelog

All notable changes to the KOTOBAAN project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-09-19

### Added
- **Active Recall Cycle Engine**: Implemented binary Ingat / Lupa assessment with an automated recovery loop that isolates and re-queues forgotten vocabulary until full session mastery is achieved.
- **Multi-Bab Selection**: Support for selecting multiple chapters simultaneously in Pustaka and flashcard sessions.
- **50-Word Batches**: Chunked study sessions into 50-item groups for JFT verbs, adjectives, and kanji.
- **Bidirectional Navigation**: Added Previous (`← Prev`) navigation button enabling learners to step backward through the active queue without state loss.
- **Native iOS SwiftUI Port**: Native iOS client architecture in `ios/Kotoba` sharing the identical SQLite schema and database contract (`kotoba.db`).
- **Triple Front-Display Modes on iOS**: Added support for Kanji, Hiragana, and Indonesian meaning front card projections in SwiftUI.
- **Automated Data & Behavioral Test Suites**: Added `DataIntegrityAuditTest.java`, `FlashcardRepresentationIntegrityTest.java`, `PustakaCardLayoutContractTest.java`, and `test_ios_behavior_contract.py`.
- **Automated iOS Cloud CI Pipeline**: Added `.github/workflows/build-ios.yml` on Apple Silicon macOS runners to automatically compile, package, and publish `Kotoba.ipa` artifacts.
- **Shared Xcode Build Scheme**: Configured `Kotoba.xcscheme` under `xcshareddata` for headless CI build execution.
- **iOS Sideloading Guide**: Added `docs/IOS_SIDELOAD_GUIDE.md` covering installation via Sideloadly and AltStore with standard free Apple IDs.

### Changed
- **Pustaka Card Geometry**: Redesigned library list items into a 3-row floating card architecture with independent padding and transparent dividers, eliminating awkward romaji line wrapping (`kir`/`ei`) and vertically displaced badges.
- **Kanji Reading Selector**: Refactored `content/process_additional_kanji.py` to prioritize high-frequency Kun-yomi over uncommon On-yomi marked with `!` in dictionary sources.
- **Cross-Platform TTS Audio Contract**: Standardized front and back audio synthesis across both Android (`FlashcardView.java`) and iOS (`FlashcardViewModel.swift`) to strictly pronounce the authoritative reading string (`item.reading`), eliminating phonetic guessing discrepancies upon card flips.
- **Furigana Preservation**: Ensured reading answer fields on the back face of flashcards remain permanently visible regardless of front-face furigana toggle state.

### Fixed
- **Kanji Tambahan #701 Reading**: Corrected entry #701 (`釣`) from obscure reading `ちょう` (`chou`) to standard everyday reading `つり` (`tsuri`), resolving audio and display desynchronization.
- **Systemic Obscure On-yomi Overrides**: Repaired 189 Additional Kanji entries where rare On-yomi overrode universal Kun-yomi readings (including `彼`, `田`, `桜`, `呼`, `払`, `鳴`, `咲`).
- **Canonical Romaji Typo Corruptions**: Corrected 25 entries in `kanji_dataset.json` where the string `mo` was corrupted to Japanese Hiragana `も` (`もkuyoubi` → `mokuyoubi`, `kodoも` → `kodomo`, `toもdachi` → `tomodachi`), and sanitized full-width `ＪＦ` to standard ASCII `JF`.
- **Duplicate Reading Render**: Suppressed redundant reading text in vocabulary cards when the reading string is identical to the main Japanese headword (common in Katakana loanwords).
- **Dark Mode Card Contrast**: Standardized card background and stroke colors in Obsidian Violet theme to prevent text clipping against container borders.
