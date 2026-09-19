import SwiftUI

public struct SettingsView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = SettingsViewModel()

    public init() {}

    public var body: some View {
        NavigationStack {
            Form {
                // Section: Tampilan
                Section(header: Text("Tampilan")) {
                    Picker("Tema Aplikasi", selection: $appState.currentTheme) {
                        ForEach(AppTheme.allCases) { theme in
                            Text(theme.rawValue).tag(theme.rawValue)
                        }
                    }
                    .pickerStyle(.segmented)

                    Picker("Sisi Depan Kartu", selection: $appState.frontModeRaw) {
                        ForEach(CardFrontMode.allCases) { mode in
                            Text(mode.rawValue).tag(mode.rawValue)
                        }
                    }

                    Toggle("Tampilkan Furigana", isOn: $appState.showFurigana)
                    Toggle("Tampilkan Romaji", isOn: $appState.showRomaji)
                }

                // Section: Suara (TTS)
                Section(header: Text("Pengaturan Suara (AVFoundation)"), footer: Text("Menggunakan suara asisten wanita Jepang native bawaan iOS.")) {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Kecepatan")
                            Spacer()
                            Text(String(format: "%.2fx", appState.speechSpeed))
                                .foregroundColor(.secondary)
                        }
                        Slider(
                            value: $appState.speechSpeed,
                            in: 0.50...1.30,
                            step: 0.05
                        ) { _ in
                            appState.updateAudioSettings(speed: appState.speechSpeed, pitch: appState.speechPitch)
                        }
                    }
                    .padding(.vertical, 4)

                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Tinggi Nada (Pitch)")
                            Spacer()
                            Text(String(format: "%.2fx", appState.speechPitch))
                                .foregroundColor(.secondary)
                        }
                        Slider(
                            value: $appState.speechPitch,
                            in: 0.80...1.20,
                            step: 0.05
                        ) { _ in
                            appState.updateAudioSettings(speed: appState.speechSpeed, pitch: appState.speechPitch)
                        }
                    }
                    .padding(.vertical, 4)

                    Button(action: {
                        viewModel.testSpeech()
                    }) {
                        HStack {
                            Image(systemName: "speaker.wave.2.fill")
                            Text("Tes Suara Jepang")
                        }
                        .foregroundColor(.accentColor)
                    }
                }

                // Section: Database & Reset
                Section(header: Text("Data & Kemajuan")) {
                    HStack {
                        Text("Total Kosakata & Kanji")
                        Spacer()
                        Text("4.363")
                            .foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Kurikulum Bab 1–25")
                        Spacer()
                        Text("863 kata")
                            .foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Kanji Formal")
                        Spacer()
                        Text("613 kanji")
                            .foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Kanji Tambahan")
                        Spacer()
                        Text("2.119 kanji")
                            .foregroundColor(.secondary)
                    }

                    Button(role: .destructive, action: {
                        viewModel.showResetConfirmation = true
                    }) {
                        HStack {
                            Image(systemName: "trash")
                            Text("Reset Semua Kemajuan Belajar")
                        }
                    }
                }

                // Section: Tentang
                Section(header: Text("Tentang Aplikasi")) {
                    HStack {
                        Text("Aplikasi")
                        Spacer()
                        Text("KOTOBAAN (iOS)")
                            .foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Versi")
                        Spacer()
                        Text("2.0.0 (SwiftUI Native)")
                            .foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Basis Referensi")
                        Spacer()
                        Text("KOTOBAAN (v2.0.0)")
                            .foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Mode Suara")
                        Spacer()
                        Text("Offline Native (0 MB model)")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("Pengaturan")
            .alert("Reset Kemajuan Belajar?", isPresented: $viewModel.showResetConfirmation) {
                Button("Batal", role: .cancel) {}
                Button("Reset Sekarang", role: .destructive) {
                    viewModel.resetProgress()
                }
            } message: {
                Text("Semua riwayat hafalan, repetisi SRS, dan status penguasaan akan dikembalikan ke keadaan semula.")
            }
        }
    }
}
