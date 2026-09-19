#!/usr/bin/env bash
set -e

echo "============================================================"
echo "KOTOBA.APP PRODUCTION BUILD PIPELINE (V2/V3 SIGNING)"
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

BUILD_DIR="$PROJECT_ROOT/build"
CLASSES_DIR="$BUILD_DIR/classes"

echo "1. Cleaning and preparing build directories..."
rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR"
find app/src/main/java -name "*.class" -delete 2>/dev/null || true

echo "2. Building canonical seed database..."
python3 "$PROJECT_ROOT/content/build_kotoba_database.py"

echo "3. Generating R.java via AAPT..."
aapt package -f -m \
    -J app/src/main/java \
    -M app/src/main/AndroidManifest.xml \
    -S app/src/main/res \
    -I "$ANDROID_JAR" \
    --min-sdk-version 26 \
    --target-sdk-version 35

echo "4. Compiling Java sources with javac (source/target 8)..."
JAVA_SOURCES=$(find app/src/main/java -name "*.java")
javac -source 8 -target 8 \
    -cp "$ANDROID_JAR:app/libs/onnxruntime.jar:app/src/main/java" \
    -d "$CLASSES_DIR" \
    $JAVA_SOURCES

echo "5. Converting bytecode to Dalvik Executable (classes.dex) via D8..."
CLASS_FILES=$(find "$CLASSES_DIR" -name "*.class")
java -cp "$R8_JAR" com.android.tools.r8.D8 \
    --min-api 26 \
    --lib "$ANDROID_JAR" \
    --output "$BUILD_DIR/" \
    $CLASS_FILES app/libs/onnxruntime.jar

echo "6. Assembling initial unaligned APK package..."
aapt package -f \
    -M app/src/main/AndroidManifest.xml \
    -S app/src/main/res \
    -A app/src/main/assets \
    -I "$ANDROID_JAR" \
    --min-sdk-version 26 \
    --target-sdk-version 35 \
    -F "$BUILD_DIR/Kotoba.unaligned.apk"

echo "7. Adding classes.dex and native libraries to APK package..."
(cd "$BUILD_DIR" && aapt add Kotoba.unaligned.apk classes.dex)

mkdir -p "$BUILD_DIR/lib/arm64-v8a"
cp app/libs/arm64-v8a/*.so "$BUILD_DIR/lib/arm64-v8a/"
(cd "$BUILD_DIR" && aapt add Kotoba.unaligned.apk lib/arm64-v8a/libonnxruntime.so lib/arm64-v8a/libonnxruntime4j_jni.so)

echo "8. Aligning APK on 4-byte boundaries via zipalign..."
zipalign -f -p 4 \
    "$BUILD_DIR/Kotoba.unaligned.apk" \
    "$BUILD_DIR/Kotoba.aligned.apk"

echo "9. Verifying 4-byte alignment..."
zipalign -c 4 "$BUILD_DIR/Kotoba.aligned.apk"

if [ -f "$KEYSTORE" ] && [ -f "$APKSIGNER_JAR" ]; then
    echo "10. Signing APK with APK Signature Scheme v1, v2 & v3 via apksigner..."
    java -jar "$APKSIGNER_JAR" sign \
        --ks "$KEYSTORE" \
        --ks-key-alias "$KEY_ALIAS" \
        --ks-pass "pass:$KEY_PASS" \
        --v1-signing-enabled true \
        --v2-signing-enabled true \
        --v3-signing-enabled true \
        --out "$PROJECT_ROOT/KOTOBA FINAL.apk" \
        "$BUILD_DIR/Kotoba.aligned.apk"

    echo "11. Verifying APK signatures (v1, v2, v3)..."
    java -jar "$APKSIGNER_JAR" verify --verbose --min-sdk-version 21 "$PROJECT_ROOT/KOTOBA FINAL.apk"
else
    echo "10. Keystore not found at $KEYSTORE. Copying aligned unsigned APK..."
    cp "$BUILD_DIR/Kotoba.aligned.apk" "$PROJECT_ROOT/KOTOBA FINAL.apk"
fi

APK_SIZE=$(ls -lh "$PROJECT_ROOT/KOTOBA FINAL.apk" | awk '{print $5}')
echo "============================================================"
echo "BUILD SUCCESSFUL: $PROJECT_ROOT/KOTOBA FINAL.apk ($APK_SIZE)"
echo "============================================================"
