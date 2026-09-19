import SwiftUI

@main
public struct KotobaApp: App {
    @StateObject private var appState = AppState.shared

    public init() {}

    public var body: some Scene {
        WindowGroup {
            MainTabView()
                .environmentObject(appState)
                .preferredColorScheme(appState.theme.colorScheme)
        }
    }
}
