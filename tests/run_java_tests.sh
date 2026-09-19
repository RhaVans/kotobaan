#!/usr/bin/env bash
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TESTS_DIR="$PROJECT_ROOT/tests"
BUILD_DIR="$PROJECT_ROOT/build/test_classes"

ANDROID_JAR="${ANDROID_JAR:-/root/.android_tools/android.jar}"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

echo "Compiling Java sources and test files..."
javac -source 8 -target 8 \
    -cp "$ANDROID_JAR:$PROJECT_ROOT/app/src/main/java" \
    -d "$BUILD_DIR" \
    "$PROJECT_ROOT/app/src/main/java/com/kotoba/app/data/model/LearningObject.java" \
    "$PROJECT_ROOT/app/src/main/java/com/kotoba/app/data/model/UserProgress.java" \
    "$PROJECT_ROOT/app/src/main/java/com/kotoba/app/data/model/CycleItem.java" \
    "$PROJECT_ROOT/app/src/main/java/com/kotoba/app/data/model/LearningCycle.java" \
    "$PROJECT_ROOT/app/src/main/java/com/kotoba/app/engine/SrsScheduler.java" \
    "$PROJECT_ROOT/app/src/main/java/com/kotoba/app/engine/WeaknessDetector.java" \
    "$PROJECT_ROOT/app/src/main/java/com/kotoba/app/engine/ReviewEngine.java" \
    "$PROJECT_ROOT/app/src/main/java/com/kotoba/app/engine/IngatLupaEngine.java" \
    "$TESTS_DIR/SrsSchedulerTest.java" \
    "$TESTS_DIR/WeaknessDetectorTest.java" \
    "$TESTS_DIR/ReviewEngineTest.java" \
    "$TESTS_DIR/IngatLupaEngineTest.java" \
    "$PROJECT_ROOT/app/src/main/java/com/kotoba/app/audio/PronunciationTarget.java" \
    "$PROJECT_ROOT/app/src/main/java/com/kotoba/app/audio/TtsTextPreprocessor.java" \
    "$PROJECT_ROOT/app/src/main/java/com/kotoba/app/audio/TtsKanaPhonemizer.java" \
    "$TESTS_DIR/PustakaFilterTest.java" \
    "$TESTS_DIR/PustakaDynamicFilterTest.java" \
    "$TESTS_DIR/KanjiExpansionTest.java" \
    "$TESTS_DIR/ThemeAndStateTest.java" \
    "$TESTS_DIR/JapaneseTtsSystemTest.java" \
    "$TESTS_DIR/SageTtsKanaPhonemizerTest.java" \
    "$TESTS_DIR/FlashcardRepresentationIntegrityTest.java" \
    "$PROJECT_ROOT/app/src/main/java/com/kotoba/app/ui/responsive/ResponsiveLayoutSystem.java" \
    "$TESTS_DIR/PustakaCardLayoutContractTest.java" \
    "$TESTS_DIR/DataIntegrityAuditTest.java" \
    "$TESTS_DIR/ResponsiveLayoutSystemTest.java" \
    "$TESTS_DIR/KanjiReadingResearchTest.java" \
    "$TESTS_DIR/DualReadingSystemTest.java"

echo "Executing Java Test Suites (with -ea assertions enabled)..."
java -ea -cp "$BUILD_DIR" tests.SrsSchedulerTest
java -ea -cp "$BUILD_DIR" tests.WeaknessDetectorTest
java -ea -cp "$BUILD_DIR" tests.ReviewEngineTest
java -ea -cp "$BUILD_DIR" tests.IngatLupaEngineTest
java -ea -cp "$BUILD_DIR" tests.PustakaFilterTest
java -ea -cp "$BUILD_DIR" tests.PustakaDynamicFilterTest
java -ea -cp "$BUILD_DIR" tests.KanjiExpansionTest
java -ea -cp "$BUILD_DIR" tests.ThemeAndStateTest
java -ea -cp "$BUILD_DIR" tests.JapaneseTtsSystemTest
java -ea -cp "$BUILD_DIR" tests.SageTtsKanaPhonemizerTest
java -ea -cp "$BUILD_DIR" tests.FlashcardRepresentationIntegrityTest
java -ea -cp "$BUILD_DIR" tests.PustakaCardLayoutContractTest
java -ea -cp "$BUILD_DIR" tests.DataIntegrityAuditTest
java -ea -cp "$BUILD_DIR:$ANDROID_JAR" tests.ResponsiveLayoutSystemTest
java -ea -cp "$BUILD_DIR" tests.KanjiReadingResearchTest
java -ea -cp "$BUILD_DIR" tests.DualReadingSystemTest

echo "============================================================"
echo "ALL JAVA UNIT TESTS PASSED CLEANLY"
echo "============================================================"
