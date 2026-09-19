import Foundation

public struct UserProgress: Identifiable, Hashable, Codable {
    public enum MasteryState: String, CaseIterable, Identifiable, Codable {
        case unseen = "UNSEEN"
        case learning = "LEARNING"
        case weak = "WEAK"
        case stable = "STABLE"
        case mastered = "MASTERED"

        public var id: String { rawValue }

        public var symbol: String {
            switch self {
            case .unseen: return "○"
            case .learning: return "▲"
            case .weak: return "▼"
            case .stable: return "●"
            case .mastered: return "◆"
            }
        }

        public var label: String {
            switch self {
            case .unseen: return "Baru"
            case .learning: return "Belajar"
            case .weak: return "Lemah"
            case .stable: return "Stabil"
            case .mastered: return "Terkuasai"
            }
        }

        public var formatted: String {
            return "\(symbol) \(label)"
        }
    }

    public var id: String { objectId }
    public let objectId: String
    public var intervalDays: Int
    public var easeFactor: Double
    public var repetitions: Int
    public var lapses: Int
    public var stability: Double
    public var retrievability: Double
    public var lastResponseTimeMs: Int
    public var lastReviewEpoch: Int64
    public var nextReviewEpoch: Int64
    public var masteryState: MasteryState

    public init(
        objectId: String,
        intervalDays: Int = 0,
        easeFactor: Double = 2.5,
        repetitions: Int = 0,
        lapses: Int = 0,
        stability: Double = 0.0,
        retrievability: Double = 1.0,
        lastResponseTimeMs: Int = 0,
        lastReviewEpoch: Int64 = 0,
        nextReviewEpoch: Int64 = 0,
        masteryState: MasteryState = .unseen
    ) {
        self.objectId = objectId
        self.intervalDays = intervalDays
        self.easeFactor = easeFactor
        self.repetitions = repetitions
        self.lapses = lapses
        self.stability = stability
        self.retrievability = retrievability
        self.lastResponseTimeMs = lastResponseTimeMs
        self.lastReviewEpoch = lastReviewEpoch
        self.nextReviewEpoch = nextReviewEpoch
        self.masteryState = masteryState
    }

    public static func createDefault(objectId: String) -> UserProgress {
        return UserProgress(objectId: objectId)
    }
}
