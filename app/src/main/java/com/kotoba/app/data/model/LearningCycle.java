package com.kotoba.app.data.model;

import java.io.Serializable;

public class LearningCycle implements Serializable {
    public enum Source {
        CURRICULUM,
        LIBRARY,
        JFT_VERB,
        JFT_ADJ,
        KANJI
    }

    public enum MaterialType {
        KOTOBA,
        KANJI,
        KATA_KERJA,
        KATA_SIFAT
    }

    public enum DisplayMode {
        KANJI,
        KANA,
        INDONESIA
    }

    public enum Status {
        ACTIVE,
        COMPLETED,
        ABANDONED
    }

    private final String cycleId;
    private final Source source;
    private final MaterialType materialType;
    private final int configuredAmount;
    private DisplayMode displayMode;
    private final String poolDefinitionJson;
    private final long startedAtEpoch;
    private long completedAtEpoch;
    private Status status;

    public LearningCycle(String cycleId, Source source, MaterialType materialType,
                         int configuredAmount, DisplayMode displayMode,
                         String poolDefinitionJson, long startedAtEpoch) {
        this(cycleId, source, materialType, configuredAmount, displayMode,
             poolDefinitionJson, startedAtEpoch, 0, Status.ACTIVE);
    }

    public LearningCycle(String cycleId, Source source, MaterialType materialType,
                         int configuredAmount, DisplayMode displayMode,
                         String poolDefinitionJson, long startedAtEpoch,
                         long completedAtEpoch, Status status) {
        this.cycleId = cycleId;
        this.source = source != null ? source : Source.CURRICULUM;
        this.materialType = materialType != null ? materialType : MaterialType.KOTOBA;
        this.configuredAmount = configuredAmount;
        this.displayMode = displayMode != null ? displayMode : DisplayMode.KANJI;
        this.poolDefinitionJson = poolDefinitionJson != null ? poolDefinitionJson : "";
        this.startedAtEpoch = startedAtEpoch;
        this.completedAtEpoch = completedAtEpoch;
        this.status = status != null ? status : Status.ACTIVE;
    }

    public String getCycleId() { return cycleId; }
    public Source getSource() { return source; }
    public MaterialType getMaterialType() { return materialType; }
    public int getConfiguredAmount() { return configuredAmount; }
    public DisplayMode getDisplayMode() { return displayMode; }
    public void setDisplayMode(DisplayMode displayMode) { this.displayMode = displayMode; }
    public String getPoolDefinitionJson() { return poolDefinitionJson; }
    public long getStartedAtEpoch() { return startedAtEpoch; }
    public long getCompletedAtEpoch() { return completedAtEpoch; }
    public void setCompletedAtEpoch(long completedAtEpoch) { this.completedAtEpoch = completedAtEpoch; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
