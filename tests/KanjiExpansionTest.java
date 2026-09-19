package tests;

import com.kotoba.app.data.model.LearningObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Unit test suite for Kanji Discovery & Expansion data models and segregation invariants.
 */
public class KanjiExpansionTest {

    public static class KanjiExpansionEvaluator {
        public static List<LearningObject> filter(
                List<LearningObject> source,
                String subSection,
                String groupLabel,
                String search
        ) {
            List<LearningObject> result = new ArrayList<>();
            for (LearningObject lo : source) {
                if (lo.getType() != LearningObject.Type.KANJI) {
                    continue;
                }

                // Sub-section segregation
                if ("613".equalsIgnoreCase(subSection)) {
                    if (lo.getId().startsWith("kanji_add_")) {
                        continue;
                    }
                } else if ("ADDITIONAL".equalsIgnoreCase(subSection) || "TAMBAHAN".equalsIgnoreCase(subSection)) {
                    if (!lo.getId().startsWith("kanji_add_")) {
                        continue;
                    }
                }

                // Group match
                if (groupLabel != null && !groupLabel.isEmpty() && !groupLabel.equalsIgnoreCase("SEMUA")) {
                    if (lo.getGroupLabel() == null || !groupLabel.equalsIgnoreCase(lo.getGroupLabel())) {
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

    public static void main(String[] args) {
        System.out.println("Running KanjiExpansionTest suite...");

        List<LearningObject> mockDataset = new ArrayList<>();

        // 1. Mock 613 Formal Kanji
        for (int i = 1; i <= 613; i++) {
            int g = ((i - 1) / 50) + 1;
            int start = (g - 1) * 50 + 1;
            int end = Math.min(613, g * 50);
            String groupLabel = String.format("%02d–%02d", start, end);

            mockDataset.add(new LearningObject(
                    String.format("kanji_%04d", i),
                    LearningObject.Type.KANJI,
                    LearningObject.WordType.LAINNYA,
                    null,
                    "N5",
                    "字" + i,
                    "じ" + i,
                    "ji" + i,
                    "Arti Formal " + i,
                    "Kanji #" + i,
                    groupLabel,
                    i,
                    "{}"
            ));
        }

        // 2. Mock 2,119 Additional Kanji in batches of 50
        for (int i = 1; i <= 2119; i++) {
            int b = ((i - 1) / 50) + 1;
            int start = (b - 1) * 50 + 1;
            int end = Math.min(2119, b * 50);
            String groupLabel = String.format("Additional %02d–%02d", start, end);

            mockDataset.add(new LearningObject(
                    String.format("kanji_add_%04d", i),
                    LearningObject.Type.KANJI,
                    LearningObject.WordType.LAINNYA,
                    null,
                    "Tambahan",
                    "添" + i,
                    "てん" + i,
                    "ten" + i,
                    "Arti Tambahan " + i,
                    "Tambahan #" + i,
                    groupLabel,
                    1000 + i,
                    "{}"
            ));
        }

        // Test 1: Formal 613 segregation
        List<LearningObject> formalOnly = KanjiExpansionEvaluator.filter(mockDataset, "613", "SEMUA", "");
        assert formalOnly.size() == 613 : "Expected exactly 613 formal kanji, got " + formalOnly.size();
        for (LearningObject lo : formalOnly) {
            assert lo.getId().startsWith("kanji_") && !lo.getId().startsWith("kanji_add_") : "Contamination in formal set: " + lo.getId();
        }
        System.out.println(" [PASS] Test 1: Formal 613 segregation (613 items, 0 contamination)");

        // Test 2: Additional Kanji segregation
        List<LearningObject> additionalOnly = KanjiExpansionEvaluator.filter(mockDataset, "ADDITIONAL", "SEMUA", "");
        assert additionalOnly.size() == 2119 : "Expected exactly 2119 additional kanji, got " + additionalOnly.size();
        for (LearningObject lo : additionalOnly) {
            assert lo.getId().startsWith("kanji_add_") : "Non-additional item in additional set: " + lo.getId();
        }
        System.out.println(" [PASS] Test 2: Additional Kanji segregation (2119 items, 0 collision)");

        // Test 3: Zero set intersection between 613 and Additional IDs
        Set<String> formalIds = new HashSet<>();
        for (LearningObject lo : formalOnly) formalIds.add(lo.getId());
        for (LearningObject lo : additionalOnly) {
            assert !formalIds.contains(lo.getId()) : "ID overlap detected: " + lo.getId();
        }
        System.out.println(" [PASS] Test 3: Zero ID intersection between 613 and Additional");

        // Test 4: Batch 1 filtering on Additional (capped at 50)
        List<LearningObject> batch1 = KanjiExpansionEvaluator.filter(mockDataset, "ADDITIONAL", "Additional 01–50", "");
        assert batch1.size() == 50 : "Batch 1 must contain exactly 50 cards, got " + batch1.size();
        assert batch1.get(0).getId().equals("kanji_add_0001") : "Batch 1 first card mismatch";
        assert batch1.get(49).getId().equals("kanji_add_0050") : "Batch 1 50th card mismatch";
        System.out.println(" [PASS] Test 4: Batch 1 (Additional 01–50) contains exactly 50 cards");

        // Test 5: Final partial batch filtering (Batch 43: 2101–2119 = 19 cards)
        List<LearningObject> batch43 = KanjiExpansionEvaluator.filter(mockDataset, "ADDITIONAL", "Additional 2101–2119", "");
        assert batch43.size() == 19 : "Final batch must contain exactly 19 cards, got " + batch43.size();
        assert batch43.get(18).getId().equals("kanji_add_2119") : "Final batch last card mismatch";
        System.out.println(" [PASS] Test 5: Final batch (Additional 2101–2119) contains exactly 19 cards");

        // Test 6: Search within Additional
        List<LearningObject> searchResults = KanjiExpansionEvaluator.filter(mockDataset, "ADDITIONAL", "SEMUA", "ten5");
        assert !searchResults.isEmpty() : "Search must return results for 'ten5'";
        for (LearningObject lo : searchResults) {
            assert lo.getRomaji().contains("ten5") : "Search result mismatch: " + lo.getRomaji();
        }
        System.out.println(" [PASS] Test 6: Search within Additional operates accurately (" + searchResults.size() + " matches)");

        System.out.println("============================================================");
        System.out.println("ALL KANJI EXPANSION TESTS PASSED (6/6 Assertions Verified)");
        System.out.println("============================================================");
    }
}
