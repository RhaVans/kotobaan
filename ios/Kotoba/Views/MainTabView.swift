import SwiftUI

public struct MainTabView: View {
    @EnvironmentObject private var appState: AppState

    public init() {}

    public var body: some View {
        TabView(selection: $appState.selectedTab) {
            FlashcardView()
                .tabItem {
                    Label(TabItem.belajar.title, systemImage: TabItem.belajar.iconName)
                }
                .tag(TabItem.belajar)

            PustakaView()
                .tabItem {
                    Label(TabItem.pustaka.title, systemImage: TabItem.pustaka.iconName)
                }
                .tag(TabItem.pustaka)

            ReviewView()
                .tabItem {
                    Label(TabItem.review.title, systemImage: TabItem.review.iconName)
                }
                .tag(TabItem.review)

            SettingsView()
                .tabItem {
                    Label(TabItem.pengaturan.title, systemImage: TabItem.pengaturan.iconName)
                }
                .tag(TabItem.pengaturan)
        }
    }
}
