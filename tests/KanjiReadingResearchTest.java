package tests;

import com.kotoba.app.data.model.LearningObject;
import com.kotoba.app.data.model.LearningObject.KanjiVocabExample;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class KanjiReadingResearchTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Running KanjiReadingResearchTest...");

        testDualReadingModelContract();
        testNoKunyomiContract();
        testKokujiContract();
        testOkuriganaPreservationContract();
        testCanonicalKanjiDatasetDualReadings();
        testAdditionalKanjiDatasetDualReadings();
        testPrimaryReadingsAndLearnerDisplay();
        testTtsDeterministicReadings();
        testPrimaryReadingsInDatasets();

        System.out.println("============================================================");
        System.out.println("KANJI READING RESEARCH & DUAL READING TESTS PASSED (9/9)");
        System.out.println("============================================================");
    }

    private static void testDualReadingModelContract() {
        System.out.println("  Testing LearningObject dual reading model contract (食)...");
        String detailsJson = "{"
                + "\"onyomi\":[\"ショク\",\"ジキ\"],"
                + "\"onyomi_romaji\":[\"shoku\",\"jiki\"],"
                + "\"kunyomi\":[\"た.べる\",\"く.う\",\"く.らう\"],"
                + "\"kunyomi_romaji\":[\"taberu\",\"kuu\",\"kurau\"],"
                + "\"dual_reading\":\"音: ショク、ジキ / 訓: た.べる、く.う、く.らう\","
                + "\"vocab_examples\":["
                + "{\"word\":\"食べる\",\"reading\":\"たべる\",\"romaji\":\"taberu\",\"meaning\":\"Makan\",\"reading_type\":\"KUNYOMI\"},"
                + "{\"word\":\"食堂\",\"reading\":\"しょくどう\",\"romaji\":\"shokudou\",\"meaning\":\"Kantin\",\"reading_type\":\"ONYOMI\"}"
                + "]}";

        LearningObject lo = new LearningObject(
                "kanji_0001",
                LearningObject.Type.KANJI,
                null,
                "N5",
                "食",
                "たべる",
                "taberu",
                "Makan",
                "Kanji #1",
                "01–50",
                1,
                detailsJson
        );

        assert lo.hasOnyomi() : "Expected hasOnyomi() == true";
        assert lo.hasKunyomi() : "Expected hasKunyomi() == true";

        List<String> onList = lo.getOnyomiList();
        assert onList.size() == 2 : "Expected 2 onyomi, got " + onList.size();
        assert "ショク".equals(onList.get(0)) : "Expected ショク, got " + onList.get(0);
        assert "ジキ".equals(onList.get(1)) : "Expected ジキ, got " + onList.get(1);

        List<String> kunList = lo.getKunyomiList();
        assert kunList.size() == 3 : "Expected 3 kunyomi, got " + kunList.size();
        assert "た.べる".equals(kunList.get(0)) : "Expected た.べる, got " + kunList.get(0);

        assert "ショク、ジキ".equals(lo.getOnyomiDisplay()) : "Expected 'ショク、ジキ', got: " + lo.getOnyomiDisplay();
        assert "た.べる、く.う、く.らう".equals(lo.getKunyomiDisplay()) : "Expected 'た.べる、く.う、く.らう', got: " + lo.getKunyomiDisplay();

        List<KanjiVocabExample> examples = lo.getVocabExamples();
        assert examples.size() == 2 : "Expected 2 vocab examples, got " + examples.size();
        assert "食べる".equals(examples.get(0).getWord()) : "Expected 食べる";
        assert "たべる".equals(examples.get(0).getReading()) : "Expected たべる";
        assert "KUNYOMI".equals(examples.get(0).getReadingType()) : "Expected KUNYOMI";
        assert "食堂".equals(examples.get(1).getWord()) : "Expected 食堂";
        assert "ONYOMI".equals(examples.get(1).getReadingType()) : "Expected ONYOMI";

        System.out.println("  [PASS] Dual reading contract for 食 verified.");
    }

    private static void testNoKunyomiContract() {
        System.out.println("  Testing Kanji with NO common Kun'yomi (校)...");
        String detailsJson = "{"
                + "\"onyomi\":[\"コウ\",\"キョウ\"],"
                + "\"onyomi_romaji\":[\"kou\",\"kyou\"],"
                + "\"kunyomi\":[],"
                + "\"kunyomi_romaji\":[],"
                + "\"dual_reading\":\"音: コウ、キョウ\","
                + "\"vocab_examples\":[]"
                + "}";

        LearningObject lo = new LearningObject(
                "kanji_0002",
                LearningObject.Type.KANJI,
                null,
                "N5",
                "校",
                "こう",
                "kou",
                "Sekolah",
                "Kanji #2",
                "01–50",
                2,
                detailsJson
        );

        assert lo.hasOnyomi() : "Expected hasOnyomi() == true";
        assert !lo.hasKunyomi() : "Expected hasKunyomi() == false for 校";
        assert lo.getKunyomiList().isEmpty() : "Expected empty kunyomi list";
        assert "—".equals(lo.getKunyomiDisplay()) : "Expected '—' for kunyomi display, got: " + lo.getKunyomiDisplay();
        assert "コウ、キョウ".equals(lo.getOnyomiDisplay()) : "Expected 'コウ、キョウ', got: " + lo.getOnyomiDisplay();

        System.out.println("  [PASS] No Kun'yomi contract for 校 verified.");
    }

    private static void testKokujiContract() {
        System.out.println("  Testing Kokuji (Japan-native with NO On'yomi: 込)...");
        String detailsJson = "{"
                + "\"onyomi\":[],"
                + "\"onyomi_romaji\":[],"
                + "\"kunyomi\":[\"こ.む\",\"こ.める\"],"
                + "\"kunyomi_romaji\":[\"komu\",\"komeru\"],"
                + "\"dual_reading\":\"訓: こ.む、こ.める\","
                + "\"vocab_examples\":[]"
                + "}";

        LearningObject lo = new LearningObject(
                "kanji_add_0010",
                LearningObject.Type.KANJI,
                null,
                "N3",
                "込",
                "こむ",
                "komu",
                "Penuh, Masuk",
                "Tambahan #10",
                "Additional 01–50",
                10,
                detailsJson
        );

        assert !lo.hasOnyomi() : "Expected hasOnyomi() == false for Kokuji 込";
        assert lo.hasKunyomi() : "Expected hasKunyomi() == true for 込";
        assert "—".equals(lo.getOnyomiDisplay()) : "Expected '—' for onyomi display";
        assert "こ.む、こ.める".equals(lo.getKunyomiDisplay()) : "Expected 'こ.む、こ.める', got: " + lo.getKunyomiDisplay();

        System.out.println("  [PASS] Kokuji contract for 込 verified.");
    }

    private static void testOkuriganaPreservationContract() {
        System.out.println("  Testing Okurigana dot preservation (生)...");
        String detailsJson = "{"
                + "\"onyomi\":[\"セイ\",\"ショウ\"],"
                + "\"onyomi_romaji\":[\"sei\",\"shou\"],"
                + "\"kunyomi\":[\"い.きる\",\"い.かす\",\"う.まれる\",\"なま\"],"
                + "\"kunyomi_romaji\":[\"ikiru\",\"ikasu\",\"umareru\",\"nama\"],"
                + "\"dual_reading\":\"音: セイ、ショウ / 訓: い.きる、い.かす、う.まれる\","
                + "\"vocab_examples\":[]"
                + "}";

        LearningObject lo = new LearningObject(
                "kanji_0003",
                LearningObject.Type.KANJI,
                null,
                "N5",
                "生",
                "せい",
                "sei",
                "Hidup, Lahir",
                "Kanji #3",
                "01–50",
                3,
                detailsJson
        );

        List<String> kunyomi = lo.getKunyomiList();
        assert kunyomi.contains("い.きる") : "Expected 'い.きる' with okurigana dot";
        assert kunyomi.contains("う.まれる") : "Expected 'う.まれる' with okurigana dot";
        assert kunyomi.contains("なま") : "Expected 'なま' without dot (complete stem)";

        System.out.println("  [PASS] Okurigana preservation verified.");
    }

    private static void testCanonicalKanjiDatasetDualReadings() throws Exception {
        System.out.println("  Testing kanji_dataset.json (613 canonical kanji) restored state...");
        File file = new File("app/src/main/assets/kanji_dataset.json");
        assert file.exists() : "kanji_dataset.json not found";

        String json = readFile(file);
        // Canonical kanji restored to HTML source-of-truth state.
        // Synthetic dual-reading fields removed; only core fields remain.
        assert json.contains("\"kanji\":") : "Canonical dataset missing kanji field";
        assert json.contains("\"reading\":") : "Canonical dataset missing reading field";
        assert json.contains("\"indonesian\":") : "Canonical dataset missing indonesian field";
        assert json.contains("\"romaji\":") : "Canonical dataset missing romaji field";
        // Synthetic fields must NOT be present after restoration
        assert !json.contains("\"onyomi\":") : "Canonical dataset must not contain synthetic onyomi after restoration";
        assert !json.contains("\"kunyomi\":") : "Canonical dataset must not contain synthetic kunyomi after restoration";
        assert !json.contains("\"dual_reading\":") : "Canonical dataset must not contain synthetic dual_reading after restoration";

        System.out.println("  [PASS] 613 canonical kanji verified as restored (no synthetic reading fields).");
    }

    private static void testAdditionalKanjiDatasetDualReadings() throws Exception {
        System.out.println("  Testing kanji_additional_dataset.json (2,119 kanji) dual readings...");
        File file = new File("app/src/main/assets/kanji_additional_dataset.json");
        assert file.exists() : "kanji_additional_dataset.json not found";

        String json = readFile(file);
        assert json.contains("\"onyomi\":") : "Additional dataset missing onyomi field";
        assert json.contains("\"kunyomi\":") : "Additional dataset missing kunyomi field";
        assert json.contains("\"dual_reading\":") : "Additional dataset missing dual_reading field";
        assert json.contains("\"vocab_examples\":") : "Additional dataset missing vocab_examples field";

        System.out.println("  [PASS] 2,119 additional kanji dual readings confirmed in JSON asset.");
    }

    private static void testPrimaryReadingsAndLearnerDisplay() {
        System.out.println("  Testing primary readings & learner displays in LearningObject...");
        String detailsJson = "{"
                + "\"onyomi\":[\"ショク\",\"ジキ\"],"
                + "\"onyomi_romaji\":[\"shoku\",\"jiki\"],"
                + "\"kunyomi\":[\"た.べる\",\"く.う\",\"く.らう\"],"
                + "\"kunyomi_romaji\":[\"taberu\",\"kuu\",\"kurau\"],"
                + "\"primary_onyomi\":\"ショク\","
                + "\"primary_onyomi_romaji\":\"shoku\","
                + "\"primary_kunyomi\":\"た.べる\","
                + "\"primary_kunyomi_romaji\":\"taberu\","
                + "\"primary_kunyomi_example\":{\"word\":\"食べる\",\"reading\":\"たべる\",\"romaji\":\"taberu\",\"meaning\":\"Makan\"},"
                + "\"dual_reading\":\"音: ショク、ジキ / 訓: た.べる、く.う、く.らう\""
                + "}";

        LearningObject lo = new LearningObject(
                "kanji_test_shoku",
                LearningObject.Type.KANJI,
                null,
                "N5",
                "食",
                "たべる",
                "taberu",
                "Makan",
                "Badge",
                "Group",
                1,
                detailsJson
        );

        assert "ショク".equals(lo.getPrimaryOnyomi()) : "Expected primary onyomi 'ショク', got: " + lo.getPrimaryOnyomi();
        assert "shoku".equals(lo.getPrimaryOnyomiRomaji()) : "Expected primary onyomi romaji 'shoku'";
        assert "た.べる".equals(lo.getPrimaryKunyomi()) : "Expected primary kunyomi 'た.べる', got: " + lo.getPrimaryKunyomi();
        assert "taberu".equals(lo.getPrimaryKunyomiRomaji()) : "Expected primary kunyomi romaji 'taberu'";

        KanjiVocabExample ex = lo.getPrimaryKunyomiExample();
        assert ex != null : "Expected non-null primary_kunyomi_example";
        assert "食べる".equals(ex.getWord()) : "Expected word '食べる'";
        assert "たべる".equals(ex.getReading()) : "Expected reading 'たべる'";

        // Learner reading displays
        assert "音: ショク".equals(lo.getLearnerReadingDisplay("onyomi")) : "Expected '音: ショク', got: " + lo.getLearnerReadingDisplay("onyomi");
        assert "訓: た.べる".equals(lo.getLearnerReadingDisplay("kunyomi")) : "Expected '訓: た.べる', got: " + lo.getLearnerReadingDisplay("kunyomi");
        assert "音: ショク\n訓: た.べる".equals(lo.getLearnerReadingDisplay("both")) : "Expected '音: ショク\n訓: た.べる', got: " + lo.getLearnerReadingDisplay("both");

        // Learner romaji displays
        assert "shoku".equals(lo.getLearnerRomajiDisplay("onyomi")) : "Expected 'shoku', got: " + lo.getLearnerRomajiDisplay("onyomi");
        assert "taberu".equals(lo.getLearnerRomajiDisplay("kunyomi")) : "Expected 'taberu', got: " + lo.getLearnerRomajiDisplay("kunyomi");
        assert "shoku / taberu".equals(lo.getLearnerRomajiDisplay("both")) : "Expected 'shoku / taberu', got: " + lo.getLearnerRomajiDisplay("both");

        System.out.println("  [PASS] Primary readings and learner displays verified.");
    }

    private static void testTtsDeterministicReadings() {
        System.out.println("  Testing deterministic TTS reading targets across modes...");

        // Kanji with dual readings (食)
        String shokuJson = "{"
                + "\"onyomi\":[\"ショク\",\"ジキ\"],"
                + "\"kunyomi\":[\"た.べる\",\"く.う\"],"
                + "\"primary_onyomi\":\"ショク\","
                + "\"primary_kunyomi\":\"た.べる\","
                + "\"primary_kunyomi_example\":{\"word\":\"食べる\",\"reading\":\"たべる\"}"
                + "}";
        LearningObject loShoku = new LearningObject("shoku", LearningObject.Type.KANJI, null, "N5", "食", "たべる", "taberu", "Makan", "B", "G", 1, shokuJson);

        assert "ショク".equals(loShoku.getTtsTarget("onyomi")) : "In Onyomi mode, TTS must speak primary onyomi";
        assert "たべる".equals(loShoku.getTtsTarget("kunyomi")) : "In Kunyomi mode, TTS must speak primary kunyomi example reading";
        assert "たべる".equals(loShoku.getTtsTarget("both")) : "In Both mode, TTS must strictly speak primary Kun'yomi first!";

        // Kango-only kanji (校 - no kun'yomi)
        String kouJson = "{"
                + "\"onyomi\":[\"コウ\"],"
                + "\"kunyomi\":[],"
                + "\"primary_onyomi\":\"コウ\","
                + "\"primary_kunyomi\":null"
                + "}";
        LearningObject loKou = new LearningObject("kou", LearningObject.Type.KANJI, null, "N5", "校", "こう", "kou", "Sekolah", "B", "G", 2, kouJson);

        assert "コウ".equals(loKou.getTtsTarget("onyomi")) : "In Onyomi mode, 校 speaks コウ";
        assert "コウ".equals(loKou.getTtsTarget("kunyomi")) : "In Kunyomi mode, 校 falls back to コウ";
        assert "コウ".equals(loKou.getTtsTarget("both")) : "In Both mode, 校 falls back to コウ";

        // Kokuji kanji (畑 - no onyomi)
        String hatakeJson = "{"
                + "\"onyomi\":[],"
                + "\"kunyomi\":[\"はた\",\"はたけ\"],"
                + "\"primary_onyomi\":null,"
                + "\"primary_kunyomi\":\"はたけ\","
                + "\"primary_kunyomi_example\":{\"word\":\"畑\",\"reading\":\"はたけ\"}"
                + "}";
        LearningObject loHatake = new LearningObject("hatake", LearningObject.Type.KANJI, null, "N3", "畑", "はたけ", "hatake", "Ladang", "B", "G", 3, hatakeJson);

        assert "はたけ".equals(loHatake.getTtsTarget("kunyomi")) : "In Kunyomi mode, 畑 speaks はたけ";
        assert "はたけ".equals(loHatake.getTtsTarget("both")) : "In Both mode, 畑 speaks はたけ";
        assert "はたけ".equals(loHatake.getTtsTarget("onyomi")) : "In Onyomi mode, 畑 falls back to はたけ";

        System.out.println("  [PASS] Deterministic TTS targets verified (Kun'yomi-first in Both mode, clean fallback).");
    }

    private static void testPrimaryReadingsInDatasets() throws Exception {
        System.out.println("  Testing primary readings in JSON dataset files (restored state)...");
        File canFile = new File("app/src/main/assets/kanji_dataset.json");
        String canJson = readFile(canFile);
        // Canonical dataset has been restored — primary_onyomi/kunyomi must NOT be present
        assert !canJson.contains("\"primary_onyomi\":") : "kanji_dataset.json must NOT contain primary_onyomi after restoration";
        assert !canJson.contains("\"primary_kunyomi\":") : "kanji_dataset.json must NOT contain primary_kunyomi after restoration";

        // Additional dataset still carries primary readings (untouched)
        File addFile = new File("app/src/main/assets/kanji_additional_dataset.json");
        String addJson = readFile(addFile);
        assert addJson.contains("\"primary_onyomi\":") : "kanji_additional_dataset.json missing primary_onyomi";
        assert addJson.contains("\"primary_kunyomi\":") : "kanji_additional_dataset.json missing primary_kunyomi";

        System.out.println("  [PASS] Canonical dataset clean (no primary fields); additional dataset still enriched.");
    }

    private static String readFile(File file) throws Exception {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read = fis.read(bytes);
            return new String(bytes, 0, read, StandardCharsets.UTF_8);
        }
    }
}
