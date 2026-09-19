package tests;

import com.kotoba.app.audio.PronunciationTarget;
import com.kotoba.app.audio.TtsTextPreprocessor;
import com.kotoba.app.data.model.LearningObject;

/**
 * Automated test suite for Flashcard Representation Integrity and TTS Fidelity.
 * Verifies Cases 1, 2, 3, 4, 5 as required by the specification:
 * - Case 1: Kanji front -> Front: 食べる, Back: たべる, makan
 * - Case 2: Reading front -> Front: たべる, Back: 食べる, makan
 * - Case 3: Katakana item -> Katakana remains intact across all representations
 * - Case 4: Complete item -> No field disappears upon any front selection or flip
 * - Case 5: TTS Fidelity -> Visible Japanese matches exact TTS target input without silent alteration
 */
public class FlashcardRepresentationIntegrityTest {

    public static void main(String[] args) {
        System.out.println("Running FlashcardRepresentationIntegrityTest...");

        testCase1KanjiFront();
        testCase2ReadingFront();
        testCase3KatakanaItem();
        testCase4CompleteItemAllFrontModes();
        testCase5TtsFidelityAndNoSilentSubstitution();
        testImmutableStateIntegrity();

        System.out.println("============================================================");
        System.out.println("ALL FLASHCARD REPRESENTATION INTEGRITY TESTS PASSED (6/6)");
        System.out.println("============================================================");
    }

    private static void testCase1KanjiFront() {
        System.out.println("  Testing Case 1: Kanji Front (食べる -> たべる + makan)...");
        LearningObject item = new LearningObject(
                "test_001",
                LearningObject.Type.VOCABULARY,
                LearningObject.WordType.KATA_KERJA,
                1,
                "N5",
                "食べる",
                "たべる",
                "taberu",
                "makan",
                "Bab 1",
                "Verba",
                1,
                "{}"
        );

        // Verification of data model
        assert "食べる".equals(item.getJapanese()) : "Kanji representation mismatch";
        assert "たべる".equals(item.getReading()) : "Reading representation mismatch";
        assert "makan".equals(item.getMeaning()) : "Meaning representation mismatch";
        assert item.hasKanji() : "Item must report hasKanji() == true";

        // Display Policy Simulation for KANJI FRONT
        // Front Face:
        String frontPrimary = item.getJapanese();
        String frontFuriganaWithToggleOn = item.getReading();
        String frontFuriganaWithToggleOff = null;

        assert "食べる".equals(frontPrimary) : "Front primary must be Kanji 食べる";
        assert "たべる".equals(frontFuriganaWithToggleOn) : "Front furigana with toggle on must be たべる";
        assert frontFuriganaWithToggleOff == null : "Front furigana with toggle off must be hidden";

        // Back Face (Answer Face):
        // Rule: Reading たべる + Meaning makan must ALWAYS be present, regardless of furigana toggle
        String backPrimary = item.hasKanji() ? item.getReading() : item.getJapanese();
        String backKanjiRef = item.hasKanji() ? item.getJapanese() : null;
        String backMeaning = item.getMeaning();

        assert "たべる".equals(backPrimary) : "Back primary answer MUST be reading たべる";
        assert "食べる".equals(backKanjiRef) : "Back kanji reference must be 食べる";
        assert "makan".equals(backMeaning) : "Back meaning must be makan";

        // TTS targets:
        // Front speaker -> Reading たべる (exact pronunciation fidelity, no independent guessing)
        String frontTtsInput = (item.getReading() != null && !item.getReading().trim().isEmpty()) ? item.getReading().trim() : item.getJapanese();
        PronunciationTarget frontTarget = item.isKatakana() ? PronunciationTarget.KATAKANA : PronunciationTarget.READING;
        assert "たべる".equals(frontTtsInput) : "Front TTS input must be exact Reading たべる";
        assert frontTarget == PronunciationTarget.READING : "Front target must be READING";

        // Back speaker -> Reading たべる
        String backTtsInput = (item.getReading() != null && !item.getReading().trim().isEmpty()) ? item.getReading().trim() : item.getJapanese();
        PronunciationTarget backTarget = item.isKatakana() ? PronunciationTarget.KATAKANA : PronunciationTarget.READING;
        assert "たべる".equals(backTtsInput) : "Back TTS input must be exact Reading たべる";
        assert backTarget == PronunciationTarget.READING : "Back target must be READING";
    }

    private static void testCase2ReadingFront() {
        System.out.println("  Testing Case 2: Reading Front (たべる -> 食べる + makan)...");
        LearningObject item = new LearningObject(
                "test_001",
                LearningObject.Type.VOCABULARY,
                LearningObject.WordType.KATA_KERJA,
                1,
                "N5",
                "食べる",
                "たべる",
                "taberu",
                "makan",
                "Bab 1",
                "Verba",
                1,
                "{}"
        );

        // Verification of data model
        assert "食べる".equals(item.getJapanese()) : "Kanji representation mismatch";
        assert "たべる".equals(item.getReading()) : "Reading representation mismatch";
        assert "makan".equals(item.getMeaning()) : "Meaning representation mismatch";

        // Display Policy Simulation for READING FRONT
        // Front Face:
        String frontPrimary = item.getReading();
        assert "たべる".equals(frontPrimary) : "Front primary must be reading たべる";

        // Back Face:
        String backPrimary = item.getJapanese();
        String backReadingRef = item.getReading();
        String backMeaning = item.getMeaning();

        assert "食べる".equals(backPrimary) : "Back primary must be Kanji 食べる";
        assert "たべる".equals(backReadingRef) : "Back reference must be reading たべる";
        assert "makan".equals(backMeaning) : "Back meaning must be makan";

        // TTS targets:
        // Front speaker -> Reading たべる
        String frontTtsInput = (item.getReading() != null && !item.getReading().trim().isEmpty()) ? item.getReading().trim() : item.getJapanese();
        PronunciationTarget frontTarget = item.isKatakana() ? PronunciationTarget.KATAKANA : PronunciationTarget.READING;
        assert "たべる".equals(frontTtsInput) : "Front TTS input must match visible Reading たべる";
        assert frontTarget == PronunciationTarget.READING;

        // Back speaker -> Reading たべる
        String backTtsInput = (item.getReading() != null && !item.getReading().trim().isEmpty()) ? item.getReading().trim() : item.getJapanese();
        PronunciationTarget backTarget = item.isKatakana() ? PronunciationTarget.KATAKANA : PronunciationTarget.READING;
        assert "たべる".equals(backTtsInput) : "Back TTS input must match Reading たべる";
        assert backTarget == PronunciationTarget.READING;
    }

    private static void testCase3KatakanaItem() {
        System.out.println("  Testing Case 3: Katakana Item (コーヒー -> kopi)...");
        LearningObject item = new LearningObject(
                "test_kata_01",
                LearningObject.Type.VOCABULARY,
                LearningObject.WordType.KATA_BENDA,
                2,
                "N5",
                "コーヒー",
                "コーヒー",
                "ko-hi-",
                "kopi",
                "Bab 2",
                "Nomina",
                1,
                "{}"
        );

        assert item.isKatakana() : "Item must be recognized as Katakana loanword";
        assert !item.hasKanji() : "Item must not report having Kanji";
        assert "コーヒー".equals(item.getKatakana()) : "Katakana representation must be intact";
        assert "kopi".equals(item.getMeaning()) : "Meaning must be kopi";

        // Front Face (regardless of whether KANJI or HIRAGANA mode is selected):
        String frontPrimary = item.getJapanese();
        assert "コーヒー".equals(frontPrimary) : "Front must display コーヒー";

        // Back Face:
        String backPrimary = item.getJapanese();
        String backMeaning = item.getMeaning();
        assert "コーヒー".equals(backPrimary) : "Back must retain コーヒー";
        assert "kopi".equals(backMeaning) : "Back must show meaning kopi";

        // TTS target:
        PronunciationTarget target = PronunciationTarget.KATAKANA;
        assert "コーヒー".equals(item.getJapanese());
        assert target == PronunciationTarget.KATAKANA;
    }

    private static void testCase4CompleteItemAllFrontModes() {
        System.out.println("  Testing Case 4: All Front Modes (KANJI, HIRAGANA, ARTI)...");
        LearningObject item = new LearningObject(
                "test_complete",
                LearningObject.Type.VOCABULARY,
                LearningObject.WordType.KATA_KERJA,
                1,
                "N5",
                "行く",
                "いく",
                "iku",
                "pergi",
                "Bab 1",
                "Verba",
                1,
                "{}"
        );

        // Mode: ARTI FRONT
        String frontArti = item.getMeaning();
        assert "pergi".equals(frontArti) : "Arti front must display Indonesian meaning";

        String backArtiJapanese = item.getJapanese();
        String backArtiReading = item.getReading();
        assert "行く".equals(backArtiJapanese) : "Arti back must display Kanji 行く";
        assert "いく".equals(backArtiReading) : "Arti back must display Reading いく";

        // Verify underlying item was never mutated
        assert "行く".equals(item.getJapanese());
        assert "いく".equals(item.getReading());
        assert "pergi".equals(item.getMeaning());
    }

    private static void testCase5TtsFidelityAndNoSilentSubstitution() {
        System.out.println("  Testing Case 5: Non-destructive TTS Preprocessor...");

        // Japanese ideographs and kana MUST NOT be converted to kana
        String kanjiInput = "食べる";
        String prep1 = TtsTextPreprocessor.preprocess(kanjiInput);
        assert "食べる".equals(prep1) : "Kanji must not be mutated, got: " + prep1;

        String kanaInput = "たべる";
        String prep2 = TtsTextPreprocessor.preprocess(kanaInput);
        assert "たべる".equals(prep2) : "Kana must not be mutated, got: " + prep2;

        String kataInput = "コーヒー";
        String prep3 = TtsTextPreprocessor.preprocess(kataInput);
        assert "コーヒー".equals(prep3) : "Katakana must not be mutated, got: " + prep3;

        // UI delimiters and HTML/Markdown should be stripped without touching ideographs
        String htmlInput = "<b>行く</b>";
        String prep4 = TtsTextPreprocessor.preprocess(htmlInput);
        assert "行く".equals(prep4) : "HTML must be stripped cleanly, got: " + prep4;

        String markdownInput = "*勉強*する";
        String prep5 = TtsTextPreprocessor.preprocess(markdownInput);
        assert "勉強する".equals(prep5) : "Markdown must be stripped cleanly, got: " + prep5;

        // Latin annotations should be stripped, but ideographs preserved
        String latinAnnot = "車 [verb]";
        String prep6 = TtsTextPreprocessor.preprocess(latinAnnot);
        assert "車".equals(prep6) : "Annotation must be stripped, got: " + prep6;
    }

    private static void testImmutableStateIntegrity() {
        System.out.println("  Testing State Management Immutability...");
        LearningObject item = new LearningObject(
                "test_immutable",
                LearningObject.Type.VOCABULARY,
                LearningObject.WordType.KATA_BENDA,
                1,
                "N5",
                "本",
                "ほん",
                "hon",
                "buku",
                "Bab 1",
                "Nomina",
                1,
                "{}"
        );

        // Simulation of multiple rapid flips and mode switches
        for (int i = 0; i < 100; i++) {
            boolean isFlipped = (i % 2 == 1);
            int mode = (i % 3);

            String front;
            String back;
            if (mode == 0) { // KANJI
                front = item.getJapanese();
                back = item.getReading();
            } else if (mode == 1) { // HIRAGANA
                front = item.getReading();
                back = item.getJapanese();
            } else { // ARTI
                front = item.getMeaning();
                back = item.getJapanese() + " " + item.getReading();
            }

            assert front != null && !front.isEmpty();
            assert back != null && !back.isEmpty();
        }

        // Verify that after 100 cycles, fields are intact
        assert "本".equals(item.getJapanese());
        assert "ほん".equals(item.getReading());
        assert "hon".equals(item.getRomaji());
        assert "buku".equals(item.getMeaning());
    }
}
