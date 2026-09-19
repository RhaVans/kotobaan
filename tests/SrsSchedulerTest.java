package tests;

import com.kotoba.app.data.model.UserProgress;
import com.kotoba.app.engine.SrsScheduler;

public class SrsSchedulerTest {
    public static void main(String[] args) {
        System.out.println("Running SrsSchedulerTest...");

        // Test 1: New card initial state
        UserProgress p = UserProgress.createDefault("test_01");
        assert p.getMasteryState() == UserProgress.MasteryState.UNSEEN : "Initial state should be UNSEEN";
        assert p.getRepetitions() == 0 : "Initial reps should be 0";

        // Test 2: First successful review (fluent recall: 2000ms)
        long now = 1000000L;
        p = SrsScheduler.scheduleReview(p, true, 2000, now);
        assert p.getRepetitions() == 1 : "Reps should be 1";
        assert p.getIntervalDays() == 1 : "First interval should be 1 day";
        assert p.getMasteryState() == UserProgress.MasteryState.LEARNING : "State should be LEARNING";
        assert p.getNextReviewEpoch() == now + SrsScheduler.MILLIS_PER_DAY : "Due epoch should be +1 day";

        // Test 3: Second successful review (fluent recall: 1500ms)
        now = p.getNextReviewEpoch();
        p = SrsScheduler.scheduleReview(p, true, 1500, now);
        assert p.getRepetitions() == 2 : "Reps should be 2";
        assert p.getIntervalDays() == 6 : "Second interval should be 6 days";

        // Test 4: Third successful review
        now = p.getNextReviewEpoch();
        p = SrsScheduler.scheduleReview(p, true, 1200, now);
        assert p.getRepetitions() == 3 : "Reps should be 3";
        assert p.getIntervalDays() >= 14 : "Third interval should grow >= 14 days";
        assert p.getMasteryState() == UserProgress.MasteryState.STABLE : "State should transition to STABLE";

        // Test 5: Lapse / Incorrect answer
        now = p.getNextReviewEpoch();
        p = SrsScheduler.scheduleReview(p, false, 5000, now);
        assert p.getRepetitions() == 0 : "Reps should reset to 0 on lapse";
        assert p.getIntervalDays() == 1 : "Interval should reset to 1 on lapse";
        assert p.getLapses() == 1 : "Lapses should be 1";
        assert p.getMasteryState() == UserProgress.MasteryState.WEAK : "State should become WEAK on lapse";

        // Test 6: Consecutive lapse triggers quarantine (4 hours)
        now = p.getNextReviewEpoch();
        p = SrsScheduler.scheduleReview(p, false, 8000, now);
        assert p.getLapses() == 2 : "Lapses should be 2";
        assert p.getNextReviewEpoch() == now + SrsScheduler.QUARANTINE_MILLIS : "Repeated lapse should trigger 4-hour quarantine";

        System.out.println(" [PASS] SrsSchedulerTest: 6 assertions verified cleanly");
    }
}
