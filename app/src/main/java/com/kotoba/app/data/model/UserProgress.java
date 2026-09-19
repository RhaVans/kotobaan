package com.kotoba.app.data.model;

import java.io.Serializable;

public class UserProgress implements Serializable {
    public enum MasteryState {
        UNSEEN("○", "Baru"),
        LEARNING("▲", "Belajar"),
        WEAK("▼", "Lemah"),
        STABLE("●", "Stabil"),
        MASTERED("◆", "Terkuasai");

        private final String symbol;
        private final String label;

        MasteryState(String symbol, String label) {
            this.symbol = symbol;
            this.label = label;
        }

        public String getSymbol() { return symbol; }
        public String getLabel() { return label; }
        public String getFormatted() { return symbol + " " + label; }
    }

    private final String objectId;
    private int intervalDays;
    private double easeFactor;
    private int repetitions;
    private int lapses;
    private double stability;
    private double retrievability;
    private int lastResponseTimeMs;
    private long lastReviewEpoch;
    private long nextReviewEpoch;
    private MasteryState masteryState;

    public UserProgress(String objectId, int intervalDays, double easeFactor,
                        int repetitions, int lapses, double stability,
                        double retrievability, int lastResponseTimeMs,
                        long lastReviewEpoch, long nextReviewEpoch,
                        MasteryState masteryState) {
        this.objectId = objectId;
        this.intervalDays = intervalDays;
        this.easeFactor = easeFactor;
        this.repetitions = repetitions;
        this.lapses = lapses;
        this.stability = stability;
        this.retrievability = retrievability;
        this.lastResponseTimeMs = lastResponseTimeMs;
        this.lastReviewEpoch = lastReviewEpoch;
        this.nextReviewEpoch = nextReviewEpoch;
        this.masteryState = masteryState;
    }

    public static UserProgress createDefault(String objectId) {
        return new UserProgress(
                objectId,
                0,
                2.5,
                0,
                0,
                0.0,
                1.0,
                0,
                0L,
                0L,
                MasteryState.UNSEEN
        );
    }

    public String getObjectId() { return objectId; }
    public int getIntervalDays() { return intervalDays; }
    public double getEaseFactor() { return easeFactor; }
    public int getRepetitions() { return repetitions; }
    public int getLapses() { return lapses; }
    public double getStability() { return stability; }
    public double getRetrievability() { return retrievability; }
    public int getLastResponseTimeMs() { return lastResponseTimeMs; }
    public long getLastReviewEpoch() { return lastReviewEpoch; }
    public long getNextReviewEpoch() { return nextReviewEpoch; }
    public MasteryState getMasteryState() { return masteryState; }

    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }
    public void setEaseFactor(double easeFactor) { this.easeFactor = easeFactor; }
    public void setRepetitions(int repetitions) { this.repetitions = repetitions; }
    public void setLapses(int lapses) { this.lapses = lapses; }
    public void setStability(double stability) { this.stability = stability; }
    public void setRetrievability(double retrievability) { this.retrievability = retrievability; }
    public void setLastResponseTimeMs(int lastResponseTimeMs) { this.lastResponseTimeMs = lastResponseTimeMs; }
    public void setLastReviewEpoch(long lastReviewEpoch) { this.lastReviewEpoch = lastReviewEpoch; }
    public void setNextReviewEpoch(long nextReviewEpoch) { this.nextReviewEpoch = nextReviewEpoch; }
    public void setMasteryState(MasteryState masteryState) { this.masteryState = masteryState; }
}
