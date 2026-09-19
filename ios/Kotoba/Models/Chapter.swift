import Foundation

public struct Chapter: Identifiable, Hashable, Codable {
    public var id: Int { babNumber }
    public let babNumber: Int
    public let titleJa: String
    public let titleId: String
    public let theme: String
    public let level: String
    public let vocabCount: Int
    public let grammarCount: Int

    public init(
        babNumber: Int,
        titleJa: String,
        titleId: String,
        theme: String,
        level: String = "N5",
        vocabCount: Int = 0,
        grammarCount: Int = 0
    ) {
        self.babNumber = babNumber
        self.titleJa = titleJa
        self.titleId = titleId
        self.theme = theme
        self.level = level
        self.vocabCount = vocabCount
        self.grammarCount = grammarCount
    }

    public var displayLabel: String {
        return "Bab \(babNumber): \(titleJa)"
    }
}
