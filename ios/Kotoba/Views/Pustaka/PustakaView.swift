import SwiftUI

public struct PustakaView: View {
    @StateObject private var viewModel = PustakaViewModel()
    @State private var showMultiBabSheet: Bool = false

    public init() {}

    public var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Category Chip Bar
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(PustakaCategory.allCases) { category in
                            let isSelected = viewModel.selectedCategory == category
                            Button(action: {
                                viewModel.selectCategory(category)
                            }) {
                                Text(category.rawValue)
                                    .font(.subheadline)
                                    .fontWeight(isSelected ? .semibold : .regular)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(isSelected ? Color.accentColor : Color(.secondarySystemBackground))
                                    .foregroundColor(isSelected ? .white : .primary)
                                    .clipShape(Capsule())
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                }
                .background(Color(.systemBackground))

                // Secondary Filter Toolbar (Multi-Bab or Batch Picker)
                HStack {
                    if viewModel.selectedCategory == .semua || viewModel.selectedCategory == .kataBenda {
                        Button(action: { showMultiBabSheet = true }) {
                            HStack(spacing: 6) {
                                Image(systemName: "line.3.horizontal.decrease.circle")
                                Text(viewModel.selectedBabs.isEmpty ? "Semua Bab" : "\(viewModel.selectedBabs.count) Bab Terpilih")
                                    .font(.caption)
                                    .fontWeight(.medium)
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Color(.secondarySystemBackground))
                            .foregroundColor(.accentColor)
                            .cornerRadius(8)
                        }
                    } else if viewModel.availableBatchGroups.count > 1 {
                        Menu {
                            ForEach(viewModel.availableBatchGroups, id: \.self) { group in
                                Button(group) {
                                    viewModel.selectedBatchGroup = group
                                    viewModel.refresh()
                                }
                            }
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: "square.grid.2x2")
                                Text("Kelompok: \(viewModel.selectedBatchGroup)")
                                    .font(.caption)
                                    .fontWeight(.medium)
                                Image(systemName: "chevron.down")
                                    .font(.caption2)
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Color(.secondarySystemBackground))
                            .foregroundColor(.accentColor)
                            .cornerRadius(8)
                        }
                    }

                    Spacer()

                    Text("\(viewModel.totalCount) entri")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
                .background(Color(.systemBackground))

                Divider()

                // List Content
                if viewModel.isLoading {
                    Spacer()
                    ProgressView("Memuat data...")
                    Spacer()
                } else if viewModel.items.isEmpty {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 40))
                            .foregroundColor(.secondary)
                        Text("Tidak ada data yang cocok")
                            .font(.headline)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                } else {
                    List {
                        ForEach(viewModel.items) { item in
                            PustakaCardRow(item: item) {
                                viewModel.playAudio(for: item)
                            }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Pustaka")
            .searchable(text: $viewModel.searchQuery, prompt: "Cari kanji, kana, romaji, arti...")
            .onChange(of: viewModel.searchQuery) { _ in
                viewModel.refresh()
            }
            .sheet(isPresented: $showMultiBabSheet) {
                MultiBabSheetView(selectedBabs: $viewModel.selectedBabs) {
                    viewModel.refresh()
                }
                .presentationDetents([.medium, .large])
            }
        }
    }
}
