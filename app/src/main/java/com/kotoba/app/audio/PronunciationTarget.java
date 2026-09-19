package com.kotoba.app.audio;

/**
 * Explicit pronunciation target specifying the semantic Japanese representation
 * intended for Text-To-Speech synthesis.
 */
public enum PronunciationTarget {
    /**
     * Primary kanji or written ideograph form (e.g., 食べる).
     */
    KANJI,

    /**
     * Phonetic kana reading (e.g., たべる).
     */
    READING,

    /**
     * Katakana loanword or foreign representation (e.g., コーヒー).
     */
    KATAKANA,

    /**
     * Complete sentence or grammar formula.
     */
    SENTENCE,

    /**
     * Dictionary entry pronunciation (reading if available, else written form).
     */
    DICTIONARY,

    /**
     * Custom phrase or user test.
     */
    CUSTOM
}
