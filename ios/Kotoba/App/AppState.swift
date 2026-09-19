import Foundation
import SwiftUI

public enum AppTheme: String, CaseIterable, Identifiable {
    case system = "Sistem"
    case light = "Terang"
    case dark = "Gelap"

    public var id: String { rawValue }

    public var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

public enum TabItem: Int, CaseIterable, Identifiable {
    case belajar = 0
    case pustaka = 1
    case review = 2
    case pengaturan = 3

    public var id: Int { rawValue }

    public var title: String {
        switch self {
        case .belajar: return "Belajar"
        case .pustaka: return "Pustaka"
        case .review: return "Review"
        case .pengaturan: return "Pengaturan"
        }
    }

    public var iconName: String {
        switch self {
        case .belajar: return "book.fill"
        case .pustaka: return "books.vertical.fill"
        case .review: return "arrow.triangle.2.circlepath"
        case .pengaturan: return "gearshape.fill"
        }
    }
}

public final class AppState: ObservableObject {
    public static let shared = AppState()

    @Published public var selectedTab: TabItem = .belajar
    @AppStorage("app_theme") public var currentTheme: String = AppTheme.system.rawValue
    @AppStorage("show_furigana") public var showFurigana: Bool = true
    @AppStorage("show_romaji") public var showRomaji: Bool = true
    @AppStorage("speech_speed") public var speechSpeed: Double = 0.90
    @AppStorage("speech_pitch") public var speechPitch: Double = 0.95

    @Published public var activeBab: Int = 1
    @Published public var activeDeckTitle: String = "Bab 1"

    public var theme: AppTheme {
        get { AppTheme(rawValue: currentTheme) ?? .system }
        set { currentTheme = newValue.rawValue }
    }

    private init() {
        SpeechService.shared.speechRate = Float(speechSpeed)
        SpeechService.shared.speechPitch = Float(speechPitch)
    }

    public func updateAudioSettings(speed: Double, pitch: Double) {
        speechSpeed = speed
        speechPitch = pitch
        SpeechService.shared.speechRate = Float(speed)
        SpeechService.shared.speechPitch = Float(pitch)
    }
}
