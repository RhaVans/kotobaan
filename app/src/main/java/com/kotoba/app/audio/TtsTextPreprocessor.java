package com.kotoba.app.audio;

import java.util.regex.Pattern;

/**
 * Non-destructive text preprocessor for Japanese Text-To-Speech.
 * Cleans UI formatting, HTML tags, Markdown symbols, and English annotations
 * while strictly preserving authentic Japanese ideographs, kana mora timing,
 * long vowels, and pauses without silent substitution.
 */
public class TtsTextPreprocessor {

    private static final Pattern HTML_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern LATIN_ANNOTATION_PATTERN = Pattern.compile("[\\[\\(][a-zA-Z0-9\\s_\\-–]+[\\]\\)]");

    public static String preprocess(String input) {
        if (input == null) return "";
        String text = input.trim();
        if (text.isEmpty() || text.equals("—") || text.equals("-")) return "";

        // 1. Remove HTML tags
        text = HTML_PATTERN.matcher(text).replaceAll("");

        // 2. Remove Markdown formatting
        text = text.replaceAll("[*_~`#]+", "");

        // 3. Remove standalone Latin annotations like "[verb]" or "(N5)"
        text = LATIN_ANNOTATION_PATTERN.matcher(text).replaceAll("");

        // 4. Remove UI symbols, dashes, bullets, and stray delimiters
        // Preserve Japanese punctuation (、 。 ！ ？ 「 」) and prolonged sound mark (ー)
        text = text.replaceAll("[•·/\\\\|~^+=<>]", " ");
        text = text.replaceAll("^[\\s\\-\\–\\—:]+|[\\s\\-\\–\\—:]+$", "");

        // 5. Clean excess whitespace
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    public static boolean isAllKana(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // Hiragana: \u3040-\u309F, Katakana: \u30A0-\u30FF, Prolonged sound mark: \u30FC
            boolean isHiragana = (c >= 0x3040 && c <= 0x309F);
            boolean isKatakana = (c >= 0x30A0 && c <= 0x30FF);
            boolean isChouon = (c == 0x30FC);
            boolean isSpace = Character.isWhitespace(c);
            if (!isHiragana && !isKatakana && !isChouon && !isSpace) {
                return false;
            }
        }
        return true;
    }
}
