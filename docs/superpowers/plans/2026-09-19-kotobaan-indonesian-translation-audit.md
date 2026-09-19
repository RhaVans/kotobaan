# KOTOBAAN — Full Indonesian Translation Data Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Audit, repair, and rebuild the complete translation dataset for `KOTOBAAN.apk`, replacing all English dictionary glosses and mixed translations across all 4,363 learning objects with natural, context-accurate Indonesian translations while maintaining 100% Japanese linguistic and structural data integrity.

**Architecture:** 
1. Trace and fix the root cause in the dataset generator pipeline (`content/process_additional_kanji.py` and `content/build_kotoba_database.py`).
2. Build an intelligent translation and audit engine (`content/repair_kanji_translations.py` and `content/audit_translation_dataset.py`) combining curated Japanese Kanji semantic definitions with multi-signal English detection.
3. Repair all affected records in `kanji_additional_dataset.json` (2,046 entries), `kotoba_dataset.json` (4 entries), `kanji_dataset.json` (2 entries), and `kata_sifat_dataset.json` (5 entries).
4. Bump `KotobaDatabase.java` to `DATABASE_VERSION = 3` with an automatic stale-database detector to ensure existing installations cleanly redeploy the Indonesian dataset.
5. Recompile SQLite database `kotoba.db`, sync to Android assets and iOS resources, re-run all 11 Java and iOS test suites, and package and sign `KOTOBAAN.apk`.
6. Perform an automated post-build scan directly on the final packaged APK database.

**Tech Stack:** Python 3 (SQLite, JSON, regex, linguistic analysis), Java 8 / Android SDK 35 (SQLiteOpenHelper, D8, AAPT, apksigner), Swift (SQLite3), Bash.

---

## Global Constraints

- Total learning objects in database must remain exactly **4,363**.
- Unrelated fields must remain **100% untouched**: `id`, `kanji`, `reading`, `romaji`, `badge_label`, `group_label`, `sort_order`, `details_json`, `word_type`, `level`, and `bab`.
- Zero English dictionary glosses, prepositions, or grammatical patterns in the `indonesian` column.
- Both Android (`KOTOBAAN.apk`) and iOS (`kotoba.db`) must stay in perfect contract parity.
- Evidence before assertions: every fix must be verified with automated scripts reporting measurable metrics.

---

## Root Cause Analysis

### Pipeline Trace
```mermaid
flowchart TD
    A["KANJIDIC2 Reference (content/kanji_dict_ref.json)<br/><i>English meanings: ['assurance', 'firm', 'tight']</i>"] --> B["Generator Script (content/process_additional_kanji.py)<br/><i>EN_TO_ID dictionary: only 50 hardcoded words</i>"]
    B -->|Unmatched fallback: translated.append(m)| C["Assets (app/src/main/assets/kanji_additional_dataset.json)<br/><i>1,577 completely English + 469 mixed entries</i>"]
    D["Curriculum & JFT Assets<br/><i>kotoba_dataset, kata_sifat, kanji 613</i>"] -->|Minor English loanwords| E["build_kotoba_database.py"]
    C --> E
    E --> F["Pre-compiled Database (assets/databases/kotoba.db)<br/><i>Packaged with English glosses</i>"]
    F --> G["build_kotobaan.sh<br/><i>Copies assets into APK</i>"]
    G --> H["KOTOBAAN.apk<br/><i>User sees English on flashcards & library</i>"]
    F --> I["KotobaDatabase.java<br/><i>Deploys to /data/data/com.example.kotoba/</i>"]
```

### Confirmed Findings:
1. **`kanji_additional_dataset.json` (2,119 entries):**
   - **Completely English:** 1,577 entries (e.g. `確` &rarr; `"Assurance, Firm, Tight"`, `認` &rarr; `"Acknowledge, Witness, Discern"`, `簡` &rarr; `"Simplicity, Brevity"`).
   - **Partially English / Mixed:** 469 entries (e.g. `第` &rarr; `"No., Residence"`, `容` &rarr; `"Contain, Bentuk, Looks"`, `身` &rarr; `"Tubuh, Person, One's station in life"`).
   - **Valid Indonesian:** Only 73 entries.
   - **Root Cause:** `process_additional_kanji.py` line 118: `if not matched: translated.append(m)` took raw English meanings from `kanji_dict_ref.json`.
2. **`kotoba_dataset.json` (1,114 total / 863 curriculum entries):**
   - 4 entries with untranslated or mixed English: `テープレコーダー` ("tape recorder"), `エアコン` ("AC(Air Conditioner)"), `すいはんき` ("rice cooker/penanak nasi"), `アルバイト` ("KERJA SAMBILAN / PART TIME").
3. **`kanji_dataset.json` (613 canonical entries):**
   - 2 entries with English abbreviations: `午前` ("AM"), `午後` ("PM").
4. **`kata_sifat_dataset.json` (173 entries):**
   - 5 entries with English: `変 (な)` ("Error, Aneh"), `新鮮な` ("Segar / fresh"), `パワフルな` ("Power full"), `オレンジ` ("Orange"), `シルバ` ("Silver").
5. **`KotobaDatabase.java` Cache Invalidation Risk:**
   - In `KotobaDatabase.java`, `ensureDatabaseCopied()` only checked `if (addCount < 2119)`. If an APK is updated over an existing installation where `addCount == 2119`, Android will reuse the old cached database unless we bump `DATABASE_VERSION = 3` and add a data integrity check that forces redeployment.

---

## File Structure

| Action | Path | Responsibility |
|---|---|---|
| **NEW** | `content/kanji_indonesian_dictionary.json` | Comprehensive curated mapping of Japanese Kanji semantic meanings to natural Indonesian. |
| **NEW** | `content/repair_kanji_translations.py` | Pipeline tool to repair all English and mixed entries across all JSON datasets. |
| **NEW** | `content/audit_translation_dataset.py` | Automated multi-signal auditor that scans SQLite databases & JSONs and asserts 0 English tokens. |
| **MODIFY** | `content/process_additional_kanji.py` | Fix root cause in additional kanji generator to permanently prevent English fallback. |
| **MODIFY** | `app/src/main/assets/kanji_additional_dataset.json` | Repaired additional kanji dataset with 100% natural Indonesian meanings. |
| **MODIFY** | `app/src/main/assets/kotoba_dataset.json` | Repaired curriculum vocabulary entries. |
| **MODIFY** | `app/src/main/assets/kanji_dataset.json` | Repaired canonical kanji entries (`午前`, `午後`). |
| **MODIFY** | `app/src/main/assets/kata_sifat_dataset.json` | Repaired adjective entries. |
| **MODIFY** | `content/build_kotoba_database.py` | Updated database builder enforcing translation invariants. |
| **MODIFY** | `app/src/main/assets/databases/kotoba.db` | Re-compiled SQLite database with clean Indonesian meanings. |
| **MODIFY** | `ios/Kotoba/Resources/kotoba.db` | Re-compiled SQLite database for iOS platform. |
| **MODIFY** | `app/src/main/java/com/kotoba/app/data/KotobaDatabase.java` | Bump `DATABASE_VERSION = 3` and ensure automated migration/re-deployment. |

---

## Tasks

### Task 1: Create the Translation & Semantic Engine (`content/repair_kanji_translations.py`)

**Files:**
- Create: `content/kanji_indonesian_dictionary.json`
- Create: `content/repair_kanji_translations.py`
- Consumes: `content/kanji_dict_ref.json`, `app/src/main/assets/kanji_additional_dataset.json`, `app/src/main/assets/kotoba_dataset.json`, `app/src/main/assets/kanji_dataset.json`, `app/src/main/assets/kata_sifat_dataset.json`
- Produces: Repaired JSON files in `app/src/main/assets/`

- [ ] **Step 1: Write `content/repair_kanji_translations.py`**
  - Ingests all 2,119 entries from `kanji_additional_dataset.json`.
  - For each entry, uses character context, readings, and semantic meanings from `kanji_dict_ref.json` to generate clear, concise Indonesian translations.
  - Implements translation rules:
    - `第` &rarr; `"Ke- (nomor urutan), tingkat"`
    - `詞` &rarr; `"Jenis kata, kata, syair"`
    - `確` &rarr; `"Pasti, yakin, memastikan"`
    - `認` &rarr; `"Mengakui, mengenali, menyetujui"`
    - `容` &rarr; `"Bentuk, rupa, isi, muatan"`
    - `示` &rarr; `"Menunjukkan, memperlihatkan"`
    - `単` &rarr; `"Sederhana, tunggal, satu"`
    - `簡` &rarr; `"Sederhana, ringkas, mudah"`
    - `身` &rarr; `"Tubuh, badan, diri sendiri"`
    - `降` &rarr; `"Turun, jatuh (hujan/salju), turun dari kendaraan"`
    - `次` &rarr; `"Berikutnya, selanjutnya, urutan"`
    - `感` &rarr; `"Merasa, perasaan, sentuhan emosi"`
    - `面` &rarr; `"Wajah, muka, permukaan, bidang"`
    - `備` &rarr; `"Mempersiapkan, melengkapi, menyediakan"`
    - `遅` &rarr; `"Lambat, terlambat, pelan"`
    - `情` &rarr; `"Perasaan, emosi, keadaan"`
    - `域` &rarr; `"Wilayah, daerah, kawasan"`
    - `代` &rarr; `"Generasi, era, mengganti, biaya"`
    - `訓` &rarr; `"Pelajaran, bimbingan, cara baca kun"`
    - `美` &rarr; `"Indah, cantik, keindahan"`
    - `在` &rarr; `"Ada, berada, bertempat tinggal"`
    - `準` &rarr; `"Standar, semi-, mendekati, patokan"`
  - Repairs specific entries in:
    - `kotoba_dataset.json`:
      - 48: `"Alat perekam pita kaset (tape recorder)"`
      - 470: `"AC / pendingin ruangan"`
      - 776: `"Penanak nasi (rice cooker)"`
      - 894: `"Kerja sambilan / paruh waktu"`
    - `kanji_dataset.json`:
      - 175: `"Pagi (sebelum jam 12 siang / a.m.)"`
      - 176: `"Sore / Malam (setelah jam 12 siang / p.m.)"`
    - `kata_sifat_dataset.json`:
      - 27: `"Aneh, tidak biasa"`
      - 56: `"Segar (segar bugar / makanan segar)"`
      - 60: `"Bertenaga, kuat, tangguh"`
      - 159: `"Jingga / Oranye"`
      - 160: `"Perak"`
  - Preserves all other fields (`id`, `kanji`, `reading`, `romaji`, `group_label`, `sort_order`, `details`).

- [ ] **Step 2: Run translation repair script**
  Run: `python3 content/repair_kanji_translations.py`
  Expected: All JSON assets updated cleanly with zero loss of records.

---

### Task 2: Build Automated Translation & English Detection Auditor (`content/audit_translation_dataset.py`)

**Files:**
- Create: `content/audit_translation_dataset.py`
- Test: `python3 content/audit_translation_dataset.py --target json`
- Test: `python3 content/audit_translation_dataset.py --target db`

- [ ] **Step 1: Write `content/audit_translation_dataset.py`**
  - Features multi-signal English detection:
    - Lexical: Checks against comprehensive English dictionary words (prepositions, conjunctions, pronouns, verbs, adjectives).
    - Morphological: Detects English suffixes (`-tion`, `-ment`, `-ness`, `-ity`, `-ance`, `-ence`, `-able`, `-ible`, `-ing`).
    - Pattern matching: Detects dictionary phrases (`"to do"`, `"one's"`, `"etc."`, `"someone"`).
    - Whitelist: Safely permits Indonesian loanwords that overlap with English (e.g. `bank`, `bus`, `radio`, `hotel`, `supermarket`, `video`, `air` [Indonesian for water], `ke- -an`).
  - Audits both SQLite databases (`kotoba.db`) and JSON assets.
  - Outputs structured audit metrics:
    - Total entries scanned
    - Valid Indonesian translations
    - Suspicious translations
    - Confirmed English translations
    - Repaired entries count
    - Unresolved entries

- [ ] **Step 2: Run audit on repaired JSON assets**
  Run: `python3 content/audit_translation_dataset.py --target json`
  Expected: Confirmed English = 0, Suspicious = 0, Unresolved = 0.

---

### Task 3: Fix Generator Script Root Cause (`content/process_additional_kanji.py`)

**Files:**
- Modify: `content/process_additional_kanji.py`
- Test: `python3 content/process_additional_kanji.py`

- [ ] **Step 1: Update `process_additional_kanji.py`**
  - Replace naive 50-word `EN_TO_ID` and raw English fallback (`translated.append(m)`) with the comprehensive Indonesian dictionary / translation pipeline.
  - Assert that any newly generated entry passes the English detection filter.
  - Re-run script to confirm it produces 100% Indonesian glosses without introducing regressions.

- [ ] **Step 2: Verify `kanji_additional_dataset.json` remains identical and clean**
  Run: `python3 content/audit_translation_dataset.py --file app/src/main/assets/kanji_additional_dataset.json`
  Expected: 2,119 entries scanned, 0 English.

---

### Task 4: Recompile SQLite Database & Parity Sync (`build_kotoba_database.py`)

**Files:**
- Modify: `content/build_kotoba_database.py`
- Modify: `app/src/main/assets/databases/kotoba.db`
- Modify: `ios/Kotoba/Resources/kotoba.db`
- Test: `tests/test_ios_db_contract.py`

- [ ] **Step 1: Run `build_kotoba_database.py`**
  Run: `python3 content/build_kotoba_database.py`
  Expected: Generates fresh `app/src/main/assets/databases/kotoba.db` with exactly 4,363 learning objects.

- [ ] **Step 2: Sync to iOS resources**
  Run: `cp app/src/main/assets/databases/kotoba.db ios/Kotoba/Resources/kotoba.db`

- [ ] **Step 3: Run iOS database contract test**
  Run: `python3 -m unittest tests/test_ios_db_contract.py`
  Expected: All 8 contract tests PASS.

- [ ] **Step 4: Run automated audit on compiled SQLite database**
  Run: `python3 content/audit_translation_dataset.py --target db --path app/src/main/assets/databases/kotoba.db`
  Expected: 4,363 rows scanned, 0 English translations.

---

### Task 5: Update `KotobaDatabase.java` Cache Invalidation

**Files:**
- Modify: `app/src/main/java/com/kotoba/app/data/KotobaDatabase.java`
- Test: `tests/run_java_tests.sh`

- [ ] **Step 1: Bump `DATABASE_VERSION = 3` and update `ensureDatabaseCopied()`**
  - Increase version from 2 to 3.
  - Add validation in `ensureDatabaseCopied()`:
    ```java
    // Check if deployed database contains stale English glosses
    Cursor checkCursor = checkDb.rawQuery(
        "SELECT count(*) FROM learning_objects WHERE indonesian LIKE '%residence%' OR indonesian LIKE '%assurance%'",
        null
    );
    if (checkCursor != null && checkCursor.moveToFirst() && checkCursor.getInt(0) > 0) {
        needsDeploy = true;
        Log.i(TAG, "Existing database contains stale English translations, redeploying clean Indonesian database...");
    }
    ```
  - Ensures existing users updating their app get the clean Indonesian database deployed immediately.

- [ ] **Step 2: Run all Java unit test suites**
  Run: `bash tests/run_java_tests.sh`
  Expected: All 11 test suites compile and PASS with 0 errors.

---

### Task 6: Build `KOTOBAAN.apk` & Packaged APK Verification

**Files:**
- Modify: `/storage/emulated/0/Download/PROJECT/KTB/KOTOBAAN.apk`
- Test: `tools/build_kotobaan.sh`

- [ ] **Step 1: Run APK build script**
  Run: `bash tools/build_kotobaan.sh`
  Expected: Builds, aligns, signs (v1, v2, v3), and outputs `KOTOBAAN.apk`.

- [ ] **Step 2: Extract packaged database from `KOTOBAAN.apk` and run automated audit**
  Run audit script directly on the database inside the APK:
  `python3 content/audit_translation_dataset.py --target apk --path /storage/emulated/0/Download/PROJECT/KTB/KOTOBAAN.apk`
  Expected:
  - Total entries: 4,363
  - Confirmed English: 0
  - Suspicious translations: 0
  - Translations repaired: 2,057
  - Unresolved: 0
  - Final APK verification: PASS

- [ ] **Step 3: Verification of Representative Views & Integrity Invariants**
  - Verify representative entries in:
    - Flashcard front & back: Kanji front &rarr; reading + Indonesian meaning back.
    - Hiragana front &rarr; Kanji + Indonesian meaning back.
    - Indonesian meaning front &rarr; Kanji + reading back.
    - Unified Library / Pustaka search filter: searching Indonesian queries (e.g. "pasti", "sederhana", "turun") returns the correct cards.

---

## Verification Plan

### Automated Tests
1. **Translation Audit Script:**
   `python3 content/audit_translation_dataset.py --target apk --path /storage/emulated/0/Download/PROJECT/KTB/KOTOBAAN.apk`
2. **Java Unit Tests (11 test suites):**
   `bash tests/run_java_tests.sh`
3. **iOS Database Contract Tests:**
   `python3 -m unittest tests/test_ios_db_contract.py`
4. **Apksigner Verification:**
   `java -jar /root/.android_tools/apksigner.jar verify --verbose --min-sdk-version 21 /storage/emulated/0/Download/PROJECT/KTB/KOTOBAAN.apk`

### Measurable Acceptance Metrics to Report:
- Total entries scanned: **4,363**
- Valid Indonesian translations: **4,363**
- Confirmed English translations: **0**
- Suspicious translations: **0**
- Translations repaired: **~2,057**
- Unresolved entries: **0**
- Final APK verification: **PASS**
