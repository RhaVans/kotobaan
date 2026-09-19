import Foundation
import SwiftUI
import UIKit

public final class FlashcardViewModel: ObservableObject {
    @Published public var engine: IngatLupaEngine?
    @Published public var isFlipped: Bool = false
    @Published public var cardOffset: CGSize = .zero
    @Published public var showSummarySheet: Bool = false
    @Published public var availableChapters: [Chapter] = []
    @Published public var selectedBab: Int = 1
    @Published public var deckTitle: String = "Bab 1"

    private var cardStartTime: Date = Date()
    private let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
    private let successFeedback = UINotificationFeedbackGenerator()

    public init() {
        impactFeedback.prepare()
        successFeedback.prepare()
        loadChapters()
        loadDeck(forBab: 1)
    }

    public func loadChapters() {
        self.availableChapters = DatabaseService.shared.getAllChapters()
    }

    public func loadDeck(forBab bab: Int) {
        selectedBab = bab
        deckTitle = "Bab \(bab)"
        let items = DatabaseService.shared.getVocabularyForBab(bab)
        startEngine(with: items)
    }

    public func loadMultiBab(babs: [Int]) {
        if babs.count == 1, let single = babs.first {
            loadDeck(forBab: single)
            return
        }
        let sorted = babs.sorted()
        deckTitle = "Bab " + sorted.map(String.init).joined(separator: ", ")
        let items = DatabaseService.shared.getVocabularyForBabs(sorted)
        startEngine(with: items)
    }

    public func loadCustomPool(items: [LearningObject], title: String) {
        deckTitle = title
        startEngine(with: items)
    }

    private func startEngine(with items: [LearningObject]) {
        self.engine = IngatLupaEngine(pool: items)
        self.isFlipped = false
        self.cardOffset = .zero
        self.showSummarySheet = false
        self.cardStartTime = Date()
        playCurrentAudio()
    }

    public func flipCard() {
        withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
            isFlipped.toggle()
        }
        impactFeedback.impactOccurred(intensity: 0.6)
    }

    public func playCurrentAudio() {
        if isFlipped {
            playBackAudio()
        } else {
            playFrontAudio()
        }
    }

    public func playFrontAudio() {
        guard let item = engine?.currentItem else { return }
        let targetText = item.reading.isEmpty ? item.japanese : item.reading
        let target: PronunciationTarget = item.isKatakana ? .katakana : .reading
        SpeechService.shared.speak(
            targetText,
            target: target,
            displayedText: targetText,
            cardMode: "KANJI_FRONT",
            itemId: item.id
        )
    }

    public func playBackAudio() {
        guard let item = engine?.currentItem else { return }
        let targetText = item.reading.isEmpty ? item.japanese : item.reading
        let target: PronunciationTarget = item.isKatakana ? .katakana : .reading
        SpeechService.shared.speak(
            targetText,
            target: target,
            displayedText: targetText,
            cardMode: "KANJI_BACK",
            itemId: item.id
        )
    }

    public func handleIngat() {
        guard let engine = engine, let current = engine.currentItem else { return }
        let latencyMs = Int(Date().timeIntervalSince(cardStartTime) * 1000)

        // Save progress using SRS
        let existingProgress = DatabaseService.shared.getUserProgress(objectId: current.id) ?? UserProgress.createDefault(objectId: current.id)
        let updated = SrsScheduler.scheduleReview(current: existingProgress, isCorrect: true, responseTimeMs: latencyMs)
        DatabaseService.shared.saveUserProgress(updated)

        impactFeedback.impactOccurred(intensity: 0.8)

        withAnimation(.easeOut(duration: 0.25)) {
            cardOffset = CGSize(width: 400, height: 0)
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) { [weak self] in
            guard let self = self else { return }
            let completed = engine.markIngat(responseTimeMs: latencyMs)
            self.cardOffset = .zero
            self.isFlipped = false
            self.cardStartTime = Date()

            if completed {
                self.successFeedback.notificationOccurred(.success)
                self.showSummarySheet = true
            } else {
                self.playCurrentAudio()
            }
        }
    }

    public func handleLupa() {
        guard let engine = engine, let current = engine.currentItem else { return }
        let latencyMs = Int(Date().timeIntervalSince(cardStartTime) * 1000)

        // Save progress using SRS
        let existingProgress = DatabaseService.shared.getUserProgress(objectId: current.id) ?? UserProgress.createDefault(objectId: current.id)
        let updated = SrsScheduler.scheduleReview(current: existingProgress, isCorrect: false, responseTimeMs: latencyMs)
        DatabaseService.shared.saveUserProgress(updated)

        impactFeedback.impactOccurred(intensity: 1.0)

        withAnimation(.easeOut(duration: 0.25)) {
            cardOffset = CGSize(width: -400, height: 0)
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) { [weak self] in
            guard let self = self else { return }
            let completed = engine.markLupa(responseTimeMs: latencyMs)
            self.cardOffset = .zero
            self.isFlipped = false
            self.cardStartTime = Date()

            if completed {
                self.showSummarySheet = true
            } else {
                self.playCurrentAudio()
            }
        }
    }

    public func handlePrev() {
        guard let engine = engine, !engine.isComplete else { return }
        guard engine.currentIndex > 0 else { return } // already at first card — silent no-op

        impactFeedback.impactOccurred(intensity: 0.5)

        withAnimation(.easeOut(duration: 0.2)) {
            cardOffset = CGSize(width: 200, height: 0)
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            guard let self = self else { return }
            engine.goBack()
            self.cardOffset = .zero
            self.isFlipped = false
            self.cardStartTime = Date()
            self.playCurrentAudio()
        }
    }

    public func restartSession() {
        guard let engine = engine else { return }
        engine.restart()
        isFlipped = false
        cardOffset = .zero
        showSummarySheet = false
        cardStartTime = Date()
        playCurrentAudio()
    }
}
