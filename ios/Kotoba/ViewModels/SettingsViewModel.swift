import Foundation
import SwiftUI

public final class SettingsViewModel: ObservableObject {
    @Published public var showResetConfirmation: Bool = false
    @Published public var resetSuccessToast: Bool = false

    public init() {}

    public func resetProgress() {
        DatabaseService.shared.resetAllProgress()
        resetSuccessToast = true
        NotificationCenter.default.post(name: NSNotification.Name("ProgressDidReset"), object: nil)
    }

    public func testSpeech() {
        SpeechService.shared.speak("こんにちは。日本語の勉強を始めましょう。")
    }
}
