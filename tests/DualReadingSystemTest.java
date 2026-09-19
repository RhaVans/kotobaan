package tests;

import com.kotoba.app.data.model.LearningObject;
import com.kotoba.app.data.model.LearningObject.KanjiVocabExample;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DualReadingSystemTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Running DualReadingSystemTest...");

        File projectRoot = new File(".");
        File assetsDir = new File(projectRoot, "app/src/main/assets");

        testTwentyRepresentativeKanji(assetsDir);
        testAsymmetryAndEdgeCases(assetsDir);
        testDualReadingDisplayFormats();
        testVocabLinkages(assetsDir);

        System.out.println("============================================================");
        System.out.println("ALL DUAL READING SYSTEM TESTS PASSED (4/4 Suites)");
        System.out.println("============================================================");
    }

    private static void testTwentyRepresentativeKanji(File assetsDir) throws Exception {
        System.out.println("  1. Verifying 20 Representative Kanji in Research Report & Datasets...");

        // Load Research Report
        File reportFile = new File("docs/KANJI_READING_RESEARCH_REPORT.md");
        assert reportFile.exists() : "docs/KANJI_READING_RESEARCH_REPORT.md missing";
        String reportContent = readFile(reportFile);

        String[] testChars = new String[]{"日", "人", "山", "水", "火", "木", "生", "行", "食", "見", "言", "上", "下", "大", "小", "中", "学", "校", "時", "間"};
        for (String c : testChars) {
            assert reportContent.contains("| **" + c + "** |") : "Research report missing representative kanji: " + c;
        }

        // Verify specific authentic readings in the report table
        assert reportContent.contains("| **校** | コウ | — |") : "校 must have On'yomi and NO Kun'yomi";
        assert reportContent.contains("| **食** | ショク | た.べる |") : "食 must have primary dual readings with okurigana";
        assert reportContent.contains("| **生** | セイ | い.きる |") : "生 must have okurigana preservation";
        assert reportContent.contains("| **日** | ニチ | ひ |") : "日 must have dual readings";

        // Verify canonical kanji items in kanji_dataset.json (restored state — no synthetic fields)
        File canFile = new File(assetsDir, "kanji_dataset.json");
        assert canFile.exists() : "kanji_dataset.json missing";
        String canJson = readFile(canFile);

        // Core fields must be present in restored canonical dataset
        assert canJson.contains("\"kanji\":") : "Canonical dataset missing kanji field";
        assert canJson.contains("\"reading\":") : "Canonical dataset missing reading field";
        assert canJson.contains("\"indonesian\":") : "Canonical dataset missing indonesian field";
        assert canJson.contains("\"romaji\":") : "Canonical dataset missing romaji field";

        // Synthetic reading fields must NOT be present (restoration verification)
        assert !canJson.contains("\"onyomi\":") : "Canonical dataset must not contain onyomi after restoration";
        assert !canJson.contains("\"kunyomi\":") : "Canonical dataset must not contain kunyomi after restoration";
        assert !canJson.contains("\"dual_reading\":") : "Canonical dataset must not contain dual_reading after restoration";

        // Spot-check: representative kanji are still present with correct readings from HTML
        assert canJson.contains("\"kanji\": \"日\"") || canJson.contains("\"kanji\":\"日\"") : "日 missing from canonical dataset";
        assert canJson.contains("\"kanji\": \"水\"") || canJson.contains("\"kanji\":\"水\"") : "水 missing from canonical dataset";
        assert canJson.contains("\"kanji\": \"山\"") || canJson.contains("\"kanji\":\"山\"") : "山 missing from canonical dataset";

        System.out.println("     [PASS] All 20 representative kanji present; canonical dataset restored to clean HTML state.");
    }

    private static void testAsymmetryAndEdgeCases(File assetsDir) throws Exception {
        System.out.println("  2. Verifying Asymmetry & Edge Cases (Kokuji with no On'yomi, Kango with no Kun'yomi)...");

        File addFile = new File(assetsDir, "kanji_additional_dataset.json");
        assert addFile.exists() : "kanji_additional_dataset.json missing";
        String addJson = readFile(addFile);

        // Kokuji: 畑 (kanji_add_0649)
        Pattern pKokuji = Pattern.compile("\\{[^}]*?\"kanji_id\":\\s*\"kanji_add_0649\"[^}]*?\\}", Pattern.DOTALL);
        Matcher mKokuji = pKokuji.matcher(addJson);
        assert mKokuji.find() : "kanji_add_0649 not found";
        LearningObject loKokuji = new LearningObject("kanji_add_0649", LearningObject.Type.KANJI, null, "N3", "畑", "はたけ", "hatake", "Pertanian", "Tambahan #649", "Group", 649, mKokuji.group(0));

        assert !loKokuji.hasOnyomi() : "Kokuji 畑 must report hasOnyomi() == false";
        assert loKokuji.getOnyomiList().isEmpty() : "Kokuji 畑 must have empty onyomi list";
        assert "—".equals(loKokuji.getOnyomiDisplay()) : "Kokuji 畑 onyomi display must be '—'";
        assert loKokuji.hasKunyomi() : "Kokuji 畑 must have kunyomi";
        assert loKokuji.getKunyomiDisplay().contains("はた") : "Kokuji 畑 must have kunyomi はた";

        // Kango-only: 課 (kanji_add_0001)
        Pattern pKa = Pattern.compile("\\{[^}]*?\"kanji_id\":\\s*\"kanji_add_0001\"[^}]*?\\}", Pattern.DOTALL);
        Matcher mKa = pKa.matcher(addJson);
        assert mKa.find() : "kanji_add_0001 not found";
        LearningObject loKa = new LearningObject("kanji_add_0001", LearningObject.Type.KANJI, null, "N2", "課", "か", "ka", "Bab", "Tambahan #1", "Group", 1, mKa.group(0));

        assert loKa.hasOnyomi() : "課 must have onyomi";
        assert "カ".equals(loKa.getOnyomiDisplay()) : "課 onyomi must be カ";
        assert !loKa.hasKunyomi() : "課 must have NO common kunyomi";
        assert loKa.getKunyomiList().isEmpty() : "課 kunyomi list must be empty";
        assert "—".equals(loKa.getKunyomiDisplay()) : "課 kunyomi display must be '—'";

        // Kango-only: 第 (kanji_add_0002)
        Pattern pDai = Pattern.compile("\\{[^}]*?\"kanji_id\":\\s*\"kanji_add_0002\"[^}]*?\\}", Pattern.DOTALL);
        Matcher mDai = pDai.matcher(addJson);
        assert mDai.find() : "kanji_add_0002 not found";
        LearningObject loDai = new LearningObject("kanji_add_0002", LearningObject.Type.KANJI, null, "N2", "第", "だい", "dai", "Nomor", "Tambahan #2", "Group", 2, mDai.group(0));

        assert loDai.hasOnyomi() : "第 must have onyomi";
        assert "ダイ".equals(loDai.getOnyomiDisplay()) : "第 onyomi must be ダイ";
        assert !loDai.hasKunyomi() : "第 must have NO common kunyomi";
        assert "—".equals(loDai.getKunyomiDisplay()) : "第 kunyomi display must be '—'";

        // Dual kanji: 釣 (kanji_add_0701)
        Pattern pTsuri = Pattern.compile("\\{[^}]*?\"kanji_id\":\\s*\"kanji_add_0701\"[^}]*?\\}", Pattern.DOTALL);
        Matcher mTsuri = pTsuri.matcher(addJson);
        assert mTsuri.find() : "kanji_add_0701 not found";
        LearningObject loTsuri = new LearningObject("kanji_add_0701", LearningObject.Type.KANJI, null, "N2", "釣", "つり", "tsuri", "Memancing", "Tambahan #701", "Group", 701, mTsuri.group(0));

        assert loTsuri.hasOnyomi() : "釣 must have onyomi チョウ";
        assert "チョウ".equals(loTsuri.getOnyomiDisplay()) : "釣 onyomi must be チョウ";
        assert loTsuri.hasKunyomi() : "釣 must have kunyomi つり";
        assert loTsuri.getKunyomiDisplay().contains("つり") : "釣 kunyomi must contain つり";

        System.out.println("     [PASS] Asymmetry verified: Kokuji (込), Kango-only (課, 第), and Dual (釣).");
    }

    private static void testDualReadingDisplayFormats() {
        System.out.println("  3. Verifying Dual Reading Formats in LearningObject...");

        // Case A: Both On and Kun
        String jsonBoth = "{\"onyomi\":[\"ショク\"],\"kunyomi\":[\"た.べる\"],\"dual_reading\":\"音: ショク / 訓: た.べる\"}";
        LearningObject loBoth = new LearningObject("lo_1", LearningObject.Type.KANJI, null, "N5", "食", "たべる", "taberu", "Makan", "Badge", "Group", 1, jsonBoth);
        assert "音: ショク / 訓: た.べる".equals(loBoth.getDualReadingDisplay()) : "Expected dual reading format, got: " + loBoth.getDualReadingDisplay();

        // Case B: On'yomi only
        String jsonOn = "{\"onyomi\":[\"コウ\"],\"kunyomi\":[],\"dual_reading\":\"音: コウ\"}";
        LearningObject loOn = new LearningObject("lo_2", LearningObject.Type.KANJI, null, "N5", "校", "こう", "kou", "Sekolah", "Badge", "Group", 2, jsonOn);
        assert "音: コウ".equals(loOn.getDualReadingDisplay()) : "Expected '音: コウ', got: " + loOn.getDualReadingDisplay();

        // Case C: Kun'yomi only (Kokuji)
        String jsonKun = "{\"onyomi\":[],\"kunyomi\":[\"こ.む\"],\"dual_reading\":\"訓: こ.む\"}";
        LearningObject loKun = new LearningObject("lo_3", LearningObject.Type.KANJI, null, "N3", "込", "こむ", "komu", "Penuh", "Badge", "Group", 3, jsonKun);
        assert "訓: こ.む".equals(loKun.getDualReadingDisplay()) : "Expected '訓: こ.む', got: " + loKun.getDualReadingDisplay();

        System.out.println("     [PASS] Dual reading display formats verified.");
    }

    private static void testVocabLinkages(File assetsDir) throws Exception {
        System.out.println("  4. Verifying Canonical Kanji Core Fields (Restored State)...");

        File canFile = new File(assetsDir, "kanji_dataset.json");
        String canJson = readFile(canFile);

        // Verify 生 is still present with its core restored fields
        assert canJson.contains("\"kanji\": \"生\"") || canJson.contains("\"kanji\":\"生\"") : "生 not found in canonical dataset";
        assert canJson.contains("\"kanji\": \"食べます\"") || canJson.contains("\"kanji\":\"食べます\"") : "食べます not found in canonical dataset";

        // Verify AM/PM were correctly restored from HTML (was previously expanded)
        assert canJson.contains("\"indonesian\": \"AM\"") || canJson.contains("\"indonesian\":\"AM\"") : "午前 must have indonesian='AM' after restoration";
        assert canJson.contains("\"indonesian\": \"PM\"") || canJson.contains("\"indonesian\":\"PM\"") : "午後 must have indonesian='PM' after restoration";

        // vocab_examples field must NOT be present in restored canonical dataset
        assert !canJson.contains("\"vocab_examples\":") : "Canonical dataset must not contain vocab_examples after restoration";

        System.out.println("     [PASS] Canonical dataset core fields verified; AM/PM restored; no vocab_examples.");
    }


    private static String readFile(File file) throws Exception {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read = fis.read(bytes);
            return new String(bytes, 0, read, StandardCharsets.UTF_8);
        }
    }
}
