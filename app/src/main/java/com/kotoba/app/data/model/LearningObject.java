package com.kotoba.app.data.model;

import java.io.Serializable;

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

    private static String extractJsonString(String json, String key) {
        if (json == null || json.isEmpty() || key == null) return "";
        String needle = "\"" + key + "\":";
        int start = json.indexOf(needle);
        if (start < 0) return "";
        start += needle.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\"')) {
            start++;
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
