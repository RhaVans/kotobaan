package tests;

import com.kotoba.app.data.model.LearningObject;
import com.kotoba.app.engine.ReviewEngine;

import java.util.ArrayList;
import java.util.List;

public class ReviewEngineTest {
    public static void main(String[] args) {
        System.out.println("Running ReviewEngineTest...");

        List<LearningObject> pool = new ArrayList<>();
        pool.add(new LearningObject("vocab_0001", LearningObject.Type.VOCABULARY, 1, "N5", "あの人", "あのひと", "anohito", "orang itu", "Bab 1", null, 1, ""));
        pool.add(new LearningObject("vocab_0002", LearningObject.Type.VOCABULARY, 1, "N5", "あなた", "あなた", "anata", "anda", "Bab 1", null, 2, ""));
        pool.add(new LearningObject("vocab_0003", LearningObject.Type.VOCABULARY, 1, "N5", "アメリカ", "アメリカ", "amerika", "Amerika", "Bab 1", null, 3, ""));
        pool.add(new LearningObject("vocab_0004", LearningObject.Type.VOCABULARY, 1, "N5", "イギリス", "イギリス", "igirisu", "Inggris", "Bab 1", null, 4, ""));
        pool.add(new LearningObject("vocab_0005", LearningObject.Type.VOCABULARY, 1, "N5", "医者", "いしゃ", "isha", "dokter", "Bab 1", null, 5, ""));

        LearningObject target = pool.get(0); // "あの人" -> "orang itu"

        // Test 1: Question Generation Type A (Japanese -> Meaning)
        ReviewEngine.Question q1 = ReviewEngine.generateQuestion(target, pool, ReviewEngine.QuestionType.JAPANESE_TO_MEANING);
        ReviewEngine.Question.SafeData safe1 = q1.getSafeData();
        ReviewEngine.Question.RevealData reveal1 = q1.getRevealData();

        assert safe1.getTargetSubject().equals("あの人") : "Target subject should be Japanese string";
        assert safe1.getOptions().size() == 4 : "Options count should be 4";
        assert safe1.getOptions().contains("orang itu") : "Options should contain correct answer";
        assert safe1.getOptions().get(reveal1.getCorrectOptionIndex()).equals("orang itu") : "Correct index should point to correct option";

        // Verify SafeData contains no leaks
        for (String opt : safe1.getOptions()) {
            assert opt != null && !opt.isEmpty() : "Option should not be empty";
        }
        assert !safe1.getPromptInstruction().contains("orang itu") : "Prompt instruction must not leak answer";
        assert !safe1.getTargetSubject().contains("orang itu") : "Target subject must not leak answer";

        // Test 2: Question Generation Type B (Meaning -> Japanese)
        ReviewEngine.Question q2 = ReviewEngine.generateQuestion(target, pool, ReviewEngine.QuestionType.MEANING_TO_JAPANESE);
        ReviewEngine.Question.SafeData safe2 = q2.getSafeData();
        ReviewEngine.Question.RevealData reveal2 = q2.getRevealData();

        assert safe2.getTargetSubject().equals("orang itu") : "Target subject should be Indonesian gloss";
        assert safe2.getOptions().size() == 4 : "Options count should be 4";
        assert safe2.getOptions().contains("あの人") : "Options should contain target Japanese string";
        assert safe2.getOptions().get(reveal2.getCorrectOptionIndex()).equals("あの人") : "Correct index should point to correct Japanese option";

        // Test 3: Answer leak detection validator
        boolean caughtLeak = false;
        try {
            ReviewEngine.Question.SafeData leakySafe = new ReviewEngine.Question.SafeData(
                    ReviewEngine.QuestionType.JAPANESE_TO_MEANING,
                    "target_id",
                    "Pilih arti untuk orang itu:", // LEAK!
                    "あの人",
                    "Bab 1",
                    safe1.getOptions()
            );
            ReviewEngine.assertNoAnswerLeak(leakySafe, reveal1);
        } catch (IllegalStateException e) {
            caughtLeak = true;
        }
        assert caughtLeak : "assertNoAnswerLeak should catch prompt containing answer!";

        System.out.println(" [PASS] ReviewEngineTest: 5 assertions verified cleanly");
    }
}
