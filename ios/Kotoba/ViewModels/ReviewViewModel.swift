import Foundation
import SwiftUI

public final class ReviewViewModel: ObservableObject {
    @Published public var dueItems: [LearningObject] = []
    @Published public var weakItems: [LearningObject] = []
    @Published public var masteryCounts: [UserProgress.MasteryState: Int] = [:]
    @Published public var totalStudied: Int = 0
    @Published public var isLoading: Bool = false

    public init() {
        refresh()
    }

    public func refresh() {
        isLoading = true
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }

            let due = DatabaseService.shared.getDueReviewItems(limit: 50)
            let weak = DatabaseService.shared.getWeakItems(limit: 50)
            let counts = DatabaseService.shared.getMasteryCounts()

            let nonUnseen = counts.filter { $0.key != .unseen }.values.reduce(0, +)

            DispatchQueue.main.async {
                self.dueItems = due
                self.weakItems = weak
                self.masteryCounts = counts
                self.totalStudied = nonUnseen
                self.isLoading = false
            }
        }
    }

    public func getCount(for state: UserProgress.MasteryState) -> Int {
        return masteryCounts[state] ?? 0
    }
}
