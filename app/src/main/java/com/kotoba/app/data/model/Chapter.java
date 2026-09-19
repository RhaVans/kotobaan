package com.kotoba.app.data.model;

import java.io.Serializable;

public class Chapter implements Serializable {
    private final int babNumber;
    private final String titleJa;
    private final String titleId;
    private final String theme;
    private final String level;
    private final int vocabCount;
    private final int grammarCount;

    public Chapter(int babNumber, String titleJa, String titleId, String theme, String level, int vocabCount, int grammarCount) {
        this.babNumber = babNumber;
        this.titleJa = titleJa;
        this.titleId = titleId;
        this.theme = theme;
        this.level = level;
        this.vocabCount = vocabCount;
        this.grammarCount = grammarCount;
    }

    public int getBabNumber() { return babNumber; }
    public String getTitleJa() { return titleJa; }
    public String getTitleId() { return titleId; }
    public String getTheme() { return theme; }
    public String getLevel() { return level; }
    public int getVocabCount() { return vocabCount; }
    public int getGrammarCount() { return grammarCount; }

    public String getDisplayLabel() {
        return "Bab " + babNumber + ": " + titleJa;
    }
}
