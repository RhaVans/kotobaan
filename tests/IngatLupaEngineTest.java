package tests;

import com.kotoba.app.data.model.CycleItem;
import com.kotoba.app.data.model.LearningCycle;
import com.kotoba.app.data.model.LearningObject;
import com.kotoba.app.engine.IngatLupaEngine;

import java.util.ArrayList;
import java.util.List;

public class IngatLupaEngineTest {

    private static List<LearningObject> createMockPool(int count, String prefix) {
        List<LearningObject> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new LearningObject(
                    prefix + "_" + i,
                    LearningObject.Type.VOCABULARY,
                    LearningObject.WordType.KATA_KERJA,
                    1,
                    "N5",
                    "Jap" + i,
                    "Read" + i,
                    "romaji" + i,
                    "Indo" + i,
                    "Badge" + i,
                    "01–50",
                    i,
                    "{}"
            ));
        }
        return list;
    }

    private static LearningCycle createMockCycle(int count) {
        return new LearningCycle(
                "cycle_test_001",
                LearningCycle.Source.CURRICULUM,
                LearningCycle.MaterialType.KOTOBA,
                count,
                LearningCycle.DisplayMode.KANJI,
                "{}",
                System.currentTimeMillis() / 1000L
        );
    }

    public static void main(String[] args) {
        System.out.println("Running IngatLupaEngineTest...");

        testEmptyPool();
        testAllIngat();
        testAllLupa();
        testSingleLupa();
        testRepeatedPartialFailure();
        testRecoveryIsolation();
        testFiftyCardJftBatch();

        System.out.println(" [PASS] IngatLupaEngineTest: All 7 edge-case suites verified cleanly");
    }

    private static void testEmptyPool() {
        LearningCycle cycle = createMockCycle(0);
        IngatLupaEngine engine = new IngatLupaEngine(cycle, new ArrayList<>());
        assert engine.isComplete() : "Empty pool must be immediately complete";
        assert engine.getCurrentItem() == null : "Empty pool current item must be null";
        assert engine.getCycleIteration() == 1 : "Iteration must start at 1";
    }

    private static void testAllIngat() {
        List<LearningObject> pool = createMockPool(20, "lo");
        LearningCycle cycle = createMockCycle(20);
        IngatLupaEngine engine = new IngatLupaEngine(cycle, pool);

        assert !engine.isComplete();
        assert engine.getInitialPoolSize() == 20;

        for (int i = 0; i < 20; i++) {
            assert !engine.isRecoveryRound();
            boolean done = engine.markIngat(1200);
            if (i < 19) {
                assert !done : "Should not be done before 20th item";
            } else {
                assert done : "Must be done after 20th item";
            }
        }

        assert engine.isComplete();
        assert engine.getCycleIteration() == 1 : "All INGAT must finish in exactly 1 iteration";
        assert engine.getTotalIngatCount() == 20;
        assert engine.getTotalLupaCount() == 0;
        assert engine.getRecordedCycleItems().size() == 20;
    }

    private static void testAllLupa() {
        List<LearningObject> pool = createMockPool(10, "lo");
        LearningCycle cycle = createMockCycle(10);
        IngatLupaEngine engine = new IngatLupaEngine(cycle, pool);

        // Pass 1: Mark all LUPA
        for (int i = 0; i < 10; i++) {
            engine.markLupa(1500);
        }

        // Must enter recovery round with all 10 items
        assert !engine.isComplete();
        assert engine.isRecoveryRound() : "Must be in recovery round";
        assert engine.getCycleIteration() == 2 : "Iteration must be 2";
        assert engine.getCurrentPassTotal() == 10 : "Recovery pass must have all 10 items";

        // Pass 2: Mark all INGAT
        for (int i = 0; i < 10; i++) {
            engine.markIngat(1000);
        }

        assert engine.isComplete() : "Must be complete after recovery pass";
        assert engine.getTotalLupaCount() == 10;
        assert engine.getTotalIngatCount() == 10;
        assert engine.getRecordedCycleItems().size() == 20;
    }

    private static void testSingleLupa() {
        List<LearningObject> pool = createMockPool(20, "lo");
        LearningCycle cycle = createMockCycle(20);
        IngatLupaEngine engine = new IngatLupaEngine(cycle, pool);

        // 19 INGAT, 1 LUPA (item at index 7)
        for (int i = 0; i < 20; i++) {
            if (i == 7) {
                engine.markLupa(2000);
            } else {
                engine.markIngat(800);
            }
        }

        // Must transition to recovery of strictly that 1 item
        assert !engine.isComplete();
        assert engine.isRecoveryRound();
        assert engine.getCycleIteration() == 2;
        assert engine.getCurrentPassTotal() == 1;
        assert engine.getCurrentItem().getId().equals("lo_8") : "Recovery item must be lo_8";

        // Mark it INGAT
        boolean done = engine.markIngat(900);
        assert done && engine.isComplete();
        assert engine.getTotalIngatCount() == 20;
        assert engine.getTotalLupaCount() == 1;
    }

    private static void testRepeatedPartialFailure() {
        // Simulates 50 -> 8 -> 3 -> 1 -> 0
        List<LearningObject> pool = createMockPool(50, "verb");
        LearningCycle cycle = createMockCycle(50);
        IngatLupaEngine engine = new IngatLupaEngine(cycle, pool);

        // Iteration 1: 42 INGAT, 8 LUPA
        for (int i = 0; i < 50; i++) {
            if (i < 8) {
                engine.markLupa(1200);
            } else {
                engine.markIngat(800);
            }
        }
        assert engine.getCycleIteration() == 2;
        assert engine.getCurrentPassTotal() == 8;

        // Iteration 2: 5 INGAT, 3 LUPA
        for (int i = 0; i < 8; i++) {
            if (i < 3) {
                engine.markLupa(1400);
            } else {
                engine.markIngat(900);
            }
        }
        assert engine.getCycleIteration() == 3;
        assert engine.getCurrentPassTotal() == 3;

        // Iteration 3: 2 INGAT, 1 LUPA
        for (int i = 0; i < 3; i++) {
            if (i == 0) {
                engine.markLupa(1600);
            } else {
                engine.markIngat(1000);
            }
        }
        assert engine.getCycleIteration() == 4;
        assert engine.getCurrentPassTotal() == 1;

        // Iteration 4: 1 INGAT (final pass)
        engine.markIngat(850);

        assert engine.isComplete();
        assert engine.getCycleIteration() == 4;
        assert engine.getTotalLupaCount() == 8 + 3 + 1;
        assert engine.getTotalIngatCount() == 42 + 5 + 2 + 1; // exactly 50 distinct items resolved
    }

    private static void testRecoveryIsolation() {
        List<LearningObject> pool = createMockPool(10, "item");
        LearningCycle cycle = createMockCycle(10);
        IngatLupaEngine engine = new IngatLupaEngine(cycle, pool);

        // Mark items 0..4 INGAT, items 5..9 LUPA
        for (int i = 0; i < 10; i++) {
            if (i < 5) engine.markIngat(700);
            else engine.markLupa(1500);
        }

        assert engine.getCycleIteration() == 2;
        assert engine.getCurrentPassTotal() == 5;

        // Verify that NO items from 0..4 appear in iteration 2
        for (int i = 0; i < 5; i++) {
            LearningObject current = engine.getCurrentItem();
            int idNum = Integer.parseInt(current.getId().replace("item_", ""));
            assert idNum >= 6 && idNum <= 10 : "Only items 6..10 may appear in recovery! Found: " + current.getId();
            engine.markIngat(800);
        }

        assert engine.isComplete();
    }

    private static void testFiftyCardJftBatch() {
        List<LearningObject> pool = createMockPool(50, "jft_verb");
        LearningCycle cycle = new LearningCycle(
                "cycle_jft_50",
                LearningCycle.Source.JFT_VERB,
                LearningCycle.MaterialType.KATA_KERJA,
                50,
                LearningCycle.DisplayMode.KANJI,
                "{}",
                System.currentTimeMillis() / 1000L
        );
        IngatLupaEngine engine = new IngatLupaEngine(cycle, pool);

        assert engine.getInitialPoolSize() == 50;
        assert engine.getCycleConfig().getMaterialType() == LearningCycle.MaterialType.KATA_KERJA;

        // 45 INGAT, 5 LUPA
        for (int i = 0; i < 50; i++) {
            if (i >= 45) {
                engine.markLupa(1800);
            } else {
                engine.markIngat(950);
            }
        }

        assert engine.isRecoveryRound();
        assert engine.getCurrentPassTotal() == 5;

        for (int i = 0; i < 5; i++) {
            engine.markIngat(750);
        }

        assert engine.isComplete();
        assert cycle.getStatus() == LearningCycle.Status.COMPLETED;
    }
}
