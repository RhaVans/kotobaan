import Foundation

public enum LearningObjectType: String, CaseIterable, Identifiable, Codable {
    case vocabulary = "VOCABULARY"
    case kanji = "KANJI"
    case grammar = "GRAMMAR"

    public var id: String { rawValue }
}

public struct LearningObject: Identifiable, Hashable, Codable {
    public let id: String
    public let type: LearningObjectType
    public let wordType: WordType
    public let bab: Int?
    public let level: String
    public let japanese: String
    public let reading: String
    public let romaji: String
    public let indonesian: String
    public let badgeLabel: String
    public let groupLabel: String
    public let sortOrder: Int
    public let detailsJson: String?

    public init(
        id: String,
        type: LearningObjectType,
        wordType: WordType = .lainnya,
        bab: Int? = nil,
        level: String = "N5",
        japanese: String,
        reading: String,
        romaji: String = "",
        indonesian: String,
        badgeLabel: String = "",
        groupLabel: String = "",
        sortOrder: Int = 0,
        detailsJson: String? = nil
    ) {
        self.id = id
        self.type = type
        self.wordType = wordType
        self.bab = bab
        self.level = level
        self.japanese = japanese
        self.reading = reading
        self.romaji = romaji
        self.indonesian = indonesian
        self.badgeLabel = badgeLabel
        self.groupLabel = groupLabel
        self.sortOrder = sortOrder
        self.detailsJson = detailsJson
    }

    public var hasKanji: Bool {
        // CJK Unified Ideographs (\u4E00-\u9FFF)
        for scalar in japanese.unicodeScalars {
            if scalar.value >= 0x4E00 && scalar.value <= 0x9FFF {
                return true
            }
        }
        return japanese != reading
    }

    public var isKatakana: Bool {
        guard !japanese.isEmpty else { return false }
        for scalar in japanese.unicodeScalars {
            let val = scalar.value
            let isKata = (val >= 0x30A0 && val <= 0x30FF) || (val == 0x30FC) || CharacterSet.whitespaces.contains(scalar)
            if !isKata { return false }
        }
        return true
    }

    public var kanji: String? {
        return hasKanji ? japanese : nil
    }

    public var katakana: String? {
        return isKatakana ? japanese : nil
    }

    public var meaning: String {
        return indonesian
    }

    public var isAdditionalKanji: Bool {
        return id.hasPrefix("kanji_add_")
    }

    public var formula: String {
        return extractJsonString(detailsJson, key: "formula")
    }

    public var explanation: String {
        return extractJsonString(detailsJson, key: "explanation")
    }

    private func extractJsonString(_ json: String?, key: String) -> String {
        guard let json = json, !json.isEmpty else { return "" }
        let needle = "\"\(key)\":"
        guard let range = json.range(of: needle) else { return "" }
        let remainder = json[range.upperBound...]
        let trimmed = remainder.trimmingCharacters(in: .whitespaces)
        guard trimmed.hasPrefix("\"") else { return "" }
        let afterQuote = trimmed.dropFirst()
        var result = ""
        var isEscaped = false
        for char in afterQuote {
            if isEscaped {
                if char == "n" {
                    result.append("\n")
                } else if char == "\"" {
                    result.append("\"")
                } else if char == "\\" {
                    result.append("\\")
                } else {
                    result.append(char)
                }
                isEscaped = false
            } else if char == "\\" {
                isEscaped = true
            } else if char == "\"" {
                break
            } else {
                result.append(char)
            }
        }
        return result
    }
}
