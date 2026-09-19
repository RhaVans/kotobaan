import SwiftUI

public struct ReviewView: View {
    @StateObject private var viewModel = ReviewViewModel()
    @State private var activeReviewDeck: [LearningObject]? = nil
    @State private var activeReviewTitle: String = ""

    public init() {}

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Mastery Status Overview
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Status Penguasaan")
                            .font(.headline)
                            .foregroundColor(.primary)

                        HStack(spacing: 8) {
                            MasteryBox(
                                symbol: UserProgress.MasteryState.unseen.symbol,
                                label: "Baru",
                                count: viewModel.getCount(for: .unseen),
                                color: .gray
                            )
                            MasteryBox(
                                symbol: UserProgress.MasteryState.learning.symbol,
                                label: "Belajar",
                                count: viewModel.getCount(for: .learning),
                                color: .blue
                            )
                            MasteryBox(
                                symbol: UserProgress.MasteryState.weak.symbol,
                                label: "Lemah",
                                count: viewModel.getCount(for: .weak),
                                color: .red
                            )
                            MasteryBox(
                                symbol: UserProgress.MasteryState.stable.symbol,
                                label: "Stabil",
                                count: viewModel.getCount(for: .stable),
                                color: .orange
                            )
                            MasteryBox(
                                symbol: UserProgress.MasteryState.mastered.symbol,
                                label: "Kuasai",
                                count: viewModel.getCount(for: .mastered),
                                color: .green
                            )
                        }
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(16)
                    .padding(.horizontal, 16)

                    // Scheduled SRS Review Card
                    VStack(alignment: .leading, spacing: 14) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Jadwal Review Hari Ini")
                                    .font(.headline)
                                    .foregroundColor(.primary)
                                Text("Berdasarkan algoritma Leitner 5-box")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Text("\(viewModel.dueItems.count)")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(.accentColor)
                        }

                        Button(action: {
                            if !viewModel.dueItems.isEmpty {
                                activeReviewDeck = viewModel.dueItems
                                activeReviewTitle = "Review Terjadwal"
                            }
                        }) {
                            HStack {
                                Image(systemName: "play.circle.fill")
                                Text(viewModel.dueItems.isEmpty ? "Tidak Ada Kartu Jatuh Tempo" : "Mulai Review (\(viewModel.dueItems.count) Kartu)")
                                    .fontWeight(.semibold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(viewModel.dueItems.isEmpty ? Color.gray.opacity(0.3) : Color.accentColor)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                        .disabled(viewModel.dueItems.isEmpty)
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(16)
                    .padding(.horizontal, 16)

                    // Weak Words Focus Drill Card
                    VStack(alignment: .leading, spacing: 14) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Latihan Kosakata Lemah")
                                    .font(.headline)
                                    .foregroundColor(.primary)
                                Text("Kata dengan riwayat lupa atau ragu")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Text("\(viewModel.weakItems.count)")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(.red)
                        }

                        Button(action: {
                            if !viewModel.weakItems.isEmpty {
                                activeReviewDeck = viewModel.weakItems
                                activeReviewTitle = "Fokus Kata Lemah"
                            }
                        }) {
                            HStack {
                                Image(systemName: "flame.fill")
                                Text(viewModel.weakItems.isEmpty ? "Tidak Ada Kata Lemah" : "Latih Kata Lemah (\(viewModel.weakItems.count) Kartu)")
                                    .fontWeight(.semibold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(viewModel.weakItems.isEmpty ? Color.gray.opacity(0.3) : Color.red)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                        .disabled(viewModel.weakItems.isEmpty)
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(16)
                    .padding(.horizontal, 16)
                }
                .padding(.vertical, 16)
            }
            .navigationTitle("Review")
            .onAppear {
                viewModel.refresh()
            }
            .fullScreenCover(item: Binding<LearningDeckWrapper?>(
                get: { activeReviewDeck != nil ? LearningDeckWrapper(items: activeReviewDeck!, title: activeReviewTitle) : nil },
                set: { if $0 == nil { activeReviewDeck = nil } }
            )) { wrapper in
                ReviewPracticeView(items: wrapper.items, title: wrapper.title) {
                    activeReviewDeck = nil
                    viewModel.refresh()
                }
            }
        }
    }
}

public struct LearningDeckWrapper: Identifiable {
    public let id = UUID()
    public let items: [LearningObject]
    public let title: String
}

public struct MasteryBox: View {
    public let symbol: String
    public let label: String
    public let count: Int
    public let color: Color

    public var body: some View {
        VStack(spacing: 4) {
            Text(symbol)
                .font(.caption)
                .foregroundColor(color)
            Text("\(count)")
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            Text(label)
                .font(.system(size: 10))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(Color(.tertiarySystemBackground))
        .cornerRadius(8)
    }
}

public struct ReviewPracticeView: View {
    public let items: [LearningObject]
    public let title: String
    public let onDismiss: () -> Void

    @StateObject private var viewModel = FlashcardViewModel()
    @EnvironmentObject private var appState: AppState

    public var body: some View {
        NavigationStack {
            VStack {
                if let item = viewModel.engine?.currentItem {
                    CardFaceView(
                        item: item,
                        isBack: viewModel.isFlipped,
                        showFurigana: appState.showFurigana,
                        showRomaji: appState.showRomaji,
                        onSpeakerTap: {
                            viewModel.playCurrentAudio()
                        }
                    )
                    .offset(viewModel.cardOffset)
                    .rotation3DEffect(.degrees(viewModel.isFlipped ? 180 : 0), axis: (x: 0, y: 1, z: 0))
                    .onTapGesture {
                        viewModel.flipCard()
                    }
                    .padding()

                    Spacer()

                    HStack(spacing: 20) {
                        Button(action: { viewModel.handleLupa() }) {
                            Text("Lupa")
                                .fontWeight(.bold)
                                .frame(maxWidth: .infinity)
                                .frame(height: 52)
                                .background(Color.red.opacity(0.12))
                                .foregroundColor(.red)
                                .cornerRadius(14)
                        }

                        Button(action: { viewModel.handleIngat() }) {
                            Text("Ingat")
                                .fontWeight(.bold)
                                .frame(maxWidth: .infinity)
                                .frame(height: 52)
                                .background(Color.green)
                                .foregroundColor(.white)
                                .cornerRadius(14)
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 20)
                } else {
                    VStack(spacing: 16) {
                        Image(systemName: "sparkles")
                            .font(.system(size: 56))
                            .foregroundColor(.accentColor)
                        Text("Sesi Selesai!")
                            .font(.title2)
                            .fontWeight(.bold)
                        Button("Kembali ke Review") {
                            onDismiss()
                        }
                        .fontWeight(.semibold)
                        .padding()
                        .background(Color.accentColor)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Tutup") {
                        onDismiss()
                    }
                }
            }
            .onAppear {
                viewModel.loadCustomPool(items: items, title: title)
            }
        }
    }
}
