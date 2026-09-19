import Foundation
import SwiftUI

public enum PustakaCategory: String, CaseIterable, Identifiable {
    case semua = "Semua"
    case kataKerja = "Kata Kerja"
    case kataSifat = "Kata Sifat"
    case kataBenda = "Kata Benda"
    case kanji613 = "Kanji (613)"
    case kanjiTambahan = "Tambahan"

    public var id: String { rawValue }
}

public final class PustakaViewModel: ObservableObject {
    @Published public var selectedCategory: PustakaCategory = .semua
    @Published public var searchQuery: String = ""
    @Published public var selectedBabs: Set<Int> = []
    @Published public var selectedBatchGroup: String = "Semua"
    @Published public var availableBatchGroups: [String] = ["Semua"]
    @Published public var items: [LearningObject] = []
    @Published public var isLoading: Bool = false
    @Published public var totalCount: Int = 0

    public init() {
        refresh()
    }

    public func selectCategory(_ category: PustakaCategory) {
        selectedCategory = category
        selectedBatchGroup = "Semua"
        loadBatchGroups()
        refresh()
    }

    public func loadBatchGroups() {
        switch selectedCategory {
        case .kataKerja:
            let groups = DatabaseService.shared.getVerbGroups()
            availableBatchGroups = ["Semua"] + groups
        case .kataSifat:
            let groups = DatabaseService.shared.getAdjectiveGroups()
            availableBatchGroups = ["Semua"] + groups
        case .kanji613:
            let groups = DatabaseService.shared.getFormalKanjiGroups()
            availableBatchGroups = ["Semua"] + groups
        case .kanjiTambahan:
            let groups = DatabaseService.shared.getAdditionalKanjiGroups()
            availableBatchGroups = ["Semua"] + groups
        case .semua, .kataBenda:
            availableBatchGroups = ["Semua"]
        }
    }

    public func refresh() {
        isLoading = true

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }

            let results: [LearningObject]
            switch self.selectedCategory {
            case .semua:
                let babsList = self.selectedBabs.isEmpty ? nil : Array(self.selectedBabs)
                results = DatabaseService.shared.queryVocabulary(
                    wordType: nil,
                    babs: babsList,
                    searchQuery: self.searchQuery.isEmpty ? nil : self.searchQuery
                )
            case .kataKerja:
                if self.selectedBatchGroup != "Semua" {
                    results = DatabaseService.shared.getVerbsByGroup(self.selectedBatchGroup)
                } else {
                    results = DatabaseService.shared.queryVocabulary(
                        wordType: "KATA_KERJA",
                        babs: nil,
                        searchQuery: self.searchQuery.isEmpty ? nil : self.searchQuery
                    )
                }
            case .kataSifat:
                if self.selectedBatchGroup != "Semua" {
                    results = DatabaseService.shared.getAdjectivesByGroup(self.selectedBatchGroup)
                } else {
                    results = DatabaseService.shared.queryVocabulary(
                        wordType: "KATA_SIFAT",
                        babs: nil,
                        searchQuery: self.searchQuery.isEmpty ? nil : self.searchQuery
                    )
                }
            case .kataBenda:
                let babsList = self.selectedBabs.isEmpty ? nil : Array(self.selectedBabs)
                results = DatabaseService.shared.queryVocabulary(
                    wordType: "KATA_BENDA",
                    babs: babsList,
                    searchQuery: self.searchQuery.isEmpty ? nil : self.searchQuery
                )
            case .kanji613:
                results = DatabaseService.shared.queryKanji(
                    subSection: "613",
                    groupLabel: self.selectedBatchGroup == "Semua" ? nil : self.selectedBatchGroup,
                    searchQuery: self.searchQuery.isEmpty ? nil : self.searchQuery
                )
            case .kanjiTambahan:
                results = DatabaseService.shared.queryKanji(
                    subSection: "ADDITIONAL",
                    groupLabel: self.selectedBatchGroup == "Semua" ? nil : self.selectedBatchGroup,
                    searchQuery: self.searchQuery.isEmpty ? nil : self.searchQuery
                )
            }

            DispatchQueue.main.async {
                self.items = results
                self.totalCount = results.count
                self.isLoading = false
            }
        }
    }

    public func playAudio(for item: LearningObject) {
        let text = item.reading.isEmpty ? item.japanese : item.reading
        let target: PronunciationTarget = item.isKatakana ? .katakana : (item.hasKanji ? .reading : .kanji)
        SpeechService.shared.speak(
            text,
            target: target,
            displayedText: item.japanese + (item.hasKanji ? " (\(item.reading))" : ""),
            cardMode: "LIBRARY_LIST",
            itemId: item.id
        )
    }
}
