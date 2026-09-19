package com.kotoba.app.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LearningObject implements Serializable {
    public enum Type {
        VOCABULARY,
        KANJI,
        GRAMMAR
    }

    public enum WordType {
        KATA_BENDA,
        KATA_KERJA,
        KATA_SIFAT_I,
        KATA_SIFAT_NA,
        KATA_KETERANGAN,
        KATA_SAMBUNG,
        PARTIKEL,
        UNGKAPAN,
        LAINNYA;

        public static WordType fromString(String val) {
            if (val == null) return LAINNYA;
            try {
                return WordType.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return LAINNYA;
            }
        }

        public String getDisplayName() {
            switch (this) {
                case KATA_BENDA: return "Kata Benda";
                case KATA_KERJA: return "Kata Kerja";
                case KATA_SIFAT_I: return "Kata Sifat - い";
                case KATA_SIFAT_NA: return "Kata Sifat - な";
                case KATA_KETERANGAN: return "Kata Keterangan";
                case KATA_SAMBUNG: return "Kata Sambung";
                case PARTIKEL: return "Partikel";
                case UNGKAPAN: return "Ungkapan";
                default: return "Lainnya";
            }
        }
    }

    private final String id;
    private final Type type;
    private final WordType wordType;
    private final Integer bab;
    private final String level;
    private final String japanese;
    private final String reading;
    private final String romaji;
    private final String indonesian;
    private final String badgeLabel;
    private final String groupLabel;
    private final int sortOrder;
    private final String detailsJson;

    public LearningObject(String id, Type type, Integer bab, String level,
                          String japanese, String reading, String romaji,
                          String indonesian, String badgeLabel, String groupLabel,
                          int sortOrder, String detailsJson) {
        this(id, type, WordType.LAINNYA, bab, level, japanese, reading, romaji,
             indonesian, badgeLabel, groupLabel, sortOrder, detailsJson);
    }

    public LearningObject(String id, Type type, WordType wordType, Integer bab, String level,
                          String japanese, String reading, String romaji,
                          String indonesian, String badgeLabel, String groupLabel,
                          int sortOrder, String detailsJson) {
        this.id = id;
        this.type = type;
        this.wordType = wordType != null ? wordType : WordType.LAINNYA;
        this.bab = bab;
        this.level = level;
        this.japanese = japanese;
        this.reading = reading;
        this.romaji = romaji;
        this.indonesian = indonesian;
        this.badgeLabel = badgeLabel;
        this.groupLabel = groupLabel;
        this.sortOrder = sortOrder;
        this.detailsJson = detailsJson;
    }

    public String getId() { return id; }
    public Type getType() { return type; }
    public WordType getWordType() { return wordType; }
    public Integer getBab() { return bab; }
    public String getLevel() { return level; }
    public String getJapanese() { return japanese; }
    public String getReading() { return reading; }
    public String getRomaji() { return romaji != null ? romaji : ""; }
    public String getIndonesian() { return indonesian; }
    public String getBadgeLabel() { return badgeLabel; }
    public String getGroupLabel() { return groupLabel != null ? groupLabel : ""; }
    public int getSortOrder() { return sortOrder; }
    public String getDetailsJson() { return detailsJson; }

    public boolean hasKanji() {
        if (japanese == null || reading == null) return false;
        // Contains CJK Unified Ideographs range (\u4E00-\u9FFF)
        for (int i = 0; i < japanese.length(); i++) {
            char c = japanese.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                return true;
            }
        }
        return !japanese.equals(reading);
    }

    public boolean isKatakana() {
        if (japanese == null || japanese.isEmpty()) return false;
        for (int i = 0; i < japanese.length(); i++) {
            char c = japanese.charAt(i);
            boolean isKata = (c >= 0x30A0 && c <= 0x30FF) || (c == 0x30FC) || Character.isWhitespace(c);
            if (!isKata) return false;
        }
        return true;
    }

    public String getKanji() {
        return hasKanji() ? japanese : null;
    }

    public String getKatakana() {
        return isKatakana() ? japanese : null;
    }

    public String getMeaning() {
        return indonesian;
    }

    public String getFormula() {
        return extractJsonString(detailsJson, "formula");
    }

    public String getExplanation() {
        return extractJsonString(detailsJson, "explanation");
    }

    public static class KanjiVocabExample implements Serializable {
        private final String word;
        private final String reading;
        private final String romaji;
        private final String meaning;
        private final String readingType; // "ONYOMI" or "KUNYOMI"

        public KanjiVocabExample(String word, String reading, String romaji, String meaning, String readingType) {
            this.word = word != null ? word : "";
            this.reading = reading != null ? reading : "";
            this.romaji = romaji != null ? romaji : "";
            this.meaning = meaning != null ? meaning : "";
            this.readingType = readingType != null ? readingType : "";
        }

        public String getWord() { return word; }
        public String getReading() { return reading; }
        public String getRomaji() { return romaji; }
        public String getMeaning() { return meaning; }
        public String getReadingType() { return readingType; }
    }

    public List<String> getOnyomiList() {
        return extractJsonStringList(detailsJson, "onyomi");
    }

    public List<String> getKunyomiList() {
        return extractJsonStringList(detailsJson, "kunyomi");
    }

    public List<String> getOnyomiRomajiList() {
        return extractJsonStringList(detailsJson, "onyomi_romaji");
    }

    public List<String> getKunyomiRomajiList() {
        return extractJsonStringList(detailsJson, "kunyomi_romaji");
    }

    public boolean hasOnyomi() {
        List<String> list = getOnyomiList();
        return list != null && !list.isEmpty();
    }

    public boolean hasKunyomi() {
        List<String> list = getKunyomiList();
        return list != null && !list.isEmpty();
    }

    public String getOnyomiDisplay() {
        List<String> list = getOnyomiList();
        if (list == null || list.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append("、");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    public String getKunyomiDisplay() {
        List<String> list = getKunyomiList();
        if (list == null || list.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append("、");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    public String getPrimaryOnyomi() {
        if (detailsJson != null && detailsJson.contains("\"primary_onyomi\":")) {
            String val = extractJsonString(detailsJson, "primary_onyomi");
            return (!val.trim().isEmpty() && !"null".equalsIgnoreCase(val.trim())) ? val.trim() : null;
        }
        List<String> ons = getOnyomiList();
        return (ons != null && !ons.isEmpty()) ? ons.get(0) : null;
    }

    public String getPrimaryKunyomi() {
        if (detailsJson != null && detailsJson.contains("\"primary_kunyomi\":")) {
            String val = extractJsonString(detailsJson, "primary_kunyomi");
            return (!val.trim().isEmpty() && !"null".equalsIgnoreCase(val.trim())) ? val.trim() : null;
        }
        List<String> kuns = getKunyomiList();
        if (kuns != null && !kuns.isEmpty()) {
            for (String k : kuns) {
                if (!k.startsWith("-") && !k.endsWith("-")) {
                    return k;
                }
            }
            return kuns.get(0);
        }
        return null;
    }

    public String getPrimaryOnyomiRomaji() {
        String val = extractJsonString(detailsJson, "primary_onyomi_romaji");
        if (val != null && !val.trim().isEmpty() && !val.equals("null")) {
            return val.trim();
        }
        List<String> rList = getOnyomiRomajiList();
        return (rList != null && !rList.isEmpty()) ? rList.get(0) : "";
    }

    public String getPrimaryKunyomiRomaji() {
        String val = extractJsonString(detailsJson, "primary_kunyomi_romaji");
        if (val != null && !val.trim().isEmpty() && !val.equals("null")) {
            return val.trim();
        }
        List<String> rList = getKunyomiRomajiList();
        return (rList != null && !rList.isEmpty()) ? rList.get(0) : "";
    }

    public KanjiVocabExample getPrimaryKunyomiExample() {
        if (detailsJson == null || detailsJson.isEmpty()) return null;
        String needle = "\"primary_kunyomi_example\":";
        int start = detailsJson.indexOf(needle);
        if (start < 0) return null;
        int oStart = detailsJson.indexOf('{', start + needle.length());
        if (oStart < 0) return null;
        int oEnd = detailsJson.indexOf('}', oStart);
        if (oEnd < 0) return null;
        String objStr = detailsJson.substring(oStart, oEnd + 1);
        String word = extractJsonString(objStr, "word");
        String rd = extractJsonString(objStr, "reading");
        if (word.isEmpty()) return null;
        return new KanjiVocabExample(word, rd, "", "", "KUNYOMI");
    }

    public String getLearnerOnyomiDisplay() {
        String on = getPrimaryOnyomi();
        return "音: " + (on != null && !on.isEmpty() ? on : "—");
    }

    public String getLearnerKunyomiDisplay() {
        String kun = getPrimaryKunyomi();
        return "訓: " + (kun != null && !kun.isEmpty() ? kun : "—");
    }

    public String getLearnerBothReadingDisplay() {
        String on = getPrimaryOnyomi();
        String kun = getPrimaryKunyomi();
        StringBuilder sb = new StringBuilder();
        sb.append("音: ").append(on != null && !on.isEmpty() ? on : "—");
        sb.append("\n");
        sb.append("訓: ").append(kun != null && !kun.isEmpty() ? kun : "—");
        return sb.toString();
    }

    public String getLearnerReadingDisplay(String readingMode) {
        String mode = (readingMode != null) ? readingMode.toLowerCase().trim() : "both";
        if ("onyomi".equals(mode)) return getLearnerOnyomiDisplay();
        if ("kunyomi".equals(mode)) return getLearnerKunyomiDisplay();
        return getLearnerBothReadingDisplay();
    }

    public String getLearnerRomajiDisplay(String readingMode) {
        String mode = (readingMode != null) ? readingMode.toLowerCase().trim() : "both";
        String onR = getPrimaryOnyomiRomaji();
        String kunR = getPrimaryKunyomiRomaji();
        if ("onyomi".equals(mode)) return onR != null ? onR : "";
        if ("kunyomi".equals(mode)) return kunR != null ? kunR : "";
        if (onR != null && !onR.isEmpty() && kunR != null && !kunR.isEmpty()) {
            return onR + " / " + kunR;
        }
        return (onR != null && !onR.isEmpty()) ? onR : ((kunR != null && !kunR.isEmpty()) ? kunR : "");
    }

    public String getTtsTarget(String readingMode) {
        if (type != Type.KANJI) {
            return (reading != null && !reading.trim().isEmpty() && !reading.trim().equals("—"))
                    ? reading.trim()
                    : japanese;
        }

        String mode = (readingMode != null) ? readingMode.toLowerCase().trim() : "both";
        String pOn = getPrimaryOnyomi();
        String pKun = getPrimaryKunyomi();
        KanjiVocabExample kunEx = getPrimaryKunyomiExample();

        if ("onyomi".equals(mode)) {
            if (pOn != null && !pOn.trim().isEmpty() && !pOn.equals("—")) {
                return pOn.trim();
            }
            return (reading != null && !reading.trim().isEmpty() && !reading.trim().equals("—")) ? reading.trim() : japanese;
        } else if ("kunyomi".equals(mode)) {
            if (kunEx != null && kunEx.getReading() != null && !kunEx.getReading().isEmpty()) {
                return kunEx.getReading().trim();
            }
            if (pKun != null && !pKun.trim().isEmpty() && !pKun.equals("—")) {
                return pKun.replace(".", "").replace("-", "").trim();
            }
            if (pOn != null && !pOn.trim().isEmpty() && !pOn.equals("—")) {
                return pOn.trim();
            }
            return (reading != null && !reading.trim().isEmpty() && !reading.trim().equals("—")) ? reading.trim() : japanese;
        } else { // "both" -> strictly default to primary Kun'yomi for learner listening/comprehension
            if (kunEx != null && kunEx.getReading() != null && !kunEx.getReading().isEmpty()) {
                return kunEx.getReading().trim();
            }
            if (pKun != null && !pKun.trim().isEmpty() && !pKun.equals("—")) {
                return pKun.replace(".", "").replace("-", "").trim();
            }
            // Fallback to primary On'yomi if no Kun'yomi exists (e.g. 校)
            if (pOn != null && !pOn.trim().isEmpty() && !pOn.equals("—")) {
                return pOn.trim();
            }
            return (reading != null && !reading.trim().isEmpty() && !reading.trim().equals("—")) ? reading.trim() : japanese;
        }
    }

    public String getDualReadingDisplay() {
        String dual = extractJsonString(detailsJson, "dual_reading");
        if (dual != null && !dual.trim().isEmpty()) {
            return dual;
        }
        boolean hasOn = hasOnyomi();
        boolean hasKun = hasKunyomi();
        if (hasOn && hasKun) {
            return "音: " + getOnyomiDisplay() + " / 訓: " + getKunyomiDisplay();
        } else if (hasOn) {
            return "音: " + getOnyomiDisplay();
        } else if (hasKun) {
            return "訓: " + getKunyomiDisplay();
        }
        return reading != null ? reading : "—";
    }

    public List<KanjiVocabExample> getVocabExamples() {
        List<KanjiVocabExample> result = new ArrayList<KanjiVocabExample>();
        if (detailsJson == null || detailsJson.isEmpty()) return result;
        String needle = "\"vocab_examples\":";
        int start = detailsJson.indexOf(needle);
        if (start < 0) return result;
        start = detailsJson.indexOf('[', start + needle.length());
        if (start < 0) return result;
        int end = detailsJson.indexOf(']', start);
        if (end < 0) return result;
        String arrayContent = detailsJson.substring(start + 1, end).trim();
        if (arrayContent.isEmpty()) return result;

        int idx = 0;
        while (idx < arrayContent.length()) {
            int oStart = arrayContent.indexOf('{', idx);
            if (oStart < 0) break;
            int oEnd = arrayContent.indexOf('}', oStart);
            if (oEnd < 0) break;
            String objStr = arrayContent.substring(oStart, oEnd + 1);
            String word = extractJsonString(objStr, "word");
            String rd = extractJsonString(objStr, "reading");
            String rm = extractJsonString(objStr, "example_romaji");
            if (rm.isEmpty()) {
                rm = extractJsonString(objStr, "romaji");
            }
            String meaning = extractJsonString(objStr, "meaning");
            String type = extractJsonString(objStr, "reading_type");
            if (!word.isEmpty()) {
                result.add(new KanjiVocabExample(word, rd, rm, meaning, type));
            }
            idx = oEnd + 1;
        }
        return result;
    }

    public static List<String> extractJsonStringList(String json, String key) {
        List<String> list = new ArrayList<String>();
        if (json == null || json.isEmpty() || key == null) return list;
        String needle = "\"" + key + "\":";
        int start = json.indexOf(needle);
        if (start < 0) return list;
        start = json.indexOf('[', start + needle.length());
        if (start < 0) return list;
        int end = json.indexOf(']', start);
        if (end < 0) return list;
        String arrayContent = json.substring(start + 1, end).trim();
        if (arrayContent.isEmpty()) return list;

        int i = 0;
        while (i < arrayContent.length()) {
            int qStart = arrayContent.indexOf('\"', i);
            if (qStart < 0) break;
            int qEnd = arrayContent.indexOf('\"', qStart + 1);
            while (qEnd > 0 && arrayContent.charAt(qEnd - 1) == '\\') {
                qEnd = arrayContent.indexOf('\"', qEnd + 1);
            }
            if (qEnd < 0) break;
            String item = arrayContent.substring(qStart + 1, qEnd);
            list.add(item.replace("\\\"", "\"").replace("\\\\", "\\"));
            i = qEnd + 1;
        }
        return list;
    }

    private static String extractJsonString(String json, String key) {
        if (json == null || json.isEmpty() || key == null) return "";
        String needle = "\"" + key + "\":";
        int start = json.indexOf(needle);
        if (start < 0) return "";
        start += needle.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t' || json.charAt(start) == '\r' || json.charAt(start) == '\n')) {
            start++;
        }
        if (start >= json.length()) return "";
        if (json.startsWith("null", start)) {
            return "";
        }
        if (json.charAt(start) == '\"') {
            start++;
        } else {
            return "";
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != '\"') {
            if (json.charAt(end) == '\\') end++;
            end++;
        }
        if (end <= start) return "";
        String val = json.substring(start, end);
        return val.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LearningObject that = (LearningObject) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "LearningObject{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", wordType=" + wordType +
                ", japanese='" + japanese + '\'' +
                ", indonesian='" + indonesian + '\'' +
                '}';
    }
}
