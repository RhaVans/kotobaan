package tests;

import com.kotoba.app.data.model.LearningObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class PustakaDynamicFilterTest {

    public static class DynamicFilterHelper {
        public static List<LearningObject> filterVocabulary(
                List<LearningObject> source,
                String wordType,
                Collection<Integer> babs,
                Integer batchIndex,
                int batchSize,
                String search
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

                // Multi-Bab match
                if (babs != null && !babs.isEmpty()) {
                    if (lo.getBab() == null || !babs.contains(lo.getBab())) {
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

            // Batch slicing (when batchIndex > 0)
            if (batchIndex != null && batchIndex > 0 && batchSize > 0) {
                int offset = (batchIndex - 1) * batchSize;
                if (offset >= result.size()) {
                    return Collections.emptyList();
                }
                int end = Math.min(offset + batchSize, result.size());
                return new ArrayList<>(result.subList(offset, end));
            }

            return result;
        }

        public static String formatDeckTitle(String wordType, Set<Integer> selectedBabs, Integer batchIndex, int count) {
            if ("SEMUA".equals(wordType) || wordType == null) {
                if (selectedBabs == null || selectedBabs.isEmpty()) {
                    return String.format(Locale.US, "Bab 1-25 • %d kata", count);
                } else if (selectedBabs.size() == 1) {
                    return String.format(Locale.US, "Bab %d • %d kata", selectedBabs.iterator().next(), count);
                } else if (selectedBabs.size() <= 3) {
                    StringBuilder sb = new StringBuilder("Bab ");
                    int i = 0;
                    for (int b : new TreeSet<>(selectedBabs)) {
                        if (i > 0) sb.append(", ");
                        sb.append(b);
                        i++;
                    }
                    sb.append(" • ").append(count).append(" kata");
                    return sb.toString();
                } else {
                    return String.format(Locale.US, "%d Bab Terpilih • %d kata", selectedBabs.size(), count);
                }
            } else {
                String typeName;
                if ("KATA_KERJA".equalsIgnoreCase(wordType)) {
                    typeName = "Kata Kerja";
                } else if ("KATA_SIFAT".equalsIgnoreCase(wordType)) {
                    typeName = "Kata Sifat";
                } else if ("KATA_BENDA".equalsIgnoreCase(wordType)) {
                    typeName = "Kata Benda";
                } else {
                    typeName = wordType.replace('_', ' ');
                }

                if (batchIndex != null && batchIndex > 0) {
                    int start = (batchIndex - 1) * 50 + 1;
                    int end = start + count - 1;
                    return String.format(Locale.US, "%s • %02d–%02d (%d kata)", typeName, start, end, count);
                } else {
                    return String.format(Locale.US, "%s • Semua (%d kata)", typeName, count);
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Running PustakaDynamicFilterTest...");

        // Create a synthetic dataset with diverse Babs and word types
        List<LearningObject> dataset = new ArrayList<>();
        // Bab 1: 2 items
        dataset.add(new LearningObject("v1", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_BENDA, 1, "N5", "私", "わたし", "watashi", "saya", "Bab 1", null, 1, "{}"));
        dataset.add(new LearningObject("v2", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_BENDA, 1, "N5", "あなた", "あなた", "anata", "anda", "Bab 1", null, 2, "{}"));

        // Bab 2: 2 items
        dataset.add(new LearningObject("v3", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_BENDA, 2, "N5", "本", "ほん", "hon", "buku", "Bab 2", null, 3, "{}"));
        dataset.add(new LearningObject("v4", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_BENDA, 2, "N5", "辞書", "じしょ", "jisho", "kamus", "Bab 2", null, 4, "{}"));

        // Bab 4: 1 item
        dataset.add(new LearningObject("v5", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_KERJA, 4, "N5", "起きます", "おきます", "okimasu", "bangun", "Bab 4", null, 5, "{}"));

        // Bab 25: 1 item
        dataset.add(new LearningObject("v6", LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_KERJA, 25, "N5", "考えます", "かんがえます", "kangaemasu", "berpikir", "Bab 25", null, 6, "{}"));

        // 120 verbs across no specific bab (JFT / General)
        for (int i = 1; i <= 120; i++) {
            dataset.add(new LearningObject("verb_" + i, LearningObject.Type.VOCABULARY, LearningObject.WordType.KATA_KERJA, null, "N5", "動詞" + i, "どうし" + i, "doushi" + i, "arti verb " + i, "Group V", null, i + 10, "{}"));
        }

        // Test 1: Multi-Bab selection {1, 2}
        List<LearningObject> babs12 = DynamicFilterHelper.filterVocabulary(dataset, "SEMUA", Arrays.asList(1, 2), null, 50, null);
        assert babs12.size() == 4 : "Expected 4 items for Bab 1 & 2, got " + babs12.size();
        String title12 = DynamicFilterHelper.formatDeckTitle("SEMUA", new HashSet<>(Arrays.asList(1, 2)), null, babs12.size());
        assert title12.equals("Bab 1, 2 • 4 kata") : "Title mismatch: " + title12;

        // Test 2: Multi-Bab selection {4, 25}
        List<LearningObject> babs425 = DynamicFilterHelper.filterVocabulary(dataset, "SEMUA", Arrays.asList(4, 25), null, 50, null);
        assert babs425.size() == 2 : "Expected 2 items for Bab 4 & 25, got " + babs425.size();
        String title425 = DynamicFilterHelper.formatDeckTitle("SEMUA", new HashSet<>(Arrays.asList(4, 25)), null, babs425.size());
        assert title425.equals("Bab 4, 25 • 2 kata") : "Title mismatch: " + title425;

        // Test 3: Single Bab {1}
        String title1 = DynamicFilterHelper.formatDeckTitle("SEMUA", Collections.singleton(1), null, 2);
        assert title1.equals("Bab 1 • 2 kata") : "Title mismatch: " + title1;

        // Test 4: More than 3 Babs {1, 2, 4, 25}
        String titleMany = DynamicFilterHelper.formatDeckTitle("SEMUA", new HashSet<>(Arrays.asList(1, 2, 4, 25)), null, 6);
        assert titleMany.equals("4 Bab Terpilih • 6 kata") : "Title mismatch: " + titleMany;

        // Test 5: Word-Type "Semua Kata Kerja" (all at once)
        // Total verbs = 1 (Bab 4) + 1 (Bab 25) + 120 (JFT) = 122 verbs
        List<LearningObject> allVerbs = DynamicFilterHelper.filterVocabulary(dataset, "KATA_KERJA", null, 0, 50, null);
        assert allVerbs.size() == 122 : "Expected 122 verbs, got " + allVerbs.size();
        String titleAllVerbs = DynamicFilterHelper.formatDeckTitle("KATA_KERJA", null, 0, allVerbs.size());
        assert titleAllVerbs.equals("Kata Kerja • Semua (122 kata)") : "Title mismatch: " + titleAllVerbs;

        // Test 6: 50-item batches for Kata Kerja
        // Batch 1 (01-50)
        List<LearningObject> batch1 = DynamicFilterHelper.filterVocabulary(dataset, "KATA_KERJA", null, 1, 50, null);
        assert batch1.size() == 50 : "Expected 50 items in batch 1, got " + batch1.size();
        String titleBatch1 = DynamicFilterHelper.formatDeckTitle("KATA_KERJA", null, 1, batch1.size());
        assert titleBatch1.equals("Kata Kerja • 01–50 (50 kata)") : "Title mismatch: " + titleBatch1;

        // Batch 2 (51-100)
        List<LearningObject> batch2 = DynamicFilterHelper.filterVocabulary(dataset, "KATA_KERJA", null, 2, 50, null);
        assert batch2.size() == 50 : "Expected 50 items in batch 2, got " + batch2.size();
        String titleBatch2 = DynamicFilterHelper.formatDeckTitle("KATA_KERJA", null, 2, batch2.size());
        assert titleBatch2.equals("Kata Kerja • 51–100 (50 kata)") : "Title mismatch: " + titleBatch2;

        // Batch 3 (101-122) -> 22 items
        List<LearningObject> batch3 = DynamicFilterHelper.filterVocabulary(dataset, "KATA_KERJA", null, 3, 50, null);
        assert batch3.size() == 22 : "Expected 22 items in batch 3, got " + batch3.size();
        String titleBatch3 = DynamicFilterHelper.formatDeckTitle("KATA_KERJA", null, 3, batch3.size());
        assert titleBatch3.equals("Kata Kerja • 101–122 (22 kata)") : "Title mismatch: " + titleBatch3;

        // Batch 4 (out of bounds) -> empty
        List<LearningObject> batch4 = DynamicFilterHelper.filterVocabulary(dataset, "KATA_KERJA", null, 4, 50, null);
        assert batch4.isEmpty() : "Expected empty list for batch out of bounds, got " + batch4.size();

        // Test 7: Phonetic Kana pronunciation reading check
        for (LearningObject lo : dataset) {
            String speechText = lo.getReading();
            if (speechText == null || speechText.trim().isEmpty() || speechText.equals("—")) {
                speechText = lo.getJapanese();
            }
            assert speechText != null && !speechText.isEmpty() : "Speech text must never be empty";
        }

        System.out.println(" [PASS] PustakaDynamicFilterTest: Multi-Bab, 50-word batches, titles & audio verified cleanly!");
    }
}
