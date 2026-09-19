import Foundation

public final class IngatLupaEngine: ObservableObject {
    public let initialPool: [LearningObject]

    @Published public private(set) var currentPassItems: [LearningObject]
    @Published public private(set) var currentLupaList: [LearningObject]
    @Published public private(set) var allResolvedItems: [LearningObject]

    @Published public private(set) var currentIndex: Int = 0
    @Published public private(set) var cycleIteration: Int = 1
    @Published public private(set) var isComplete: Bool = false
    @Published public private(set) var totalIngatCount: Int = 0
    @Published public private(set) var totalLupaCount: Int = 0

    public init(pool: [LearningObject]) {
        self.initialPool = pool
        self.currentPassItems = pool
        self.currentLupaList = []
        self.allResolvedItems = []
        self.isComplete = pool.isEmpty
    }

    public var isRecoveryRound: Bool {
        return cycleIteration > 1
    }

    public var currentItem: LearningObject? {
        guard !isComplete, currentIndex < currentPassItems.count else { return nil }
        return currentPassItems[currentIndex]
    }

    public var currentPassTotal: Int {
        return currentPassItems.count
    }

    public var initialPoolSize: Int {
        return initialPool.count
    }

    public var pendingLupaCount: Int {
        return currentLupaList.count
    }

    public var accuracyPercentage: Double {
        let total = totalIngatCount + totalLupaCount
        guard total > 0 else { return 100.0 }
        return (Double(totalIngatCount) / Double(total)) * 100.0
    }

    @discardableResult
    public func markIngat(responseTimeMs: Int = 1000) -> Bool {
        guard !isComplete, let item = currentItem else { return true }

        totalIngatCount += 1
        if !allResolvedItems.contains(where: { $0.id == item.id }) {
            allResolvedItems.append(item)
        }

        currentIndex += 1
        checkEndOfPass()
        return isComplete
    }

    @discardableResult
    public func markLupa(responseTimeMs: Int = 1000) -> Bool {
        guard !isComplete, let item = currentItem else { return true }

        totalLupaCount += 1
        currentLupaList.append(item)

        currentIndex += 1
        checkEndOfPass()
        return isComplete
    }

    private func checkEndOfPass() {
        if currentIndex >= currentPassItems.count {
            if currentLupaList.isEmpty {
                isComplete = true
            } else {
                currentPassItems = currentLupaList
                currentLupaList = []
                cycleIteration += 1
                currentIndex = 0
            }
        }
    }

    /// Navigate back one card within the current pass.
    /// Silent no-op when already at the first card.
    /// Does NOT undo any committed Ingat/Lupa judgment.
    public func goBack() {
        guard currentIndex > 0 else { return }
        currentIndex -= 1
    }

    public func restart() {
        currentPassItems = initialPool
        currentLupaList = []
        allResolvedItems = []
        currentIndex = 0
        cycleIteration = 1
        totalIngatCount = 0
        totalLupaCount = 0
        isComplete = initialPool.isEmpty
    }
}
