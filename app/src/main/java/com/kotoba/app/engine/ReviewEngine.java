package com.kotoba.app.engine;

import com.kotoba.app.data.model.LearningObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReviewEngine {

    public enum QuestionType {
        JAPANESE_TO_MEANING, // Target: Japanese string -> Options: Indonesian meanings
        MEANING_TO_JAPANESE, // Target: Indonesian meaning -> Options: Japanese words
        KANJI_TO_READING     // Target: Kanji -> Options: Hiragana readings
    }

    public static class Question {
        public static class SafeData {
            private final QuestionType type;
            private final String targetId;
            private final String promptInstruction;
            private final String targetSubject;
            private final String badgeLabel;
            private final List<String> options;

            public SafeData(QuestionType type, String targetId, String promptInstruction,
                            String targetSubject, String badgeLabel, List<String> options) {
                this.type = type;
                this.targetId = targetId;
                this.promptInstruction = promptInstruction;
                this.targetSubject = targetSubject;
                this.badgeLabel = badgeLabel;
                this.options = Collections.unmodifiableList(options);
            }

            public QuestionType getType() { return type; }
            public String getTargetId() { return targetId; }
            public String getPromptInstruction() { return promptInstruction; }
            public String getTargetSubject() { return targetSubject; }
            public String getBadgeLabel() { return badgeLabel; }
            public List<String> getOptions() { return options; }
        }

        public static class RevealData {
            private final int correctOptionIndex;
            private final String correctAnswer;
            private final String targetJapanese;
            private final String targetReading;
            private final String targetMeaning;
            private final String explanation;

            public RevealData(int correctOptionIndex, String correctAnswer, String targetJapanese,
                              String targetReading, String targetMeaning, String explanation) {
                this.correctOptionIndex = correctOptionIndex;
                this.correctAnswer = correctAnswer;
                this.targetJapanese = targetJapanese;
                this.targetReading = targetReading;
                this.targetMeaning = targetMeaning;
                this.explanation = explanation;
            }

            public int getCorrectOptionIndex() { return correctOptionIndex; }
            public String getCorrectAnswer() { return correctAnswer; }
            public String getTargetJapanese() { return targetJapanese; }
            public String getTargetReading() { return targetReading; }
            public String getTargetMeaning() { return targetMeaning; }
            public String getExplanation() { return explanation; }
        }

        private final SafeData safeData;
        private final RevealData revealData;

        public Question(SafeData safeData, RevealData revealData) {
            this.safeData = safeData;
            this.revealData = revealData;
        }

        public SafeData getSafeData() { return safeData; }
        public RevealData getRevealData() { return revealData; }
    }

    public static Question generateQuestion(LearningObject target, List<LearningObject> pool, QuestionType type) {
        String promptInstruction;
        String targetSubject;
        String correctAnswer;

        if (type == QuestionType.JAPANESE_TO_MEANING) {
            promptInstruction = "Pilih arti bahasa Indonesia yang tepat:";
            targetSubject = target.getJapanese();
            correctAnswer = target.getIndonesian();
        } else if (type == QuestionType.MEANING_TO_JAPANESE) {
            promptInstruction = "Pilih kosakata bahasa Jepang yang tepat:";
            targetSubject = target.getIndonesian();
            correctAnswer = target.getJapanese();
        } else { // KANJI_TO_READING
            promptInstruction = "Pilih cara baca (furigana) yang tepat:";
            targetSubject = target.getJapanese();
            correctAnswer = target.getReading();
        }

        // Collect 3 unique distractors
        List<String> distractors = new ArrayList<>();
        List<LearningObject> shuffledPool = new ArrayList<>(pool);
        Collections.shuffle(shuffledPool);

        for (LearningObject obj : shuffledPool) {
            if (obj.getId().equals(target.getId())) continue;
            String candidate;
            if (type == QuestionType.JAPANESE_TO_MEANING) {
                candidate = obj.getIndonesian();
            } else if (type == QuestionType.MEANING_TO_JAPANESE) {
                candidate = obj.getJapanese();
            } else {
                candidate = obj.getReading();
            }
            if (!candidate.equals(correctAnswer) && !distractors.contains(candidate)) {
                distractors.add(candidate);
                if (distractors.size() == 3) break;
            }
        }

        // Fallback distractors if pool is too small
        while (distractors.size() < 3) {
            String fallback = "Pilihan alternatif " + (distractors.size() + 1);
            if (!distractors.contains(fallback)) {
                distractors.add(fallback);
            }
        }

        List<String> options = new ArrayList<>(distractors);
        options.add(correctAnswer);
        Collections.shuffle(options);

        int correctIndex = options.indexOf(correctAnswer);

        String explanation;
        if (target.getType() == LearningObject.Type.GRAMMAR) {
            explanation = target.getFormula() + "\n" + target.getExplanation();
        } else {
            explanation = target.getJapanese() + " (" + target.getReading() + ") — " + target.getIndonesian();
        }

        Question.SafeData safeData = new Question.SafeData(
                type,
                target.getId(),
                promptInstruction,
                targetSubject,
                target.getBadgeLabel(),
                options
        );

        Question.RevealData revealData = new Question.RevealData(
                correctIndex,
                correctAnswer,
                target.getJapanese(),
                target.getReading(),
                target.getIndonesian(),
                explanation
        );

        assertNoAnswerLeak(safeData, revealData);

        return new Question(safeData, revealData);
    }

    public static void assertNoAnswerLeak(Question.SafeData safe, Question.RevealData reveal) {
        if (safe.getTargetSubject().equalsIgnoreCase(reveal.getCorrectAnswer())) {
            throw new IllegalStateException("CRITICAL ANSWER LEAK: Target subject contains the correct answer directly!");
        }
        if (safe.getPromptInstruction().contains(reveal.getCorrectAnswer())) {
            throw new IllegalStateException("CRITICAL ANSWER LEAK: Prompt instruction leaked the correct answer!");
        }
    }
}
