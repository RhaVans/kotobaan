# KOTOBAAN v2.0.0 Release Notes

**Release Date:** September 19, 2026  
**Application Name:** KOTOBA  
**Package ID:** `com.kotoba.app`  
**Version Code:** `8`  
**Version Name:** `2.0.0-FINAL`  
**Target Platform:** Android 8.0+ (API 26+)  

---

## Release Assets

| Asset File | Size | SHA-256 Checksum |
|---|---|---|
| **`KOTOBAAN.apk`** | **856,269 bytes (~837 KB)** | `25f283ce1cc34a324c6078e8224ffe3bace2455d6a3800408f7e5dcfbd336029` |

### Cryptographic Signatures
- **JAR Signing (v1):** Verified
- **APK Signature Scheme v2:** Verified
- **APK Signature Scheme v3:** Verified

---

## Highlights in v2.0.0

- **Data Integrity Overhaul**: Repaired 189 Additional Kanji entries to prioritize standard Kun-yomi over obscure On-yomi readings (specifically correcting entry #701 `釣` to `つり` / `tsuri`). Corrected 25 canonical kanji romaji typing corruptions.
- **Symmetrical TTS Speech**: Ensured exact phonetic consistency between front and back card faces using system `TextToSpeech`.
- **Pustaka 3-Row Card Layout**: Structured vocabulary cards into isolated floating rows with clear margins, eliminating cramped romaji line breaks and displaced category badges.
- **Active Recall Engine**: Binary Ingat/Lupa progression with automated recovery cycles.
- **Multi-Bab Selection**: Multi-chapter study filtering and 50-item batches.

---

## Installation Instructions

1. Download **`KOTOBAAN.apk`** to your Android device.
2. Verify checksum if desired:
   ```bash
   sha256sum KOTOBAAN.apk
   # Output must match: 25f283ce1cc34a324c6078e8224ffe3bace2455d6a3800408f7e5dcfbd336029
   ```
3. Tap the downloaded APK to install.
4. Ensure Japanese language voice data is installed on your device for pronunciation output.
