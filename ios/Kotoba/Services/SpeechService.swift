import Foundation
import AVFoundation

public final class SpeechService: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    public static let shared = SpeechService()

    private let synthesizer = AVSpeechSynthesizer()
    @Published public private(set) var isSpeaking: Bool = false
    @Published public var speechRate: Float = 0.90
    @Published public var speechPitch: Float = 0.95

    private override init() {
        super.init()
        synthesizer.delegate = self
        configureAudioSession()
    }

    private func configureAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [.duckOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("SpeechService audio session configuration error: \(error)")
        }
    }

    public func speak(
        _ text: String,
        target: PronunciationTarget = .custom,
        displayedText: String = "",
        cardMode: String = "DEFAULT",
        itemId: String = ""
    ) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        print("""
        ==================== [TTS DEBUG] ====================
        DISPLAYED:    \(displayedText.isEmpty ? trimmed : displayedText)
        TTS INPUT:    \(trimmed)
        SOURCE FIELD: \(target.rawValue)
        CARD MODE:    \(cardMode)
        ITEM ID:      \(itemId)
        ====================================================
        """)

        stop()

        let utterance = AVSpeechUtterance(string: trimmed)
        utterance.voice = findBestJapaneseVoice()
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * speechRate
        utterance.pitchMultiplier = speechPitch

        synthesizer.speak(utterance)
    }

    public func stop() {
        synthesizer.stopSpeaking(at: .immediate)
    }

    private func findBestJapaneseVoice() -> AVSpeechSynthesisVoice? {
        let voices = AVSpeechSynthesisVoice.speechVoices().filter { $0.language.hasPrefix("ja") }
        // Prefer enhanced/premium voice if downloaded on device, fallback to standard ja-JP
        if let enhanced = voices.first(where: { $0.quality == .enhanced }) {
            return enhanced
        }
        return AVSpeechSynthesisVoice(language: "ja-JP")
    }

    // MARK: - AVSpeechSynthesizerDelegate

    public func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didStart utterance: AVSpeechUtterance) {
        DispatchQueue.main.async {
            self.isSpeaking = true
        }
    }

    public func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        DispatchQueue.main.async {
            self.isSpeaking = false
        }
    }

    public func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        DispatchQueue.main.async {
            self.isSpeaking = false
        }
    }
}
