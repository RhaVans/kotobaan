package com.kotoba.app.engine;

import com.kotoba.app.data.model.UserProgress;

public class SrsScheduler {
    public static final long MILLIS_PER_DAY = 86400000L;
    public static final long QUARANTINE_MILLIS = 4 * 3600000L; // 4 hours

    public static UserProgress scheduleReview(UserProgress current, boolean isCorrect, int responseTimeMs, long currentEpoch) {
        int interval = current.getIntervalDays();
        double ease = current.getEaseFactor();
        int reps = current.getRepetitions();
        int lapses = current.getLapses();
        double stability = current.getStability();

        // Evaluate recall quality
        boolean isStruggling = responseTimeMs > 10000;
        boolean isFluent = responseTimeMs >= 800 && responseTimeMs <= 4000;

        if (isCorrect) {
            reps++;
            if (isFluent) {
                ease = Math.min(3.0, ease + 0.1);
            } else if (isStruggling) {
                ease = Math.max(1.3, ease - 0.15);
            }

            if (reps == 1) {
                interval = 1;
                stability = 1.0;
            } else if (reps == 2) {
                interval = isStruggling ? 3 : 6;
                stability = interval;
            } else {
                double multiplier = ease;
                if (isStruggling) {
                    multiplier = Math.max(1.2, ease * 0.7);
                }
                interval = (int) Math.round(interval * multiplier);
                stability = interval;
            }

            // Next due epoch
            long nextDue = currentEpoch + (interval * MILLIS_PER_DAY);

            // Determine mastery state
            UserProgress.MasteryState state;
            if (reps >= 5 && interval >= 21) {
                state = UserProgress.MasteryState.MASTERED;
            } else if (reps >= 3 && interval >= 7) {
                state = UserProgress.MasteryState.STABLE;
            } else {
                state = UserProgress.MasteryState.LEARNING;
            }

            current.setIntervalDays(interval);
            current.setEaseFactor(ease);
            current.setRepetitions(reps);
            current.setStability(stability);
            current.setRetrievability(1.0);
            current.setLastResponseTimeMs(responseTimeMs);
            current.setLastReviewEpoch(currentEpoch);
            current.setNextReviewEpoch(nextDue);
            current.setMasteryState(state);

        } else {
            // Incorrect attempt / lapse
            lapses++;
            reps = 0;
            interval = 1;
            ease = Math.max(1.3, ease - 0.2);
            stability = 0.5;

            // Anti-runaway safeguard: quarantine item if repeated lapse
            long nextDue;
            if (lapses >= 2) {
                nextDue = currentEpoch + QUARANTINE_MILLIS;
            } else {
                nextDue = currentEpoch + MILLIS_PER_DAY;
            }

            current.setIntervalDays(interval);
            current.setEaseFactor(ease);
            current.setRepetitions(reps);
            current.setLapses(lapses);
            current.setStability(stability);
            current.setRetrievability(0.0);
            current.setLastResponseTimeMs(responseTimeMs);
            current.setLastReviewEpoch(currentEpoch);
            current.setNextReviewEpoch(nextDue);
            current.setMasteryState(UserProgress.MasteryState.WEAK);
        }

        return current;
    }
}
