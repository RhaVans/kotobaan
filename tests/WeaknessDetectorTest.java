package tests;

import com.kotoba.app.data.model.UserProgress;
import com.kotoba.app.engine.WeaknessDetector;

public class WeaknessDetectorTest {
    public static void main(String[] args) {
        System.out.println("Running WeaknessDetectorTest...");

        // Test 1: Latency profiling
        assert WeaknessDetector.profileLatency(500) == WeaknessDetector.LatencyProfile.IMPULSIVE : "500ms should be IMPULSIVE";
        assert WeaknessDetector.profileLatency(1500) == WeaknessDetector.LatencyProfile.FLUENT : "1500ms should be FLUENT";
        assert WeaknessDetector.profileLatency(6000) == WeaknessDetector.LatencyProfile.HESITANT : "6000ms should be HESITANT";
        assert WeaknessDetector.profileLatency(12000) == WeaknessDetector.LatencyProfile.STRUGGLING : "12000ms should be STRUGGLING";

        // Test 2: Weakness score for pristine card
        UserProgress p1 = UserProgress.createDefault("vocab_01");
        double score1 = WeaknessDetector.calculateWeaknessScore(p1);
        assert score1 == 0.0 : "Pristine card should have 0.0 weakness score";

        // Test 3: Weakness score for card with lapses
        p1.setLapses(3);
        p1.setEaseFactor(1.5);
        p1.setLastResponseTimeMs(11000);
        p1.setMasteryState(UserProgress.MasteryState.WEAK);
        double score2 = WeaknessDetector.calculateWeaknessScore(p1);
        assert score2 > 0.5 : "Card with 3 lapses and high latency should have score > 0.5";

        // Test 4: Explanation text
        String explanation = WeaknessDetector.getWeaknessExplanation(p1);
        assert explanation.contains("kesalahan berulang") : "Explanation should mention repeated mistakes";

        System.out.println(" [PASS] WeaknessDetectorTest: 4 assertions verified cleanly");
    }
}
