#!/usr/bin/env bash
set -e

echo "============================================================"
echo "KOTOBAAN LITE (PRE-TTS SYSTEM-TTS) BUILD PIPELINE"
echo "============================================================"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

ANDROID_JAR="${ANDROID_JAR:-/root/.android_tools/android.jar}"
R8_JAR="${R8_JAR:-/root/.android_tools/r8.jar}"
APKSIGNER_JAR="${APKSIGNER_JAR:-/root/.android_tools/apksigner.jar}"
KEYSTORE="${KEYSTORE:-/root/.android_tools/release.keystore}"
KEY_ALIAS="${KEY_ALIAS:-bungkei}"
KEY_PASS="${KEY_PASS:-bungkeipass}"

BUILD_DIR="$PROJECT_ROOT/build_kotobaan"
CLASSES_DIR="$BUILD_DIR/classes"
ASSETS_LITE_DIR="$BUILD_DIR/assets"
SAGE_ENGINE_JAVA="$PROJECT_ROOT/app/src/main/java/com/kotoba/app/audio/SageTtsEngine.java"
SAGE_ENGINE_BACKUP="$BUILD_DIR/SageTtsEngine.java.bak"

cleanup() {
    if [ -f "$SAGE_ENGINE_BACKUP" ]; then
        echo "Restoring original SageTtsEngine.java..."
        cp "$SAGE_ENGINE_BACKUP" "$SAGE_ENGINE_JAVA"
        rm -f "$SAGE_ENGINE_BACKUP"
    fi
}
trap cleanup EXIT INT TERM

echo "1. Cleaning and preparing build directories..."
rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR"
mkdir -p "$ASSETS_LITE_DIR"

echo "2. Preparing lightweight assets (excluding ONNX neural model)..."
cp -r app/src/main/assets/* "$ASSETS_LITE_DIR/"
rm -f "$ASSETS_LITE_DIR/sage_voice.onnx"
rm -f "$ASSETS_LITE_DIR/model.onnx.json"
echo "   Lite assets prepared:"
ls -lh "$ASSETS_LITE_DIR"

echo "3. Generating R.java via AAPT..."
aapt package -f -m \
    -J app/src/main/java \
    -M app/src/main/AndroidManifest.xml \
    -S app/src/main/res \
    -I "$ANDROID_JAR" \
    --min-sdk-version 26 \
    --target-sdk-version 35

echo "4. Swapping SageTtsEngine with system-TTS stub for lite build..."
cp "$SAGE_ENGINE_JAVA" "$SAGE_ENGINE_BACKUP"
cat << 'EOF' > "$SAGE_ENGINE_JAVA"
package com.kotoba.app.audio;

import android.content.Context;

/**
 * Lightweight stub for KOTOBAAN pre-TTS edition.
 * Routes speech cleanly to native Android TextToSpeech.
 */
public class SageTtsEngine {
    public interface InitCallback {
        void onInitialized(boolean success, String message);
    }

    public interface SpeechCallback {
        void onSpeechStarted();
        void onSpeechCompleted(long latencyMs, float durationSec, float rtf);
        void onError(String error);
    }

    private static volatile SageTtsEngine sInstance;

    public static SageTtsEngine getInstance(Context context) {
        return null;
    }

    public boolean isInitialized() {
        return false;
    }

    public void initialize(final InitCallback callback) {
        if (callback != null) {
            callback.onInitialized(false, "System TTS mode");
        }
    }

    public void stop() {}

    public void speak(final String kanaText, final SpeechCallback callback) {
        if (callback != null) {
            callback.onError("System TTS fallback");
        }
    }

    public void release() {}
}
EOF

echo "5. Compiling Java sources with javac (source/target 8, no ONNX dependencies)..."
JAVA_SOURCES=$(find app/src/main/java -name "*.java" ! -name "TtsKanaPhonemizer.java")
javac -source 8 -target 8 \
    -cp "$ANDROID_JAR:app/src/main/java" \
    -d "$CLASSES_DIR" \
    $JAVA_SOURCES

echo "6. Converting bytecode to Dalvik Executable (classes.dex) via D8..."
CLASS_FILES=$(find "$CLASSES_DIR" -name "*.class")
java -cp "$R8_JAR" com.android.tools.r8.D8 \
    --min-api 26 \
    --lib "$ANDROID_JAR" \
    --output "$BUILD_DIR/" \
    $CLASS_FILES

echo "7. Assembling initial unaligned APK package with lite assets..."
aapt package -f \
    -M app/src/main/AndroidManifest.xml \
    -S app/src/main/res \
    -A "$ASSETS_LITE_DIR" \
    -I "$ANDROID_JAR" \
    --min-sdk-version 26 \
    --target-sdk-version 35 \
    -F "$BUILD_DIR/Kotobaan.unaligned.apk"

echo "8. Adding classes.dex to APK package (no native .so libraries)..."
(cd "$BUILD_DIR" && aapt add Kotobaan.unaligned.apk classes.dex)

echo "9. Aligning APK on 4-byte boundaries via zipalign..."
zipalign -f -p 4 \
    "$BUILD_DIR/Kotobaan.unaligned.apk" \
    "$BUILD_DIR/Kotobaan.aligned.apk"

echo "10. Verifying 4-byte alignment..."
zipalign -c 4 "$BUILD_DIR/Kotobaan.aligned.apk"

if [ -f "$KEYSTORE" ] && [ -f "$APKSIGNER_JAR" ]; then
    echo "11. Signing APK with APK Signature Scheme v1, v2 & v3 via apksigner..."
    java -jar "$APKSIGNER_JAR" sign \
        --ks "$KEYSTORE" \
        --ks-key-alias "$KEY_ALIAS" \
        --ks-pass "pass:$KEY_PASS" \
        --v1-signing-enabled true \
        --v2-signing-enabled true \
        --v3-signing-enabled true \
        --out "$PROJECT_ROOT/KOTOBAAN.apk" \
        "$BUILD_DIR/Kotobaan.aligned.apk"

    echo "12. Verifying APK signatures (v1, v2, v3)..."
    java -jar "$APKSIGNER_JAR" verify --verbose --min-sdk-version 21 "$PROJECT_ROOT/KOTOBAAN.apk"
else
    echo "11. Keystore not found at $KEYSTORE. Copying aligned unsigned APK..."
    cp "$BUILD_DIR/Kotobaan.aligned.apk" "$PROJECT_ROOT/KOTOBAAN.apk"
fi

APK_SIZE=$(ls -lh "$PROJECT_ROOT/KOTOBAAN.apk" | awk '{print $5}')
echo "============================================================"
echo "BUILD SUCCESSFUL: $PROJECT_ROOT/KOTOBAAN.apk ($APK_SIZE)"
echo "============================================================"
