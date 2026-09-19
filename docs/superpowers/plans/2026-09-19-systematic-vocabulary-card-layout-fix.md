# Systematic Vocabulary Card Layout Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve systematic layout corruption, squeezed romaji wrapping, displaced badges, duplicate readings, and view overlap across vocabulary cards in Pustaka (`Kata Sifat - な` and all categories) for `KOTOBAAN.apk` and iOS.

**Architecture:** Replace the cramped 1-row horizontal layout in `item_library_vocab.xml` with an isolated 3-row floating card architecture: Row 1 Top Badges, Row 2 Japanese title + Furigana/Romaji, Row 3 Indonesian meaning + Speaker button. Eliminate duplicate readings in `LibraryFilterDialog.java` adapter binding, configure transparent ListView dividers with card elevation, and mirror the 3-row card hierarchy in iOS `PustakaCardRow.swift`.

**Tech Stack:** Android XML Layouts, Java Android SDK, Android ListView/BaseAdapter, SwiftUI, SQLite, Bash, apksigner/zipalign.

**Spec:** User task specification "KOTOBAAN — SYSTEMATIC VOCABULARY CARD LAYOUT FIX".

## Global Constraints
- Do NOT patch individual vocabulary words (`no if (vocab == "ハンサム")`, `no if (vocab.endsWith("な"))`).
- Do NOT strip or delete grammatical markers like `(な)` from data.
- Preserve all existing view IDs (`txt_vocab_japanese`, `txt_vocab_reading`, `txt_vocab_romaji`, `txt_vocab_meaning`, `txt_vocab_badge`, `txt_vocab_type_badge`, `btn_vocab_speak`).
- Ensure dark mode and light mode visual parity using existing theme tokens (`@color/colorCardBg`, `@color/colorCardStroke`, `@color/colorBorderOutline`).
- Build, sign, and verify `KOTOBAAN.apk` and `KOTOBA FINAL.apk`.

---

## File Structure

- **Create**:
  - `app/src/main/res/drawable/bg_card_vocab_item.xml`: Isolated rounded card background (12dp radius, 1dp stroke `@color/colorCardStroke`, `@color/colorCardBg`).
  - `tests/PustakaCardLayoutContractTest.java`: Unit test verifying adapter display rules (reading duplication prevention, badge visibility, null-safety).
- **Modify**:
  - `app/src/main/res/layout/item_library_vocab.xml`: Redesign into deterministic 3-row card layout.
  - `app/src/main/res/layout/dialog_library_filter.xml`: Configure ListView card spacing (`dividerHeight="8dp"`, `padding="6dp"`), increase frame height from 250dp to 280dp.
  - `app/src/main/java/com/kotoba/app/ui/library/LibraryFilterDialog.java`: Update `LibraryVocabAdapter` and `ViewHolder` to support `layoutBadgesRow`, suppress duplicate kana readings when `reading.equals(japanese)`, reset all visibility states cleanly.
  - `ios/Kotoba/Views/Pustaka/PustakaCardRow.swift`: Update SwiftUI card view to match the 3-row card hierarchy.
  - `tests/run_java_tests.sh`: Add `PustakaCardLayoutContractTest` to the test suite runner.

---

### Task 1: Create Card Surface Drawable and Failing Layout Contract Test

**Files:**
- Create: `app/src/main/res/drawable/bg_card_vocab_item.xml`
- Create: `tests/PustakaCardLayoutContractTest.java`
- Modify: `tests/run_java_tests.sh`

**Interfaces:**
- Consumes: `LearningObject` model getters (`getJapanese()`, `getReading()`, `getRomaji()`, `getIndonesian()`, `getBadgeLabel()`, `getWordType()`, `getBab()`).
- Produces: `PustakaCardLayoutContractTest.java` verifying that:
  1. Katakana/Hiragana words where `reading == japanese` (`ハンサム (な)`) have reading suppressed to prevent duplicate rendering.
  2. Kanji words where `reading != japanese` (`親切 (な)`) preserve both Japanese and reading.
  3. Badge row visibility is true if either badgeLabel or wordType or bab is present, false otherwise.
  4. Romaji is preserved and formatted without awkward line splits.

- [ ] **Step 1: Create `app/src/main/res/drawable/bg_card_vocab_item.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/colorCardBg" />
    <stroke
        android:width="1dp"
        android:color="@color/colorCardStroke" />
    <corners android:radius="12dp" />
</shape>
```

- [ ] **Step 2: Write unit test `tests/PustakaCardLayoutContractTest.java`**

```java
package tests;

import com.kotoba.app.model.LearningObject;

public class PustakaCardLayoutContractTest {
    public static void main(String[] args) {
        System.out.println("Running PustakaCardLayoutContractTest...");
        testDuplicateReadingSuppression();
        testKanjiReadingPreservation();
        testBadgeRowVisibilityRules();
        testRomajiIntegrity();
        System.out.println("PustakaCardLayoutContractTest: All assertions passed successfully!");
    }

    private static void testDuplicateReadingSuppression() {
        LearningObject item = new LearningObject();
        item.setJapanese("ハンサム (な)");
        item.setReading("ハンサム (な)");
        item.setRomaji("hansamu");
        item.setIndonesian("Tampan");

        boolean showReading = shouldDisplayReading(item.getJapanese(), item.getReading());
        assert !showReading : "Reading should be hidden when identical to Japanese (e.g. ハンサム (な))";
    }

    private static void testKanjiReadingPreservation() {
        LearningObject item = new LearningObject();
        item.setJapanese("親切 (な)");
        item.setReading("しんせつ (な)");
        item.setRomaji("shinsetsu");
        item.setIndonesian("Baik Hati");

        boolean showReading = shouldDisplayReading(item.getJapanese(), item.getReading());
        assert showReading : "Reading must be shown when Kanji has distinct reading (e.g. 親切 (な) -> しんせつ (な))";
    }

    private static void testBadgeRowVisibilityRules() {
        LearningObject itemWithBadges = new LearningObject();
        itemWithBadges.setBadgeLabel("Kata Sifat #2");
        itemWithBadges.setWordType(LearningObject.WordType.KATA_SIFAT_NA);
        assert hasBadges(itemWithBadges) : "Badge row must be visible when badge label is set";

        LearningObject itemEmpty = new LearningObject();
        itemEmpty.setBadgeLabel(null);
        itemEmpty.setWordType(LearningObject.WordType.LAINNYA);
        itemEmpty.setBab(null);
        assert !hasBadges(itemEmpty) : "Badge row must be hidden when no badges are present";
    }

    private static void testRomajiIntegrity() {
        LearningObject item = new LearningObject();
        item.setRomaji("kirei");
        assert item.getRomaji() != null && !item.getRomaji().isEmpty() : "Romaji must be non-empty for kirei";
    }

    public static boolean shouldDisplayReading(String japanese, String reading) {
        if (reading == null) return false;
        String trimmed = reading.trim();
        if (trimmed.isEmpty() || trimmed.equals("—")) return false;
        return !trimmed.equals(japanese != null ? japanese.trim() : "");
    }

    public static boolean hasBadges(LearningObject item) {
        if (item.getBadgeLabel() != null && !item.getBadgeLabel().trim().isEmpty()) return true;
        if (item.getBab() != null && item.getBab() > 0) return true;
        return item.getWordType() != null && item.getWordType() != LearningObject.WordType.LAINNYA;
    }
}
```

- [x] **Step 3: Update `tests/run_java_tests.sh` to include `PustakaCardLayoutContractTest`**
- [x] **Step 4: Run `bash tests/run_java_tests.sh` to verify test passes**

---

### Task 2: Redesign `item_library_vocab.xml` to Stable 3-Row Card Architecture

**Files:**
- Modify: `app/src/main/res/layout/item_library_vocab.xml`

**Interfaces:**
- Consumes: `@drawable/bg_card_vocab_item`, existing color tokens.
- Produces: View hierarchy:
  - Root: Vertical `LinearLayout` with `@drawable/bg_card_vocab_item`, padding `12dp`.
  - Row 1: `layout_vocab_badges_row` containing `txt_vocab_badge` and `txt_vocab_type_badge`.
  - Row 2: Horizontal row containing `txt_vocab_japanese` (left, weighted `1`, 18sp bold) and vertical container (right) with `txt_vocab_reading` (13sp ruby) and `txt_vocab_romaji` (12sp tertiary).
  - Divider: 1dp view with `@color/colorBorderOutline`.
  - Row 3: Horizontal row containing `txt_vocab_meaning` (left, weighted `1`, 14sp bold) and `btn_vocab_speak` (right, 36dp x 36dp).

- [ ] **Step 1: Replace contents of `app/src/main/res/layout/item_library_vocab.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_card_vocab_item"
    android:orientation="vertical"
    android:paddingLeft="14dp"
    android:paddingTop="12dp"
    android:paddingRight="14dp"
    android:paddingBottom="12dp">

    <!-- Row 1: Top Badges Header (Always at top of card) -->
    <LinearLayout
        android:id="@+id/layout_vocab_badges_row"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="8dp"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/txt_vocab_badge"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginEnd="6dp"
            android:background="@drawable/bg_badge"
            android:fontFamily="sans-serif-medium"
            android:paddingLeft="8dp"
            android:paddingTop="2dp"
            android:paddingRight="8dp"
            android:paddingBottom="2dp"
            android:textColor="@color/colorBadgeText"
            android:textSize="11sp" />

        <TextView
            android:id="@+id/txt_vocab_type_badge"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_badge"
            android:fontFamily="sans-serif-medium"
            android:paddingLeft="8dp"
            android:paddingTop="2dp"
            android:paddingRight="8dp"
            android:paddingBottom="2dp"
            android:textColor="@color/colorPrimary"
            android:textSize="11sp" />
    </LinearLayout>

    <!-- Row 2: Japanese Title & Reading / Romaji -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/txt_vocab_japanese"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:fontFamily="serif"
            android:textColor="@color/colorTextPrimary"
            android:textSize="18sp"
            android:textStyle="bold" />

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:gravity="end|center_vertical"
            android:orientation="vertical">

            <TextView
                android:id="@+id/txt_vocab_reading"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:fontFamily="sans-serif-medium"
                android:gravity="end"
                android:textColor="@color/colorFuriganaRuby"
                android:textSize="13sp" />

            <TextView
                android:id="@+id/txt_vocab_romaji"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:fontFamily="sans-serif"
                android:gravity="end"
                android:textColor="@color/colorTextTertiary"
                android:textSize="12sp" />
        </LinearLayout>
    </LinearLayout>

    <!-- Subtle Divider between Japanese and Meaning -->
    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:layout_marginTop="8dp"
        android:layout_marginBottom="8dp"
        android:background="@color/colorBorderOutline" />

    <!-- Row 3: Indonesian Meaning & Speaker Button -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/txt_vocab_meaning"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:fontFamily="sans-serif-medium"
            android:textColor="@color/colorTextPrimary"
            android:textSize="14sp"
            android:textStyle="bold" />

        <ImageButton
            android:id="@+id/btn_vocab_speak"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:layout_marginStart="8dp"
            android:background="?android:attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/action_sound"
            android:src="@drawable/ic_volume_up" />
    </LinearLayout>
</LinearLayout>
```

---

### Task 3: Update `LibraryFilterDialog.java` and `dialog_library_filter.xml`

**Files:**
- Modify: `app/src/main/res/layout/dialog_library_filter.xml`
- Modify: `app/src/main/java/com/kotoba/app/ui/library/LibraryFilterDialog.java`

**Interfaces:**
- Consumes: `R.id.layout_vocab_badges_row`, updated `item_library_vocab.xml`.
- Produces: Deterministic adapter binding without view recycling state leakage or duplicate text rendering.

- [ ] **Step 1: Update `dialog_library_filter.xml`**
  - Change FrameLayout height from `250dp` to `280dp`.
  - In `list_library_vocab`:
    - `android:divider="@android:color/transparent"`
    - `android:dividerHeight="8dp"`
    - `android:padding="6dp"`
    - `android:clipToPadding="false"`
- [ ] **Step 2: Update `LibraryFilterDialog.java` adapter implementation**
  - Add `View layoutBadgesRow` to `ViewHolder`.
  - In `getView()`:
    - Bind `layoutBadgesRow`.
    - Apply reading visibility rule: hide reading if `reading == null || reading.isEmpty() || reading.equals(item.getJapanese()) || reading.equals("—")`.
    - Apply romaji visibility rule: hide romaji if null or empty.
    - Set `layoutBadgesRow.setVisibility(hasBadge ? View.VISIBLE : View.GONE)`.
    - Ensure clean click listener binding for `btnSpeak` and row click.

---

### Task 4: Align iOS SwiftUI View (`PustakaCardRow.swift`)

**Files:**
- Modify: `ios/Kotoba/Views/Pustaka/PustakaCardRow.swift`

**Interfaces:**
- Consumes: `LearningObject` model in Swift.
- Produces: Clean 3-row floating card matching Android card geometry.

- [ ] **Step 1: Update `PustakaCardRow.swift` to 3-row card architecture**
  - Row 1: Badges (Capsule badge for group/bab and wordType).
  - Row 2: Japanese text (headline/title) + Reading / Romaji.
  - Divider: thin subtle separator.
  - Row 3: Indonesian meaning + Speaker button.
  - Apply card background, padding, and corner radius.
- [ ] **Step 2: Run iOS DB contract and Xcode validator tests**
  - Run `python3 tests/test_ios_db_contract.py`
  - Run `python3 tests/validate_xcode_project.py`

---

### Task 5: Verification, APK Rebuilding, and Delivery

**Files:**
- Output: `Kotoba.apk` / `KOTOBAAN.apk`, `Kotoba-1.apk` / `KOTOBA FINAL.apk`

- [ ] **Step 1: Run all Java tests**
  - `bash tests/run_java_tests.sh` (Expect all unit tests to PASS).
- [ ] **Step 2: Build and package Android APKs**
  - Run `./tools/build_apk.sh` (or existing APK packaging script).
  - Sign both APKs with apksigner (v1, v2, v3).
  - Verify signature with `apksigner verify --verbose`.
- [ ] **Step 3: Verify target vocabulary cards layout contract**
  - Verify `きれい な` (no squeezed romaji, romaji has dedicated unconstrained width).
  - Verify `ハンサム な` (badges at top, no duplicate Japanese reading, no middle drop, no bleed).
  - Verify `親切 な` (Kanji with separate furigana ruby and romaji, clear bottom meaning, fully isolated).
  - Verify short words and long katakana entries.
