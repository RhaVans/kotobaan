package com.kotoba.app.audio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ultra-lightweight Japanese Kana-to-IPA phonemizer for Sage TTS.
 * Footprint: < 15 KB, ZERO external dictionary dependencies.
 * Deterministically maps Hiragana/Katakana to Piper VITS IPA tokens.
 */
public final class TtsKanaPhonemizer {

    private static final Map<String, String[]> MORA_MAP = new HashMap<>();
    private static final Map<String, String[]> YOON_MAP = new HashMap<>();

    static {
        // Vowels
        putMora("あ", "a"); putMora("い", "i"); putMora("う", "ɯ"); putMora("え", "e"); putMora("お", "o");
        // K-row
        putMora("か", "k", "a"); putMora("き", "k", "i"); putMora("く", "k", "ɯ"); putMora("け", "k", "e"); putMora("こ", "k", "o");
        // G-row (Dakuon)
        putMora("が", "ɡ", "a"); putMora("ぎ", "ɡ", "i"); putMora("ぐ", "ɡ", "ɯ"); putMora("げ", "ɡ", "e"); putMora("ご", "ɡ", "o");
        // S-row
        putMora("さ", "s", "a"); putMora("し", "ɕ", "i"); putMora("す", "s", "ɯ"); putMora("せ", "s", "e"); putMora("そ", "s", "o");
        // Z-row
        putMora("ざ", "z", "a"); putMora("じ", "ʑ", "i"); putMora("ず", "z", "ɯ"); putMora("ぜ", "z", "e"); putMora("ぞ", "z", "o");
        // T-row
        putMora("た", "t", "a"); putMora("ち", "t", "ɕ", "i"); putMora("つ", "ʦ", "ɯ"); putMora("て", "t", "e"); putMora("と", "t", "o");
        // D-row
        putMora("だ", "d", "a"); putMora("ぢ", "ʑ", "i"); putMora("づ", "z", "ɯ"); putMora("で", "d", "e"); putMora("ど", "d", "o");
        // N-row
        putMora("な", "n", "a"); putMora("に", "ɲ", "i"); putMora("ぬ", "n", "ɯ"); putMora("ね", "n", "e"); putMora("の", "n", "o");
        // H-row
        putMora("は", "h", "a"); putMora("ひ", "ç", "i"); putMora("ふ", "ɸ", "ɯ"); putMora("へ", "h", "e"); putMora("ほ", "h", "o");
        // B-row
        putMora("ば", "b", "a"); putMora("び", "b", "i"); putMora("ぶ", "b", "ɯ"); putMora("べ", "b", "e"); putMora("ぼ", "b", "o");
        // P-row
        putMora("ぱ", "p", "a"); putMora("ぴ", "p", "i"); putMora("ぷ", "p", "ɯ"); putMora("ぺ", "p", "e"); putMora("ぽ", "p", "o");
        // M-row
        putMora("ま", "m", "a"); putMora("み", "m", "i"); putMora("む", "m", "ɯ"); putMora("め", "m", "e"); putMora("も", "m", "o");
        // Y-row
        putMora("や", "j", "a"); putMora("ゆ", "j", "ɯ"); putMora("よ", "j", "o");
        // R-row
        putMora("ら", "ɾ", "a"); putMora("り", "ɾ", "i"); putMora("る", "ɾ", "ɯ"); putMora("れ", "ɾ", "e"); putMora("ろ", "ɾ", "o");
        // W-row
        putMora("わ", "w", "a"); putMora("を", "o");
        // Moraic nasal
        putMora("ん", "n");

        // Yoon compounds (2-character contractions)
        putYoon("きゃ", "k", "j", "a"); putYoon("きゅ", "k", "j", "ɯ"); putYoon("きょ", "k", "j", "o");
        putYoon("ぎゃ", "ɡ", "j", "a"); putYoon("ぎゅ", "ɡ", "j", "ɯ"); putYoon("ぎょ", "ɡ", "j", "o");
        putYoon("しゃ", "ɕ", "a");     putYoon("しゅ", "ɕ", "ɯ");     putYoon("しょ", "ɕ", "o");
        putYoon("じゃ", "ʑ", "a");     putYoon("じゅ", "ʑ", "ɯ");     putYoon("じょ", "ʑ", "o");
        putYoon("ちゃ", "t", "ɕ", "a"); putYoon("ちゅ", "t", "ɕ", "ɯ"); putYoon("ちょ", "t", "ɕ", "o");
        putYoon("にゃ", "ɲ", "a");     putYoon("にゅ", "ɲ", "ɯ");     putYoon("にょ", "ɲ", "o");
        putYoon("ひゃ", "ç", "a");     putYoon("ひゅ", "ç", "ɯ");     putYoon("ひょ", "ç", "o");
        putYoon("びゃ", "b", "j", "a"); putYoon("びゅ", "b", "j", "ɯ"); putYoon("びょ", "b", "j", "o");
        putYoon("ぴゃ", "p", "j", "a"); putYoon("ぴゅ", "p", "j", "ɯ"); putYoon("ぴょ", "p", "j", "o");
        putYoon("みゃ", "m", "j", "a"); putYoon("みゅ", "m", "j", "ɯ"); putYoon("みょ", "m", "j", "o");
        putYoon("りゃ", "ɾ", "j", "a"); putYoon("りゅ", "ɾ", "j", "ɯ"); putYoon("りょ", "ɾ", "j", "o");

        // Katakana loanword Yoon compounds
        putYoon("ふぁ", "ɸ", "a"); putYoon("ふぃ", "ɸ", "i"); putYoon("ふぇ", "ɸ", "e"); putYoon("ふぉ", "ɸ", "o");
        putMora("てぃ", "t", "i"); putMora("でぃ", "d", "i"); putMora("でゅ", "d", "j", "ɯ");
        putYoon("うぃ", "w", "i"); putYoon("うぇ", "w", "e"); putYoon("うぉ", "w", "o");
    }

    private static void putMora(String kana, String... phonemes) {
        MORA_MAP.put(kana, phonemes);
    }

    private static void putYoon(String kana, String... phonemes) {
        YOON_MAP.put(kana, phonemes);
    }

    private TtsKanaPhonemizer() {}

    /**
     * Converts Katakana characters in text to Hiragana.
     */
    public static String katakanaToHiragana(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x30A1 && c <= 0x30F6) {
                sb.append((char) (c - 0x60));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Converts Japanese Kana text into an ordered list of IPA phonemes.
     */
    public static List<String> kanaToPhonemes(String kanaText) {
        List<String> phonemes = new ArrayList<>();
        if (kanaText == null || kanaText.trim().isEmpty()) {
            return phonemes;
        }

        String hira = katakanaToHiragana(kanaText.trim());
        // Handle common greeting particle shifts
        hira = hira.replace("こんにちは", "こんにちわ");
        hira = hira.replace("こんばんは", "こんばんわ");

        int len = hira.length();
        int i = 0;

        while (i < len) {
            char c = hira.charAt(i);

            // Punctuation
            if (c == '、' || c == ',' || c == '，') {
                phonemes.add(",");
                i++;
                continue;
            }
            if (c == '。' || c == '.' || c == '！' || c == '!' || c == '？' || c == '?') {
                phonemes.add(".");
                i++;
                continue;
            }
            if (c == ' ' || c == '　' || c == '\t' || c == '\n') {
                phonemes.add(" ");
                i++;
                continue;
            }

            // Chouonpu (ー)
            if (c == 'ー') {
                phonemes.add("ː");
                i++;
                continue;
            }

            // Sokuon (っ) gemination
            if (c == 'っ') {
                if (i + 1 < len) {
                    String nextPart = (i + 3 <= len && YOON_MAP.containsKey(hira.substring(i + 1, i + 3)))
                            ? hira.substring(i + 1, i + 3)
                            : String.valueOf(hira.charAt(i + 1));
                    String[] tokens = YOON_MAP.get(nextPart);
                    if (tokens == null) {
                        tokens = MORA_MAP.get(nextPart);
                    }
                    if (tokens != null && tokens.length > 0) {
                        phonemes.add(tokens[0]); // Duplicate leading consonant
                    }
                }
                i++;
                continue;
            }

            // 2-character Yoon compound check
            if (i + 1 < len) {
                String pair = hira.substring(i, i + 2);
                String[] tokens = YOON_MAP.get(pair);
                if (tokens != null) {
                    for (String t : tokens) {
                        phonemes.add(t);
                    }
                    i += 2;
                    continue;
                }
            }

            // 1-character Mora check
            String single = String.valueOf(c);
            String[] tokens = MORA_MAP.get(single);
            if (tokens != null) {
                for (String t : tokens) {
                    phonemes.add(t);
                }
                i++;
                continue;
            }

            // Fallback for roman letters, numbers, etc.
            phonemes.add(single);
            i++;
        }

        return phonemes;
    }
}
