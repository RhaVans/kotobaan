import Foundation

public final class SrsScheduler {
    public static let secondsPerDay: Int64 = 86400
    public static let quarantineSeconds: Int64 = 4 * 3600 // 4 hours

    public static func scheduleReview(
        current: UserProgress,
        isCorrect: Bool,
        responseTimeMs: Int,
        currentEpoch: Int64 = Int64(Date().timeIntervalSince1970)
    ) -> UserProgress {
        var progress = current
        var interval = progress.intervalDays
        var ease = progress.easeFactor
        var reps = progress.repetitions
        var lapses = progress.lapses
        var stability = progress.stability

        let isStruggling = responseTimeMs > 10000
        let isFluent = responseTimeMs >= 800 && responseTimeMs <= 4000

        if isCorrect {
            reps += 1
            if isFluent {
                ease = min(3.0, ease + 0.1)
            } else if isStruggling {
                ease = max(1.3, ease - 0.15)
            }

            if reps == 1 {
                interval = 1
                stability = 1.0
            } else if reps == 2 {
                interval = isStruggling ? 3 : 6
                stability = Double(interval)
            } else {
                var multiplier = ease
                if isStruggling {
                    multiplier = max(1.2, ease * 0.7)
                }
                interval = Int(round(Double(interval) * multiplier))
                stability = Double(interval)
            }

            let nextDue = currentEpoch + (Int64(interval) * secondsPerDay)

            let state: UserProgress.MasteryState
            if reps >= 5 && interval >= 21 {
                state = .mastered
            } else if reps >= 3 && interval >= 7 {
                state = .stable
            } else {
                state = .learning
            }

            progress.intervalDays = interval
            progress.easeFactor = ease
            progress.repetitions = reps
            progress.stability = stability
            progress.retrievability = 1.0
            progress.lastResponseTimeMs = responseTimeMs
            progress.lastReviewEpoch = currentEpoch
            progress.nextReviewEpoch = nextDue
            progress.masteryState = state
        } else {
            lapses += 1
            reps = 0
            interval = 1
            ease = max(1.3, ease - 0.2)
            stability = 0.5

            let nextDue: Int64
            if lapses >= 2 {
                nextDue = currentEpoch + quarantineSeconds
            } else {
                nextDue = currentEpoch + secondsPerDay
            }

            progress.intervalDays = interval
            progress.easeFactor = ease
            progress.repetitions = reps
            progress.lapses = lapses
            progress.stability = stability
            progress.retrievability = 0.0
            progress.lastResponseTimeMs = responseTimeMs
            progress.lastReviewEpoch = currentEpoch
            progress.nextReviewEpoch = nextDue
            progress.masteryState = .weak
        }

        return progress
    }
}
