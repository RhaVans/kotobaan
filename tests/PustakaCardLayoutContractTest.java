package tests;

import com.kotoba.app.data.model.LearningObject;

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
        LearningObject item = new LearningObject(
                "adj_jft_0002",
                LearningObject.Type.VOCABULARY,
                LearningObject.WordType.KATA_SIFAT_NA,
                null,
                "JFT",
                "ハンサム (な)",
                "ハンサム (な)",
                "hansamu",
                "Tampan",
                "Kata Sifat #2",
                "01–50",
                1,
                null
        );

        boolean showReading = shouldDisplayReading(item.getJapanese(), item.getReading());
        assert !showReading : "Reading should be hidden when identical to Japanese (e.g. ハンサム (な))";
    }

    private static void testKanjiReadingPreservation() {
        LearningObject item = new LearningObject(
                "adj_jft_0003",
                LearningObject.Type.VOCABULARY,
                LearningObject.WordType.KATA_SIFAT_NA,
                null,
                "JFT",
                "親切 (な)",
                "しんせつ (な)",
                "shinsetsu",
                "Baik Hati",
                "Kata Sifat #3",
                "01–50",
                2,
                null
        );

        boolean showReading = shouldDisplayReading(item.getJapanese(), item.getReading());
        assert showReading : "Reading must be shown when Kanji has distinct reading (e.g. 親切 (な) -> しんせつ (な))";
    }

    private static void testBadgeRowVisibilityRules() {
        LearningObject itemWithBadges = new LearningObject(
                "adj_jft_0001",
                LearningObject.Type.VOCABULARY,
                LearningObject.WordType.KATA_SIFAT_NA,
                null,
                "JFT",
                "きれい (な)",
                "きれい (な)",
                "kirei",
                "Cantik / Bersih",
                "Kata Sifat #1",
                "01–50",
                0,
                null
        );
        assert hasBadges(itemWithBadges) : "Badge row must be visible when badge label is set";

        LearningObject itemEmpty = new LearningObject(
                "vocab_none",
                LearningObject.Type.VOCABULARY,
                LearningObject.WordType.LAINNYA,
                null,
                "N5",
                "これ",
                "これ",
                "kore",
                "Ini",
                null,
                null,
                1,
                null
        );
        assert !hasBadges(itemEmpty) : "Badge row must be hidden when no badges are present";
    }

    private static void testRomajiIntegrity() {
        LearningObject item = new LearningObject(
                "adj_jft_0001",
                LearningObject.Type.VOCABULARY,
                LearningObject.WordType.KATA_SIFAT_NA,
                null,
                "JFT",
                "きれい (な)",
                "きれい (な)",
                "kirei",
                "Cantik / Bersih",
                "Kata Sifat #1",
                "01–50",
                0,
                null
        );
        assert item.getRomaji() != null && !item.getRomaji().isEmpty() : "Romaji must be non-empty for kirei";
        assert "kirei".equals(item.getRomaji()) : "Romaji must match exactly";
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
