import Foundation

/// Explicit pronunciation target specifying the semantic Japanese representation
/// intended for Text-To-Speech synthesis.
public enum PronunciationTarget: String, CaseIterable, Codable {
    /// Primary kanji or written ideograph form (e.g. 食べる).
    case kanji = "KANJI"

    /// Phonetic kana reading (e.g. たべる).
    case reading = "READING"

    /// Katakana loanword or foreign representation (e.g. コーヒー).
    case katakana = "KATAKANA"

    /// Complete sentence or grammar formula.
    case sentence = "SENTENCE"

    /// Dictionary entry pronunciation (reading if available, else written form).
    case dictionary = "DICTIONARY"

    /// Custom phrase or user test.
    case custom = "CUSTOM"
}
