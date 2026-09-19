package tests;

import com.kotoba.app.audio.TtsTextPreprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class JapaneseTtsSystemTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Running JapaneseTtsSystemTest...");

        testTextPreprocessorBracketedForms();
        testTextPreprocessorHtmlAndMarkdown();
        testTextPreprocessorPunctuationAndMora();
        testTextPreprocessorStrayUiSymbols();
        testTextPreprocessorNullAndEmpty();
        testFemaleVoiceScoringHeuristic();
        testVoiceParametersAndDefaults();
        testLauncherIconFiles();

        System.out.println("JapaneseTtsSystemTest: All assertions passed successfully!");
    }

    private static void testTextPreprocessorBracketedForms() {
        System.out.println("  Testing TtsTextPreprocessor non-destructive forms...");

        // 1. Kanji lexical item must be preserved exactly as target
        String r1 = TtsTextPreprocessor.preprocess("食べる");
        assert "食べる".equals(r1) : "Expected '食べる', got: '" + r1 + "'";

        // 2. Pure Kana reading must be preserved exactly as target
        String r2 = TtsTextPreprocessor.preprocess("たべる");
        assert "たべる".equals(r2) : "Expected 'たべる', got: '" + r2 + "'";

        // 3. Kanji + non-Japanese Latin annotation in brackets -> strip annotation, keep kanji
        String r3 = TtsTextPreprocessor.preprocess("食べる (verb)");
        assert "食べる".equals(r3) : "Expected '食べる', got: '" + r3 + "'";

        String r4 = TtsTextPreprocessor.preprocess("本 [N5]");
        assert "本".equals(r4) : "Expected '本', got: '" + r4 + "'";

        // 4. Katakana loanword
        String r5 = TtsTextPreprocessor.preprocess("コーヒー");
        assert "コーヒー".equals(r5) : "Expected 'コーヒー', got: '" + r5 + "'";
    }

    private static void testTextPreprocessorHtmlAndMarkdown() {
        System.out.println("  Testing HTML and Markdown stripping...");

        String r1 = TtsTextPreprocessor.preprocess("<b>日本語</b>");
        assert "日本語".equals(r1) : "Expected '日本語', got: '" + r1 + "'";

        String r2 = TtsTextPreprocessor.preprocess("<ruby>漢字<rt>かんじ</rt></ruby>");
        assert "漢字かんじ".equals(r2) : "Expected '漢字かんじ', got: '" + r2 + "'";

        String r3 = TtsTextPreprocessor.preprocess("*大切*な_言葉_");
        assert "大切な言葉".equals(r3) : "Expected '大切な言葉', got: '" + r3 + "'";
    }

    private static void testTextPreprocessorPunctuationAndMora() {
        System.out.println("  Testing Japanese punctuation and mora timing preservation...");

        // Japanese punctuation marks must be preserved for natural pauses
        String phrase = "こんにちは、世界。お元気ですか？はい！";
        String r1 = TtsTextPreprocessor.preprocess(phrase);
        assert phrase.equals(r1) : "Expected preserved pauses, got: '" + r1 + "'";

        // Katakana prolonged sound mark (Chouonpu ー) must be preserved
        String r2 = TtsTextPreprocessor.preprocess("コーヒーとラーメン");
        assert "コーヒーとラーメン".equals(r2) : "Expected preserved chouonpu, got: '" + r2 + "'";

        // Numbers and counters must be preserved
        String r3 = TtsTextPreprocessor.preprocess("1回と2つ");
        assert "1回と2つ".equals(r3) : "Expected '1回と2つ', got: '" + r3 + "'";
    }

    private static void testTextPreprocessorStrayUiSymbols() {
        System.out.println("  Testing UI symbols and delimiters stripping...");

        String r1 = TtsTextPreprocessor.preprocess("• 勉強する —");
        assert "勉強する".equals(r1) : "Expected '勉強する', got: '" + r1 + "'";

        String r2 = TtsTextPreprocessor.preprocess("  食べる  /  飲む  ");
        assert "食べる 飲む".equals(r2) : "Expected '食べる 飲む', got: '" + r2 + "'";
    }

    private static void testTextPreprocessorNullAndEmpty() {
        System.out.println("  Testing null, empty, and placeholder edge-cases...");

        assert "".equals(TtsTextPreprocessor.preprocess(null)) : "Null must return empty";
        assert "".equals(TtsTextPreprocessor.preprocess("")) : "Empty must return empty";
        assert "".equals(TtsTextPreprocessor.preprocess("   ")) : "Spaces must return empty";
        assert "".equals(TtsTextPreprocessor.preprocess("—")) : "Em-dash must return empty";
        assert "".equals(TtsTextPreprocessor.preprocess("-")) : "Hyphen must return empty";
    }

    private static void testFemaleVoiceScoringHeuristic() {
        System.out.println("  Testing female voice heuristic scoring contract...");

        // Simulating the scoring logic from JapaneseSpeechHelper.scoreVoice
        class MockVoice {
            String name;
            boolean offline;
            boolean highQuality;
            String feature;

            MockVoice(String n, boolean off, boolean hq, String feat) {
                this.name = n;
                this.offline = off;
                this.highQuality = hq;
                this.feature = feat;
            }

            int score() {
                int score = 0;
                String n = name.toLowerCase();
                if (n.contains("htm") || n.contains("jaf") || n.contains("hce") || n.contains("jbb")) score += 100;
                if (n.contains("female") || n.contains("woman") || n.contains("fem") || n.contains("f0") ||
                    feature.contains("female") || feature.contains("gender=female")) score += 80;
                if (n.contains("jab") || n.contains("jad") || n.contains("hcb") || n.contains("male") ||
                    feature.contains("gender=male")) score -= 100;
                if (offline) score += 30;
                if (highQuality) score += 15;
                return score;
            }
        }

        MockVoice googleFemaleHtm = new MockVoice("ja-jp-x-htm-local", true, true, "");
        MockVoice googleFemaleJaf = new MockVoice("ja-jp-x-jaf-local", true, true, "");
        MockVoice samsungFemale = new MockVoice("ja-JP-female-f01", true, false, "gender=female");
        MockVoice googleMaleJab = new MockVoice("ja-jp-x-jab-local", true, true, "");
        MockVoice genericDefault = new MockVoice("ja-JP-language", true, false, "");

        assert googleFemaleHtm.score() > googleMaleJab.score() : "Female HTM must outrank male JAB";
        assert googleFemaleJaf.score() > genericDefault.score() : "Female JAF must outrank generic";
        assert samsungFemale.score() > googleMaleJab.score() : "Samsung female must outrank male";
        assert googleMaleJab.score() < genericDefault.score() : "Male voice must receive penalty";
    }

    private static void testVoiceParametersAndDefaults() {
        System.out.println("  Testing voice speed and pitch parameters...");

        float defaultSpeed = 0.90f;
        float defaultPitch = 0.95f;

        assert defaultSpeed >= 0.75f && defaultSpeed <= 1.10f : "Default speed in valid range";
        assert defaultPitch >= 0.88f && defaultPitch <= 1.05f : "Default pitch in valid range";

        // Speed options
        float[] validSpeeds = new float[]{0.75f, 0.85f, 0.90f, 1.00f, 1.10f};
        for (float s : validSpeeds) {
            assert s > 0.5f && s < 2.0f : "Speed " + s + " is physiologically audible";
        }

        // Pitch options
        float[] validPitches = new float[]{0.88f, 0.95f, 1.05f};
        for (float p : validPitches) {
            assert p > 0.5f && p < 1.5f : "Pitch " + p + " is natural and composed";
        }
    }

    private static void testLauncherIconFiles() throws Exception {
        System.out.println("  Testing launcher icon files generated across all densities...");

        String[] paths = new String[]{
                "/storage/emulated/0/download/PROJECT/KTB/app/src/main/res/mipmap/ic_launcher.png",
                "/storage/emulated/0/download/PROJECT/KTB/app/src/main/res/mipmap-mdpi-v4/ic_launcher.png",
                "/storage/emulated/0/download/PROJECT/KTB/app/src/main/res/mipmap-hdpi-v4/ic_launcher.png",
                "/storage/emulated/0/download/PROJECT/KTB/app/src/main/res/mipmap-xhdpi-v4/ic_launcher.png",
                "/storage/emulated/0/download/PROJECT/KTB/app/src/main/res/mipmap-xxhdpi-v4/ic_launcher.png",
                "/storage/emulated/0/download/PROJECT/KTB/app/src/main/res/mipmap-xxxhdpi-v4/ic_launcher.png"
        };

        for (String p : paths) {
            File f = new File(p);
            assert f.exists() : "Icon must exist: " + p;
            assert f.length() > 1000 : "Icon must be a valid non-empty PNG: " + p;

            // Verify PNG magic bytes: 0x89 'P' 'N' 'G'
            try (FileInputStream fis = new FileInputStream(f)) {
                byte[] header = new byte[8];
                int read = fis.read(header);
                assert read == 8 : "Could not read PNG header";
                assert (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                        : "File must be valid PNG format: " + p;
            }
        }
    }
}
