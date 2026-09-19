import Foundation

public struct LearningCycle: Identifiable, Hashable, Codable {
    public enum Source: String, CaseIterable, Identifiable, Codable {
        case curriculum = "CURRICULUM"
        case library = "LIBRARY"
        case jftVerb = "JFT_VERB"
        case jftAdj = "JFT_ADJ"
        case kanji = "KANJI"

        public var id: String { rawValue }
    }

    public enum MaterialType: String, CaseIterable, Identifiable, Codable {
        case kotoba = "KOTOBA"
        case kanji = "KANJI"
        case kataKerja = "KATA_KERJA"
        case kataSifat = "KATA_SIFAT"

        public var id: String { rawValue }
    }

    public enum DisplayMode: String, CaseIterable, Identifiable, Codable {
        case kanji = "KANJI"
        case kana = "KANA"
        case indonesia = "INDONESIA"

        public var id: String { rawValue }
    }

    public enum Status: String, CaseIterable, Identifiable, Codable {
        case active = "ACTIVE"
        case completed = "COMPLETED"
        case abandoned = "ABANDONED"

        public var id: String { rawValue }
    }

    public var id: String { cycleId }
    public let cycleId: String
    public let source: Source
    public let materialType: MaterialType
    public let configuredAmount: Int
    public var displayMode: DisplayMode
    public let poolDefinitionJson: String
    public let startedAtEpoch: Int64
    public var completedAtEpoch: Int64
    public var status: Status

    public init(
        cycleId: String,
        source: Source = .curriculum,
        materialType: MaterialType = .kotoba,
        configuredAmount: Int,
        displayMode: DisplayMode = .kanji,
        poolDefinitionJson: String = "",
        startedAtEpoch: Int64 = Int64(Date().timeIntervalSince1970),
        completedAtEpoch: Int64 = 0,
        status: Status = .active
    ) {
        self.cycleId = cycleId
        self.source = source
        self.materialType = materialType
        self.configuredAmount = configuredAmount
        self.displayMode = displayMode
        self.poolDefinitionJson = poolDefinitionJson
        self.startedAtEpoch = startedAtEpoch
        self.completedAtEpoch = completedAtEpoch
        self.status = status
    }
}

public struct CycleItem: Identifiable, Hashable, Codable {
    public enum Response: String, CaseIterable, Identifiable, Codable {
        case pending = "PENDING"
        case ingat = "INGAT"
        case lupa = "LUPA"

        public var id: String { rawValue }
    }

    public enum PoolType: String, CaseIterable, Identifiable, Codable {
        case normal = "NORMAL"
        case recovery = "RECOVERY"

        public var id: String { rawValue }
    }

    public var id: String { "\(cycleId)_\(objectId)_\(cycleIteration)" }
    public let cycleId: String
    public let objectId: String
    public let poolType: PoolType
    public let cycleIteration: Int
    public var response: Response
    public var responseTimeMs: Int
    public var attemptCount: Int

    public init(
        cycleId: String,
        objectId: String,
        poolType: PoolType,
        cycleIteration: Int,
        response: Response = .pending,
        responseTimeMs: Int = 0,
        attemptCount: Int = 0
    ) {
        self.cycleId = cycleId
        self.objectId = objectId
        self.poolType = poolType
        self.cycleIteration = cycleIteration
        self.response = response
        self.responseTimeMs = responseTimeMs
        self.attemptCount = attemptCount
    }
}
