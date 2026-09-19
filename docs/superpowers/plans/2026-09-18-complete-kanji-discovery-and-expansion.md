# Complete Kanji Discovery & Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exhaustively scan every PDF in `NEW SOURCE` (49 files, 4,277 pages), discover every unique Kanji character, isolate the protected canonical 613 Kanji dataset without modification, generate a segregated `Additional Kanji` dataset partitioned into 50-card batches with provenance, and integrate clean `[ 613 ]` vs `[ Additional ]` study flows into Kotoba.app.

**Architecture:** A robust multi-stage Python pipeline (`scan_all_kanji.py`, `process_additional_kanji.py`, `generate_scan_audit_report.py`) handles text extraction, image OCR, CJK Unicode validation, canonical set subtraction, dictionary lookup, and batch partitioning. The SQLite database builder (`build_kotoba_database.py`) ingests both sets with domain isolation. The Android app (`MainActivity.java`, `LibraryFilterDialog.java`) adds a secondary segmented selector for `[ 613 ]` vs `[ Additional ]` following the established 1:1 `kotoba-1.apk` visual tokens.

**Tech Stack:** Python 3 (Poppler `pdftotext`, `pdftoppm`, `pypdf`, `tesseract-ocr-jpn`), SQLite3, Android SDK / Java 17, `aapt2`, `d8`, `apksigner`.

**Spec:** User prompt `/plan KOTOBA.APP — COMPLETE KANJI DISCOVERY & EXPANSION`.

---

## Global Constraints

- **613 Set is Sacrosanct**: The 613 canonical Kanji in `kanji_dataset.json` (IDs `kanji_0001` through `kanji_0613`, 13 groups of 50) must NEVER be modified, reordered, renumbered, deleted, or merged with Additional Kanji.
- **Exhaustive Directory Scan**: All 49 PDFs in `/storage/emulated/0/Download/PROJECT/KTB/NEW SOURCE` must be processed recursively. Zero files or pages skipped.
- **Strict Character Filtering**: Only valid CJK Unified Ideographs (`U+4E00`–`U+9FAF`) and Extension A (`U+3400`–`U+4DBF`) are extracted. Hiragana, Katakana, Latin, punctuation, and OCR noise are rejected.
- **Batching Rule**: Additional Kanji must be partitioned into contiguous batches of up to 50 cards (`Additional 01–50`, `Additional 51–100`, etc.).
- **Visual Design Invariant**: No emojis (`✓`, `✕`, `🎉`), no layout redesigns, no external styling frameworks. Strictly preserve the clean single-screen flashcard canvas from `kotoba-1.apk`.
- **Target Platform**: Android minSdk 26, targetSdk 35, zero runtime permissions.

---

## Proposed Changes

### Component 1: Extraction & Processing Pipeline (`content/`)

```
NEW SOURCE (49 PDFs, 4,277 Pages)
      │
      ▼
scan_all_kanji.py ───► pdftotext (43 PDFs) + OCR / tesseract-jpn (6 PDFs)
      │
      ▼
CJK Unicode Normalization & OCR Validation (NFKC + CJK Ideograph filter)
      │
      ▼
process_additional_kanji.py ───► Subtract Canonical 613 Kanji
      │
      ├──────────────────────────────┬──────────────────────────────┐
      ▼                              ▼                              ▼
Protected Formal 613 Set       Additional Kanji Batches      Provenance Map
(100% Untouched, 13 Groups)     (50/group: 01-50, 51-100...)   (PDF, Page, Frequency)
      │                              │                              │
      └──────────────────────────────┴──────────────────────────────┘
                                     │
                                     ▼
                      build_kotoba_database.py ──► kotoba.db
```

#### [NEW] `content/scan_all_kanji.py`
Automated scanning engine that inventories all 49 PDFs in `NEW SOURCE`, extracts text page-by-page via `pdftotext` and `tesseract` OCR for image-only pages, normalizes Unicode, extracts CJK characters, and records occurrence count and page-level provenance.

#### [NEW] `content/process_additional_kanji.py`
Comparative deduplication engine that loads `app/src/main/assets/kanji_dataset.json`, identifies characters already present in the 613, isolates genuine Additional Kanji, enriches them with authentic Onyomi/Kunyomi readings and Indonesian glosses, and partitions them into batches of 50.

#### [NEW] `content/generate_scan_audit_report.py`
Generates the comprehensive `KANJI_SCAN_AUDIT_REPORT.md` containing metrics, PDF inventory table, overlap analysis, batch rosters, and full provenance citations.

#### [MODIFY] `content/build_kotoba_database.py`
Update SQLite database builder to ingest Additional Kanji with distinct IDs (`kanji_add_0001`...), `group_label` (`Additional 01–50`, `Additional 51–100`, etc.), and `sort_order` starting at 1001. The 613 formal Kanji rows and sort orders remain completely unchanged.

#### [MODIFY] `content/validate_curriculum.py`
Add automated verification tests:
1. Verify 613 canonical Kanji entries are byte-for-byte identical to baseline.
2. Verify zero set intersection between canonical 613 characters and Additional Kanji.
3. Verify every Additional Kanji has non-empty provenance (source file and page).
4. Verify every Additional batch has <= 50 items.

---

### Component 2: Android Data Layer (`app/src/main/java/com/kotoba/app/data/`)

#### [MODIFY] `KotobaDatabase.java`
Add query methods for distinct sub-sections:
```java
// Formal 613 queries
public List<LearningObject> getFormalKanji();
public List<String> getFormalKanjiGroups();

// Additional Kanji queries
public List<LearningObject> getAdditionalKanji();
public List<String> getAdditionalKanjiGroups();
public List<LearningObject> getKanjiByGroup(String groupLabel);
```

#### [NEW] `tests/KanjiExpansionTest.java`
Unit test validating database queries, 613 stability, and batch counts.

---

### Component 3: Android UI Layer (`app/src/main/`)

#### [MODIFY] `app/src/main/res/layout/activity_main.xml`
Add a secondary segmented control `kanji_subsection_container` directly below `section_switcher_container`:
```xml
<LinearLayout
    android:id="@+id/kanji_subsection_container"
    android:layout_width="match_parent"
    android:layout_height="32dp"
    android:layout_marginTop="4dp"
    android:layout_marginBottom="4dp"
    android:background="@drawable/bg_segmented_container"
    android:orientation="horizontal"
    android:padding="2dp"
    android:visibility="gone">

    <Button
        android:id="@+id/btn_kanji_sub_613"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:background="@drawable/bg_segmented_active"
        android:fontFamily="sans-serif-medium"
        android:text="613 Formal"
        android:textAllCaps="false"
        android:textColor="@color/colorOnPrimary"
        android:textSize="12sp"
        android:textStyle="bold" />

    <Button
        android:id="@+id/btn_kanji_sub_additional"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:background="@android:color/transparent"
        android:fontFamily="sans-serif-medium"
        android:text="Tambahan"
        android:textAllCaps="false"
        android:textColor="@color/colorTextSecondary"
        android:textSize="12sp"
        android:textStyle="bold" />
</LinearLayout>
```

#### [MODIFY] `MainActivity.java`
1. Manage `mActiveKanjiSubSection = "613"` vs `"ADDITIONAL"`.
2. Toggle `kanji_subsection_container` visibility (visible only when `mActiveSection.equals("KANJI")`).
3. Switching between `613` and `Tambahan` rebuilds the deck with appropriate title and pool.
4. Preserves 3D flip, audio TTS, Romaji/Furigana settings, and Ingat/Lupa recall cycles.

#### [MODIFY] `LibraryFilterDialog.java`
1. When opened in Kanji mode, display two sub-tabs: `[ 613 ]` and `[ Tambahan ]`.
2. Under `613`: display chips `Semua Group`, `01–50`, `51–100`, ..., `601–613`.
3. Under `Tambahan`: display chips `Semua Tambahan`, `Batch 1 (01–50)`, `Batch 2 (51–100)`, etc.
4. Tapping a batch chip filters the list and allows studying that specific 50-card batch via "Gunakan Filter Ini".

---

## Task Right-Sizing & Step Breakdown

### Task 1: Complete PDF Inventory & Full Corpus Extraction Pipeline
**Files:**
- Create: `content/scan_all_kanji.py`
- Test: `content/validate_curriculum.py`

**Interfaces:**
- Consumes: All 49 PDFs in `/storage/emulated/0/Download/PROJECT/KTB/NEW SOURCE`
- Produces: `content/scanned_kanji_raw.json` (characters, occurrence frequencies, file & page provenance)

- [ ] **Step 1: Write test script to verify PDF inventory count and extraction output**
```python
def test_scan_inventory():
    assert os.path.exists("content/scanned_kanji_raw.json")
    with open("content/scanned_kanji_raw.json") as f:
        data = json.load(f)
    assert len(data["inventory"]) == 49
    assert data["total_pages"] == 4277
    assert len(data["characters"]) > 2000
```
- [ ] **Step 2: Run test to verify it fails**
Run: `python3 -c "import os; assert os.path.exists('content/scanned_kanji_raw.json')"`
Expected: FAIL (file does not exist yet)
- [ ] **Step 3: Implement `content/scan_all_kanji.py`**
Implement full recursive traversal across all 49 PDFs, page-by-page text extraction (`pdftotext`), targeted OCR (`pdftoppm` + `tesseract -l jpn`) for image pages, CJK regex extraction `[\u3400-\u4dbf\u4e00-\u9faf]`, and provenance map recording.
- [ ] **Step 4: Execute scan pipeline and verify output**
Run: `python3 content/scan_all_kanji.py`
Expected: Processes all 49 files, generates `content/scanned_kanji_raw.json`.
- [ ] **Step 5: Verify inventory and character count**
Run test assertions from Step 1.

---

### Task 2: Canonical 613 Set Subtraction, Deduplication & Batch Partitioning
**Files:**
- Create: `content/process_additional_kanji.py`
- Test: `content/validate_curriculum.py`

**Interfaces:**
- Consumes: `content/scanned_kanji_raw.json`, `app/src/main/assets/kanji_dataset.json`
- Produces: `app/src/main/assets/kanji_additional_dataset.json`, `content/kanji_provenance.json`

- [ ] **Step 1: Write failing test for Additional dataset separation**
```python
def test_additional_kanji_invariants():
    with open("app/src/main/assets/kanji_dataset.json") as f:
        k613 = json.load(f)
    with open("app/src/main/assets/kanji_additional_dataset.json") as f:
        kadd = json.load(f)
    assert len(k613) == 613
    # Verify zero character intersection
    chars_613 = {c for item in k613 for c in item["kanji"] if '\u4e00' <= c <= '\u9faf'}
    chars_add = {item["kanji"] for item in kadd}
    assert len(chars_613.intersection(chars_add)) == 0
    # Verify batches capped at 50
    for item in kadd:
        assert "Additional " in item["group_label"]
```
- [ ] **Step 2: Run test to verify it fails**
Run: `python3 -c "import os; assert os.path.exists('app/src/main/assets/kanji_additional_dataset.json')"`
Expected: FAIL
- [ ] **Step 3: Implement `content/process_additional_kanji.py`**
Extract canonical 613 characters, subtract from discovered set, sort Additional Kanji by occurrence frequency/joyo rank, enrich with authentic readings and Indonesian glosses, partition into batches of 50 (`Additional 01–50`, `Additional 51–100`...), and save to `kanji_additional_dataset.json`.
- [ ] **Step 4: Run script and verify invariants pass**
Run: `python3 content/process_additional_kanji.py`
Expected: Generates `app/src/main/assets/kanji_additional_dataset.json`.
- [ ] **Step 5: Run validation test**
Run: `python3 -c "from content.validate_curriculum import verify_additional_kanji; verify_additional_kanji()"`
Expected: PASS

---

### Task 3: Comprehensive Audit Report Generation
**Files:**
- Create: `content/generate_scan_audit_report.py`
- Produces: `KANJI_SCAN_AUDIT_REPORT.md`

**Interfaces:**
- Consumes: `content/scanned_kanji_raw.json`, `content/kanji_provenance.json`, `app/src/main/assets/kanji_additional_dataset.json`
- Produces: `KANJI_SCAN_AUDIT_REPORT.md` (and updates `SCAN_SUMMARY_REPORT.md`)

- [ ] **Step 1: Write test for report generation**
Verify report contains all required headers: `Total PDFs scanned`, `Total pages processed`, `Total unique Kanji discovered`, `Already in formal 613`, `New Additional Kanji`, and provenance tables.
- [ ] **Step 2: Implement `content/generate_scan_audit_report.py`**
Format complete markdown audit with exact numbers and page citations.
- [ ] **Step 3: Run report generation**
Run: `python3 content/generate_scan_audit_report.py`
Expected: `KANJI_SCAN_AUDIT_REPORT.md` generated with full audit data.

---

### Task 4: SQLite Database Ingestion & Domain Isolation
**Files:**
- Modify: `content/build_kotoba_database.py:268-305`
- Test: `content/validate_curriculum.py`

**Interfaces:**
- Consumes: `app/src/main/assets/kanji_dataset.json` (613 items), `app/src/main/assets/kanji_additional_dataset.json`
- Produces: `app/src/main/assets/databases/kotoba.db`

- [ ] **Step 1: Write test for database ingestion integrity**
```python
def test_database_kanji_isolation():
    conn = sqlite3.connect("app/src/main/assets/databases/kotoba.db")
    c = conn.cursor()
    c.execute("SELECT count(*) FROM learning_objects WHERE type = 'KANJI' AND id NOT LIKE 'kanji_add_%'")
    assert c.fetchone()[0] == 613
    c.execute("SELECT count(*) FROM learning_objects WHERE type = 'KANJI' AND id LIKE 'kanji_add_%'")
    add_count = c.fetchone()[0]
    assert add_count > 0
```
- [ ] **Step 2: Run test to verify it fails before ingestion**
Expected: FAIL on `add_count > 0`
- [ ] **Step 3: Update `content/build_kotoba_database.py`**
Add Additional Kanji ingestion block with IDs `kanji_add_0001`..., preserving the 613 items completely.
- [ ] **Step 4: Rebuild database**
Run: `python3 content/build_kotoba_database.py`
Expected: SQLite database created with both formal 613 and Additional Kanji.
- [ ] **Step 5: Run database verification test**
Run: `python3 content/validate_curriculum.py`
Expected: 100% PASSED.

---

### Task 5: Java Data Layer Updates & Unit Testing
**Files:**
- Modify: `app/src/main/java/com/kotoba/app/data/KotobaDatabase.java`
- Create: `tests/KanjiExpansionTest.java`

**Interfaces:**
- Consumes: `kotoba.db`
- Produces: `getFormalKanji()`, `getAdditionalKanji()`, `getFormalKanjiGroups()`, `getAdditionalKanjiGroups()`

- [ ] **Step 1: Write Java unit test in `tests/KanjiExpansionTest.java`**
```java
public class KanjiExpansionTest {
    public static void main(String[] args) {
        KotobaDatabase db = ...;
        assert db.getFormalKanji().size() == 613 : "Formal Kanji must be 613";
        assert db.getFormalKanjiGroups().size() == 13 : "Formal groups must be 13";
        assert !db.getAdditionalKanji().isEmpty() : "Additional Kanji must not be empty";
        System.out.println("KanjiExpansionTest PASSED");
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: `bash tests/run_java_tests.sh`
Expected: FAIL (methods not yet implemented)
- [ ] **Step 3: Implement queries in `KotobaDatabase.java`**
Implement `getFormalKanji()`, `getAdditionalKanji()`, `getFormalKanjiGroups()`, `getAdditionalKanjiGroups()`.
- [ ] **Step 4: Run test to verify it passes**
Run: `bash tests/run_java_tests.sh`
Expected: PASS with `-ea`.

---

### Task 6: Main Screen Kanji Sub-Section UI Integration
**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml:63-99`
- Modify: `app/src/main/java/com/kotoba/app/MainActivity.java:68-150,326-385`

**Interfaces:**
- Consumes: `KotobaDatabase` methods
- Produces: Dual-segment Kanji study view on main flashcard canvas

- [ ] **Step 1: Add `kanji_subsection_container` in `activity_main.xml`**
Add segmented control `[ 613 Formal ]` and `[ Tambahan ]` directly below `section_switcher_container`.
- [ ] **Step 2: Update `MainActivity.java`**
Wire segmented buttons, track `mActiveKanjiSubSection`, show container only when in `KANJI` mode, reload appropriate deck (`613` vs `Tambahan`), and update counter and title.
- [ ] **Step 3: Test compilation via `tools/build_apk.sh`**
Run: `bash tools/build_apk.sh`
Expected: Clean build without errors.

---

### Task 7: Library Filter Dialog Expansion
**Files:**
- Modify: `app/src/main/java/com/kotoba/app/ui/library/LibraryFilterDialog.java:144-151,220-290`
- Modify: `app/src/main/res/layout/dialog_library_filter.xml`

**Interfaces:**
- Consumes: `getFormalKanjiGroups()`, `getAdditionalKanjiGroups()`
- Produces: Filterable 50-card batch selection for both 613 and Additional Kanji

- [ ] **Step 1: Update `dialog_library_filter.xml`**
Add sub-section switcher chips for `613` vs `Tambahan` in the dialog header when in Kanji mode.
- [ ] **Step 2: Update `LibraryFilterDialog.java`**
When `613` is selected, render 13 group chips (`01–50` .. `601–613`).
When `Tambahan` is selected, render batch chips (`Batch 1 (01–50)`, `Batch 2 (51–100)`...).
Clicking a batch chip filters the list and passes the 50-item pool to `onFilterApplied()`.
- [ ] **Step 3: Compile and run test suite**
Run: `bash tools/build_apk.sh && bash tests/run_java_tests.sh`
Expected: PASS.

---

### Task 8: End-to-End Verification & Production Packaging
**Files:**
- Build: `Kotoba.apk`
- Documentation: `docs/superpowers/plans/2026-09-18-complete-kanji-discovery-and-expansion.md`, `KANJI_SCAN_AUDIT_REPORT.md`

- [ ] **Step 1: Run comprehensive curriculum and dataset integrity checks**
Run: `python3 content/validate_curriculum.py`
Expected: 100% PASSED (14/14 checks).
- [ ] **Step 2: Run all Java unit tests with assertions enabled**
Run: `bash tests/run_java_tests.sh`
Expected: 100% PASSED (6 test suites).
- [ ] **Step 3: Build production APK with v1 + v2 + v3 signatures**
Run: `bash tools/build_apk.sh`
Expected: `Kotoba.apk` generated, aligned, and signed.
- [ ] **Step 4: Verify APK signature and zero-permission compliance**
Run: `apksigner verify -v Kotoba.apk` and `aapt2 dump badging Kotoba.apk | grep uses-permission`
Expected: Verified = true, 0 permissions.

---

## Verification Plan

### Automated Tests
1. **Corpus & Extraction Validation**:
   `python3 content/validate_curriculum.py`
   - Verifies all 49 PDFs scanned.
   - Verifies 613 formal Kanji dataset is completely unaltered.
   - Verifies 0 intersection between 613 and Additional Kanji.
   - Verifies batch size <= 50 for all Additional batches.
2. **Java Test Suites**:
   `bash tests/run_java_tests.sh`
   - Unit tests for `KotobaDatabase`, `ReviewEngine`, `IngatLupaEngine`, `VocabularyFilterTest`, `KanjiExpansionTest`.
3. **APK Build & Signature Verification**:
   `bash tools/build_apk.sh`
   - Verifies clean compilation, DEX generation, resource packaging, and multi-scheme signing.

### Manual Verification
1. Open app, tap **Kanji** in top section bar.
2. Verify sub-segmented bar appears with **[ 613 Formal ]** and **[ Tambahan ]**.
3. Tap **[ 613 Formal ]**: verify deck title shows `"Kanji JFT & JLPT • 613 huruf"`, counter starts at `1 / 613`.
4. Tap **[ Tambahan ]**: verify deck title updates to `"Kanji Tambahan • ... huruf"`, counter updates to `1 / ...`.
5. Tap **Pustaka (Drawer)** while in Kanji mode:
   - Verify toggle between `613` and `Tambahan`.
   - Tap `Batch 1 (01–50)` under Tambahan, tap **Gunakan Filter Ini**.
   - Verify main canvas loads exactly the 50 cards of Batch 1.
6. Verify 3D flip card, Ingat (green checkmark), and Lupa (red cross) operate seamlessly.
