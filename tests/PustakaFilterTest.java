package tests;

import com.kotoba.app.data.model.LearningObject;

import java.util.ArrayList;
import java.util.List;

public class PustakaFilterTest {

    public static class PustakaFilterEvaluator {
        public static List<LearningObject> filter(
                List<LearningObject> source,
                String wordType,
                int bab,
                String search,
                boolean onlyWeak
        ) {
            List<LearningObject> result = new ArrayList<>();
            for (LearningObject lo : source) {
                if (lo.getType() != LearningObject.Type.VOCABULARY) {
                    continue;
                }

                // Word type match
                if (wordType != null && !wordType.isEmpty() && !wordType.equalsIgnoreCase("SEMUA")) {
                    if ("KATA_SIFAT".equalsIgnoreCase(wordType)) {
                        if (lo.getWordType() != LearningObject.WordType.KATA_SIFAT_I
                                && lo.getWordType() != LearningObject.WordType.KATA_SIFAT_NA) {
                            continue;
                        }
                    } else {
                        if (lo.getWordType() == null
                                || !wordType.equalsIgnoreCase(lo.getWordType().name())) {
                            continue;
                        }
                    }
                }

                // Bab match
                if (bab > 0) {
                    if (lo.getBab() == null || lo.getBab() != bab) {
                        continue;
                    }
                }

                // Search match
                if (search != null && !search.trim().isEmpty()) {
                    String q = search.trim().toLowerCase();
                    boolean matchJap = lo.getJapanese() != null && lo.getJapanese().toLowerCase().contains(q);
                    boolean matchRead = lo.getReading() != null && lo.getReading().toLowerCase().contains(q);
                    boolean matchRom = lo.getRomaji() != null && lo.getRomaji().toLowerCase().contains(q);
                    boolean matchIndo = lo.getIndonesian() != null && lo.getIndonesian().toLowerCase().contains(q);
                    if (!matchJap && !matchRead && !matchRom && !matchIndo) {
                        continue;
                    }
                }

                result.add(lo);
            }
            return result;
        }
    }

    private static List<LearningObject> createTestDataset() {
        List<LearningObject> list = new ArrayList<>();
        // Bab 1: Nouns only
        list.add(new LearningObject("v1", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_BENDA, 1, "N5", "あの人", "あのひと", "anohito", "orang itu", "Bab 1", null, 1, "{}"));
        list.add(new LearningObject("v2", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_BENDA, 1, "N5", "あなた", "あなた", "anata", "anda", "Bab 1", null, 2, "{}"));

        // Bab 8: Verbs, I-Adjectives, Na-Adjectives, Nouns
        list.add(new LearningObject("v3", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_KERJA, 8, "N5", "借ります", "かります", "karimasu", "meminjam", "Bab 8", null, 3, "{}"));
        list.add(new LearningObject("v4", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_KERJA, 8, "N5", "切ります", "きります", "kirimasu", "memotong", "Bab 8", null, 4, "{}"));
        list.add(new LearningObject("v5", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_SIFAT_I, 8, "N5", "青い", "あおい", "aoi", "biru", "Bab 8", null, 5, "{}"));
        list.add(new LearningObject("v6", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_SIFAT_NA, 8, "N5", "きれい", "きれい", "kirei", "cantik", "Bab 8", null, 6, "{}"));
        list.add(new LearningObject("v7", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_BENDA, 8, "N5", "花", "はな", "hana", "bunga", "Bab 8", null, 7, "{}"));

        // Bab 12: I-Adjective, Na-Adjective
        list.add(new LearningObject("v8", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_SIFAT_I, 12, "N5", "早い", "はやい", "hayai", "cepat", "Bab 12", null, 8, "{}"));
        list.add(new LearningObject("v9", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_SIFAT_NA, 12, "N5", "便利", "べんり", "benri", "praktis", "Bab 12", null, 9, "{}"));

        // Non-chapter JFT Verb (bab null)
        list.add(new LearningObject("v10", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_KERJA, null, "N5", "食べる", "たべる", "taberu", "makan", "Kata Kerja #1", "01–50", 10, "{}"));

        // Kanji character (type KANJI, wordType LAINNYA) - should never appear in vocabulary queries
        list.add(new LearningObject("k1", LearningObject.Type.KANJI, LearningObject.WordType.LAINNYA, null, "N5", "日", "ひ", "hi", "matahari", "Kanji #1", "01–50", 11, "{}"));

        return list;
    }

    public static void main(String[] args) {
        System.out.println("Running PustakaFilterTest...");
        List<LearningObject> dataset = createTestDataset();

        // 1. Kata Kerja filter
        List<LearningObject> verbs = PustakaFilterEvaluator.filter(dataset, "KATA_KERJA", 0, null, false);
        assert verbs.size() == 3 : "Expected 3 verbs, got " + verbs.size();
        for (LearningObject v : verbs) {
            assert v.getWordType() == LearningObject.WordType.KATA_KERJA : "Non-verb found";
        }

        // 2. Kata Sifat General filter (i + na)
        List<LearningObject> adjs = PustakaFilterEvaluator.filter(dataset, "KATA_SIFAT", 0, null, false);
        assert adjs.size() == 4 : "Expected 4 adjectives, got " + adjs.size();
        for (LearningObject a : adjs) {
            assert a.getWordType() == LearningObject.WordType.KATA_SIFAT_I || a.getWordType() == LearningObject.WordType.KATA_SIFAT_NA;
        }

        // 3. Kata Sifat - い filter
        List<LearningObject> iAdjs = PustakaFilterEvaluator.filter(dataset, "KATA_SIFAT_I", 0, null, false);
        assert iAdjs.size() == 2 : "Expected 2 i-adjectives, got " + iAdjs.size();
        for (LearningObject a : iAdjs) {
            assert a.getWordType() == LearningObject.WordType.KATA_SIFAT_I;
        }

        // 4. Kata Sifat - な filter
        List<LearningObject> naAdjs = PustakaFilterEvaluator.filter(dataset, "KATA_SIFAT_NA", 0, null, false);
        assert naAdjs.size() == 2 : "Expected 2 na-adjectives, got " + naAdjs.size();
        for (LearningObject a : naAdjs) {
            assert a.getWordType() == LearningObject.WordType.KATA_SIFAT_NA;
        }

        // 5. Kata Benda filter (No kanji!)
        List<LearningObject> nouns = PustakaFilterEvaluator.filter(dataset, "KATA_BENDA", 0, null, false);
        assert nouns.size() == 3 : "Expected 3 nouns, got " + nouns.size();
        for (LearningObject n : nouns) {
            assert n.getType() == LearningObject.Type.VOCABULARY && n.getWordType() == LearningObject.WordType.KATA_BENDA;
            assert !n.getId().startsWith("k") : "Kanji character leaked into Kata Benda";
        }

        // 6. Bab 1 filter
        List<LearningObject> b1 = PustakaFilterEvaluator.filter(dataset, "SEMUA", 1, null, false);
        assert b1.size() == 2 : "Expected 2 items in Bab 1, got " + b1.size();
        for (LearningObject item : b1) {
            assert item.getBab() != null && item.getBab() == 1;
        }

        // 7. Bab 8 filter
        List<LearningObject> b8 = PustakaFilterEvaluator.filter(dataset, "SEMUA", 8, null, false);
        assert b8.size() == 5 : "Expected 5 items in Bab 8, got " + b8.size();
        for (LearningObject item : b8) {
            assert item.getBab() != null && item.getBab() == 8;
        }

        // 8. Stacking: Kata Kerja + Bab 8
        List<LearningObject> b8Verbs = PustakaFilterEvaluator.filter(dataset, "KATA_KERJA", 8, null, false);
        assert b8Verbs.size() == 2 : "Expected exactly 2 verbs in Bab 8, got " + b8Verbs.size();
        for (LearningObject v : b8Verbs) {
            assert v.getWordType() == LearningObject.WordType.KATA_KERJA && v.getBab() == 8;
        }

        // 9. Stacking: Kata Sifat - い + Bab 12
        List<LearningObject> b12I = PustakaFilterEvaluator.filter(dataset, "KATA_SIFAT_I", 12, null, false);
        assert b12I.size() == 1 : "Expected 1 i-adjective in Bab 12, got " + b12I.size();
        assert b12I.get(0).getJapanese().equals("早い");

        // 10. Reset to "Semua" preserves Bab 8
        List<LearningObject> resetSemua = PustakaFilterEvaluator.filter(dataset, "SEMUA", 8, null, false);
        assert resetSemua.size() == 5 : "Expected all 5 items from Bab 8 upon word type reset, got " + resetSemua.size();

        // 11. Reset Bab (bab = 0) preserves Kata Kerja filter
        List<LearningObject> resetBab = PustakaFilterEvaluator.filter(dataset, "KATA_KERJA", 0, null, false);
        assert resetBab.size() == 3 : "Expected all 3 verbs upon Bab reset, got " + resetBab.size();

        // 12. Empty combination triggers 0 results (empty state)
        List<LearningObject> empty = PustakaFilterEvaluator.filter(dataset, "KATA_KERJA", 1, null, false);
        assert empty.isEmpty() : "Expected 0 results for verbs in Bab 1";

        // 13. Search filtering within active filter
        List<LearningObject> searched = PustakaFilterEvaluator.filter(dataset, "KATA_KERJA", 0, "きり", false);
        assert searched.size() == 1 && searched.get(0).getJapanese().equals("切ります");

        System.out.println(" [PASS] PustakaFilterTest: All 13 filtering and stacking invariants verified cleanly");
    }
}
