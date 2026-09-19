import Foundation
import SQLite3

public final class DatabaseService {
    public static let shared = DatabaseService()

    private var db: OpaquePointer?
    private let dbQueue = DispatchQueue(label: "com.kotoba.databaseQueue", qos: .userInitiated)

    private init() {
        setupDatabase()
    }

    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }

    private func getDatabaseURL() -> URL {
        let fileManager = FileManager.default
        let appSupport = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        try? fileManager.createDirectory(at: appSupport, withIntermediateDirectories: true)
        return appSupport.appendingPathComponent("kotoba.db")
    }

    private func setupDatabase() {
        let destURL = getDatabaseURL()
        let fileManager = FileManager.default

        var needsDeploy = false
        if !fileManager.fileExists(atPath: destURL.path) {
            needsDeploy = true
        } else {
            // Check if existing database is valid and contains full dataset
            var testDb: OpaquePointer?
            if sqlite3_open_v2(destURL.path, &testDb, SQLITE_OPEN_READONLY, nil) == SQLITE_OK {
                var stmt: OpaquePointer?
                if sqlite3_prepare_v2(testDb, "SELECT count(*) FROM learning_objects", -1, &stmt, nil) == SQLITE_OK {
                    if sqlite3_step(stmt) == SQLITE_ROW {
                        let count = sqlite3_column_int(stmt, 0)
                        if count < 4363 {
                            needsDeploy = true
                        }
                    } else {
                        needsDeploy = true
                    }
                    sqlite3_finalize(stmt)
                } else {
                    needsDeploy = true
                }
                sqlite3_close(testDb)
            } else {
                needsDeploy = true
            }
        }

        if needsDeploy {
            deployAssetDatabase(to: destURL)
        }

        if sqlite3_open_v2(destURL.path, &db, SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE, nil) != SQLITE_OK {
            print("Failed to open SQLite database at \(destURL.path)")
        }
    }

    private func deployAssetDatabase(to destURL: URL) {
        let fileManager = FileManager.default
        guard let bundleURL = Bundle.main.url(forResource: "kotoba", withExtension: "db") else {
            print("Warning: kotoba.db not found in main bundle, checking fallback locations")
            return
        }

        if fileManager.fileExists(atPath: destURL.path) {
            try? fileManager.removeItem(at: destURL)
        }

        do {
            try fileManager.copyItem(at: bundleURL, to: destURL)
            print("Successfully deployed kotoba.db to \(destURL.path)")
        } catch {
            print("Error deploying database: \(error)")
        }
    }

    // MARK: - Column extraction helpers

    private func textColumn(_ stmt: OpaquePointer?, index: Int32) -> String {
        guard let cStr = sqlite3_column_text(stmt, index) else { return "" }
        return String(cString: cStr)
    }

    private func optionalTextColumn(_ stmt: OpaquePointer?, index: Int32) -> String? {
        guard sqlite3_column_type(stmt, index) != SQLITE_NULL else { return nil }
        guard let cStr = sqlite3_column_text(stmt, index) else { return nil }
        return String(cString: cStr)
    }

    private func optionalIntColumn(_ stmt: OpaquePointer?, index: Int32) -> Int? {
        guard sqlite3_column_type(stmt, index) != SQLITE_NULL else { return nil }
        return Int(sqlite3_column_int(stmt, index))
    }

    private func parseLearningObject(from stmt: OpaquePointer?) -> LearningObject {
        let id = textColumn(stmt, index: 0)
        let typeStr = textColumn(stmt, index: 1)
        let bab = optionalIntColumn(stmt, index: 2)
        let level = textColumn(stmt, index: 3)
        let japanese = textColumn(stmt, index: 4)
        let reading = textColumn(stmt, index: 5)
        let romaji = textColumn(stmt, index: 6)
        let indonesian = textColumn(stmt, index: 7)
        let badgeLabel = textColumn(stmt, index: 8)
        let groupLabel = textColumn(stmt, index: 9)
        let sortOrder = Int(sqlite3_column_int(stmt, index: 10))
        let detailsJson = optionalTextColumn(stmt, index: 11)
        let wordTypeStr = textColumn(stmt, index: 12)

        let type = LearningObjectType(rawValue: typeStr) ?? .vocabulary
        let wordType = WordType.fromString(wordTypeStr)

        return LearningObject(
            id: id,
            type: type,
            wordType: wordType,
            bab: bab,
            level: level,
            japanese: japanese,
            reading: reading,
            romaji: romaji,
            indonesian: indonesian,
            badgeLabel: badgeLabel,
            groupLabel: groupLabel,
            sortOrder: sortOrder,
            detailsJson: detailsJson
        )
    }

    private let loColumns = "id, type, bab, level, japanese, reading, romaji, indonesian, badge_label, group_label, sort_order, details_json, word_type"

    // MARK: - Chapter Queries

    public func getAllChapters() -> [Chapter] {
        return dbQueue.sync {
            var result: [Chapter] = []
            let sql = "SELECT bab_number, title_ja, title_id, theme, level, vocab_count, grammar_count FROM chapters ORDER BY bab_number ASC"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    let babNumber = Int(sqlite3_column_int(stmt, 0))
                    let titleJa = textColumn(stmt, index: 1)
                    let titleId = textColumn(stmt, index: 2)
                    let theme = textColumn(stmt, index: 3)
                    let level = textColumn(stmt, index: 4)
                    let vocabCount = Int(sqlite3_column_int(stmt, 5))
                    let grammarCount = Int(sqlite3_column_int(stmt, 6))

                    result.append(Chapter(
                        babNumber: babNumber,
                        titleJa: titleJa,
                        titleId: titleId,
                        theme: theme,
                        level: level,
                        vocabCount: vocabCount,
                        grammarCount: grammarCount
                    ))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    public func getChapter(bab: Int) -> Chapter? {
        return dbQueue.sync {
            let sql = "SELECT bab_number, title_ja, title_id, theme, level, vocab_count, grammar_count FROM chapters WHERE bab_number = ?"
            var stmt: OpaquePointer?
            var chapter: Chapter? = nil
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_int(stmt, 1, Int32(bab))
                if sqlite3_step(stmt) == SQLITE_ROW {
                    chapter = Chapter(
                        babNumber: Int(sqlite3_column_int(stmt, 0)),
                        titleJa: textColumn(stmt, index: 1),
                        titleId: textColumn(stmt, index: 2),
                        theme: textColumn(stmt, index: 3),
                        level: textColumn(stmt, index: 4),
                        vocabCount: Int(sqlite3_column_int(stmt, 5)),
                        grammarCount: Int(sqlite3_column_int(stmt, 6))
                    )
                }
                sqlite3_finalize(stmt)
            }
            return chapter
        }
    }

    // MARK: - Vocabulary Queries

    public func getVocabularyForBab(_ bab: Int) -> [LearningObject] {
        return dbQueue.sync {
            var result: [LearningObject] = []
            let sql = "SELECT \(loColumns) FROM learning_objects WHERE type = 'VOCABULARY' AND bab = ? ORDER BY sort_order ASC"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_int(stmt, 1, Int32(bab))
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(parseLearningObject(from: stmt))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    public func getVocabularyForBabs(_ babs: [Int]) -> [LearningObject] {
        guard !babs.isEmpty else { return getAllChapterVocabulary() }
        return dbQueue.sync {
            var result: [LearningObject] = []
            let placeholders = Array(repeating: "?", count: babs.count).joined(separator: ",")
            let sql = "SELECT \(loColumns) FROM learning_objects WHERE type = 'VOCABULARY' AND bab IN (\(placeholders)) ORDER BY bab ASC, sort_order ASC"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                for (idx, bab) in babs.enumerated() {
                    sqlite3_bind_int(stmt, Int32(idx + 1), Int32(bab))
                }
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(parseLearningObject(from: stmt))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    public func getAllChapterVocabulary() -> [LearningObject] {
        return dbQueue.sync {
            var result: [LearningObject] = []
            let sql = "SELECT \(loColumns) FROM learning_objects WHERE type = 'VOCABULARY' AND bab > 0 ORDER BY bab ASC, sort_order ASC"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(parseLearningObject(from: stmt))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    // MARK: - Kanji Queries

    public func getFormalKanji() -> [LearningObject] {
        return dbQueue.sync {
            var result: [LearningObject] = []
            let sql = "SELECT \(loColumns) FROM learning_objects WHERE type = 'KANJI' AND id NOT LIKE 'kanji_add_%' ORDER BY sort_order ASC"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(parseLearningObject(from: stmt))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    public func getAdditionalKanji() -> [LearningObject] {
        return dbQueue.sync {
            var result: [LearningObject] = []
            let sql = "SELECT \(loColumns) FROM learning_objects WHERE id LIKE 'kanji_add_%' ORDER BY sort_order ASC"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(parseLearningObject(from: stmt))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    public func getFormalKanjiGroups() -> [String] {
        return dbQueue.sync {
            var result: [String] = []
            let sql = "SELECT group_label FROM learning_objects WHERE type = 'KANJI' AND id NOT LIKE 'kanji_add_%' AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(textColumn(stmt, index: 0))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    public func getAdditionalKanjiGroups() -> [String] {
        return dbQueue.sync {
            var result: [String] = []
            let sql = "SELECT group_label FROM learning_objects WHERE id LIKE 'kanji_add_%' AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(textColumn(stmt, index: 0))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    public func getKanjiByGroup(_ groupLabel: String) -> [LearningObject] {
        return dbQueue.sync {
            var result: [LearningObject] = []
            let sql = "SELECT \(loColumns) FROM learning_objects WHERE type = 'KANJI' AND group_label = ? ORDER BY sort_order ASC"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_text(stmt, 1, (groupLabel as NSString).utf8String, -1, nil)
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(parseLearningObject(from: stmt))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    // MARK: - Verbs & Adjectives Batches

    public func getVerbGroups() -> [String] {
        return dbQueue.sync {
            var result: [String] = []
            let sql = "SELECT group_label FROM learning_objects WHERE word_type = 'KATA_KERJA' AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(textColumn(stmt, index: 0))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    public func getVerbsByGroup(_ groupLabel: String) -> [LearningObject] {
        return dbQueue.sync {
            var result: [LearningObject] = []
            let sql = "SELECT \(loColumns) FROM learning_objects WHERE word_type = 'KATA_KERJA' AND group_label = ? ORDER BY sort_order ASC"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_text(stmt, 1, (groupLabel as NSString).utf8String, -1, nil)
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(parseLearningObject(from: stmt))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    public func getAdjectiveGroups() -> [String] {
        return dbQueue.sync {
            var result: [String] = []
            let sql = "SELECT group_label FROM learning_objects WHERE (word_type = 'KATA_SIFAT_NA' OR word_type = 'KATA_SIFAT_I') AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(textColumn(stmt, index: 0))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    public func getAdjectivesByGroup(_ groupLabel: String) -> [LearningObject] {
        return dbQueue.sync {
            var result: [LearningObject] = []
            let sql = "SELECT \(loColumns) FROM learning_objects WHERE (word_type = 'KATA_SIFAT_NA' OR word_type = 'KATA_SIFAT_I') AND group_label = ? ORDER BY sort_order ASC"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_text(stmt, 1, (groupLabel as NSString).utf8String, -1, nil)
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(parseLearningObject(from: stmt))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    // MARK: - Filter Queries (Pustaka)

    public func queryVocabulary(
        wordType: String? = nil,
        babs: [Int]? = nil,
        batchIndex: Int? = nil,
        batchSize: Int = 50,
        searchQuery: String? = nil,
        onlyWeak: Bool = false
    ) -> [LearningObject] {
        return dbQueue.sync {
            var result: [LearningObject] = []
            var sql = "SELECT lo.id, lo.type, lo.bab, lo.level, lo.japanese, lo.reading, lo.romaji, lo.indonesian, lo.badge_label, lo.group_label, lo.sort_order, lo.details_json, lo.word_type FROM learning_objects lo "

            if onlyWeak {
                sql += "INNER JOIN user_progress up ON lo.id = up.object_id "
            }

            sql += "WHERE lo.type = 'VOCABULARY' "

            if onlyWeak {
                sql += "AND (up.mastery_state = 'WEAK' OR up.lapses > 0) "
            }

            var bindValues: [String] = []

            if let wt = wordType, !wt.isEmpty, !wt.elementsEqual("SEMUA") {
                if wt.caseInsensitiveCompare("KATA_SIFAT") == .orderedSame {
                    sql += "AND lo.word_type IN ('KATA_SIFAT_I', 'KATA_SIFAT_NA') "
                } else {
                    sql += "AND lo.word_type = ? "
                    bindValues.append(wt.uppercased())
                }
            }

            if let babs = babs, !babs.isEmpty {
                let placeholders = Array(repeating: "?", count: babs.count).joined(separator: ",")
                sql += "AND lo.bab IN (\(placeholders)) "
                for b in babs {
                    bindValues.append(String(b))
                }
            }

            if let sq = searchQuery?.trimmingCharacters(in: .whitespacesAndNewlines), !sq.isEmpty {
                let match = "%\(sq)%"
                sql += "AND (lo.japanese LIKE ? OR lo.reading LIKE ? OR lo.romaji LIKE ? OR lo.indonesian LIKE ?) "
                bindValues.append(match)
                bindValues.append(match)
                bindValues.append(match)
                bindValues.append(match)
            }

            if let babs = babs, !babs.isEmpty {
                sql += "ORDER BY lo.bab ASC, lo.sort_order ASC "
            } else {
                sql += "ORDER BY CASE WHEN lo.id LIKE 'verb_jft_%' OR lo.id LIKE 'adj_jft_%' THEN 0 ELSE 1 END, lo.bab ASC, lo.sort_order ASC "
            }

            if let batch = batchIndex, batch > 0 {
                let offset = (batch - 1) * batchSize
                sql += "LIMIT \(batchSize) OFFSET \(offset)"
            }

            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                for (idx, val) in bindValues.enumerated() {
                    sqlite3_bind_text(stmt, Int32(idx + 1), (val as NSString).utf8String, -1, nil)
                }
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(parseLearningObject(from: stmt))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    public func getWordTypeCount(_ wordType: String?) -> Int {
        return dbQueue.sync {
            var sql = "SELECT COUNT(*) FROM learning_objects WHERE type = 'VOCABULARY'"
            var bindVal: String? = nil

            if let wt = wordType, !wt.isEmpty, !wt.elementsEqual("SEMUA") {
                if wt.caseInsensitiveCompare("KATA_SIFAT") == .orderedSame {
                    sql += " AND word_type IN ('KATA_SIFAT_I', 'KATA_SIFAT_NA')"
                } else {
                    sql += " AND word_type = ?"
                    bindVal = wt.uppercased()
                }
            }

            var count = 0
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                if let val = bindVal {
                    sqlite3_bind_text(stmt, 1, (val as NSString).utf8String, -1, nil)
                }
                if sqlite3_step(stmt) == SQLITE_ROW {
                    count = Int(sqlite3_column_int(stmt, 0))
                }
                sqlite3_finalize(stmt)
            }
            return count
        }
    }

    public func queryKanji(
        subSection: String? = nil,
        groupLabel: String? = nil,
        searchQuery: String? = nil
    ) -> [LearningObject] {
        return dbQueue.sync {
            var result: [LearningObject] = []
            var sql = "SELECT lo.id, lo.type, lo.bab, lo.level, lo.japanese, lo.reading, lo.romaji, lo.indonesian, lo.badge_label, lo.group_label, lo.sort_order, lo.details_json, lo.word_type FROM learning_objects lo WHERE lo.type = 'KANJI' "

            if subSection == "613" {
                sql += "AND lo.id NOT LIKE 'kanji_add_%' "
            } else if subSection == "ADDITIONAL" || subSection == "TAMBAHAN" {
                sql += "AND lo.id LIKE 'kanji_add_%' "
            }

            var bindValues: [String] = []

            if let gl = groupLabel, !gl.isEmpty, !gl.elementsEqual("SEMUA") {
                sql += "AND lo.group_label = ? "
                bindValues.append(gl)
            }

            if let sq = searchQuery?.trimmingCharacters(in: .whitespacesAndNewlines), !sq.isEmpty {
                let match = "%\(sq)%"
                sql += "AND (lo.japanese LIKE ? OR lo.reading LIKE ? OR lo.romaji LIKE ? OR lo.indonesian LIKE ?) "
                bindValues.append(match)
                bindValues.append(match)
                bindValues.append(match)
                bindValues.append(match)
            }

            sql += "ORDER BY lo.sort_order ASC"

            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                for (idx, val) in bindValues.enumerated() {
                    sqlite3_bind_text(stmt, Int32(idx + 1), (val as NSString).utf8String, -1, nil)
                }
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(parseLearningObject(from: stmt))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    // MARK: - User Progress & SRS

    public func getUserProgress(objectId: String) -> UserProgress? {
        return dbQueue.sync {
            let sql = "SELECT object_id, interval_days, ease_factor, repetitions, lapses, stability, retrievability, last_response_time_ms, last_review_epoch, next_review_epoch, mastery_state FROM user_progress WHERE object_id = ?"
            var stmt: OpaquePointer?
            var up: UserProgress? = nil
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_text(stmt, 1, (objectId as NSString).utf8String, -1, nil)
                if sqlite3_step(stmt) == SQLITE_ROW {
                    let objId = textColumn(stmt, index: 0)
                    let intervalDays = Int(sqlite3_column_int(stmt, 1))
                    let easeFactor = sqlite3_column_double(stmt, 2)
                    let repetitions = Int(sqlite3_column_int(stmt, 3))
                    let lapses = Int(sqlite3_column_int(stmt, 4))
                    let stability = sqlite3_column_double(stmt, 5)
                    let retrievability = sqlite3_column_double(stmt, 6)
                    let lastResponse = Int(sqlite3_column_int(stmt, 7))
                    let lastReview = sqlite3_column_int64(stmt, 8)
                    let nextReview = sqlite3_column_int64(stmt, 9)
                    let stateStr = textColumn(stmt, index: 10)
                    let state = UserProgress.MasteryState(rawValue: stateStr) ?? .unseen

                    up = UserProgress(
                        objectId: objId,
                        intervalDays: intervalDays,
                        easeFactor: easeFactor,
                        repetitions: repetitions,
                        lapses: lapses,
                        stability: stability,
                        retrievability: retrievability,
                        lastResponseTimeMs: lastResponse,
                        lastReviewEpoch: lastReview,
                        nextReviewEpoch: nextReview,
                        masteryState: state
                    )
                }
                sqlite3_finalize(stmt)
            }
            return up
        }
    }

    public func saveUserProgress(_ progress: UserProgress) {
        dbQueue.sync {
            let sql = "INSERT OR REPLACE INTO user_progress (object_id, interval_days, ease_factor, repetitions, lapses, stability, retrievability, last_response_time_ms, last_review_epoch, next_review_epoch, mastery_state) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_text(stmt, 1, (progress.objectId as NSString).utf8String, -1, nil)
                sqlite3_bind_int(stmt, 2, Int32(progress.intervalDays))
                sqlite3_bind_double(stmt, 3, progress.easeFactor)
                sqlite3_bind_int(stmt, 4, Int32(progress.repetitions))
                sqlite3_bind_int(stmt, 5, Int32(progress.lapses))
                sqlite3_bind_double(stmt, 6, progress.stability)
                sqlite3_bind_double(stmt, 7, progress.retrievability)
                sqlite3_bind_int(stmt, 8, Int32(progress.lastResponseTimeMs))
                sqlite3_bind_int64(stmt, 9, progress.lastReviewEpoch)
                sqlite3_bind_int64(stmt, 10, progress.nextReviewEpoch)
                sqlite3_bind_text(stmt, 11, (progress.masteryState.rawValue as NSString).utf8String, -1, nil)
                sqlite3_step(stmt)
                sqlite3_finalize(stmt)
            }
        }
    }

    public func getDueReviewItems(currentEpoch: Int64 = Int64(Date().timeIntervalSince1970), limit: Int = 50) -> [LearningObject] {
        return dbQueue.sync {
            var result: [LearningObject] = []
            let sql = "SELECT lo.id, lo.type, lo.bab, lo.level, lo.japanese, lo.reading, lo.romaji, lo.indonesian, lo.badge_label, lo.group_label, lo.sort_order, lo.details_json, lo.word_type FROM learning_objects lo INNER JOIN user_progress up ON lo.id = up.object_id WHERE up.next_review_epoch <= ? AND up.mastery_state != 'UNSEEN' ORDER BY up.next_review_epoch ASC LIMIT ?"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_int64(stmt, 1, currentEpoch)
                sqlite3_bind_int(stmt, 2, Int32(limit))
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(parseLearningObject(from: stmt))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    public func getWeakItems(limit: Int = 50) -> [LearningObject] {
        return dbQueue.sync {
            var result: [LearningObject] = []
            let sql = "SELECT lo.id, lo.type, lo.bab, lo.level, lo.japanese, lo.reading, lo.romaji, lo.indonesian, lo.badge_label, lo.group_label, lo.sort_order, lo.details_json, lo.word_type FROM learning_objects lo INNER JOIN user_progress up ON lo.id = up.object_id WHERE up.mastery_state = 'WEAK' OR up.lapses > 1 ORDER BY up.stability ASC LIMIT ?"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_int(stmt, 1, Int32(limit))
                while sqlite3_step(stmt) == SQLITE_ROW {
                    result.append(parseLearningObject(from: stmt))
                }
                sqlite3_finalize(stmt)
            }
            return result
        }
    }

    public func getMasteryCounts() -> [UserProgress.MasteryState: Int] {
        return dbQueue.sync {
            var map: [UserProgress.MasteryState: Int] = [:]
            for state in UserProgress.MasteryState.allCases {
                map[state] = 0
            }
            let sql = "SELECT mastery_state, count(*) FROM user_progress GROUP BY mastery_state"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    let stateStr = textColumn(stmt, index: 0)
                    if let st = UserProgress.MasteryState(rawValue: stateStr) {
                        map[st] = Int(sqlite3_column_int(stmt, 1))
                    }
                }
                sqlite3_finalize(stmt)
            }
            return map
        }
    }

    public func getChapterCompletion(bab: Int) -> Double {
        return dbQueue.sync {
            let sql = "SELECT count(*), SUM(CASE WHEN up.mastery_state != 'UNSEEN' THEN 1 ELSE 0 END) FROM learning_objects lo LEFT JOIN user_progress up ON lo.id = up.object_id WHERE lo.type = 'VOCABULARY' AND lo.bab = ?"
            var stmt: OpaquePointer?
            var ratio: Double = 0.0
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_int(stmt, 1, Int32(bab))
                if sqlite3_step(stmt) == SQLITE_ROW {
                    let total = sqlite3_column_int(stmt, 0)
                    let done = sqlite3_column_int(stmt, 1)
                    if total > 0 {
                        ratio = Double(done) / Double(total)
                    }
                }
                sqlite3_finalize(stmt)
            }
            return ratio
        }
    }

    // MARK: - Reset Progress

    public func resetAllProgress() {
        dbQueue.sync {
            let sql = "UPDATE user_progress SET interval_days = 0, ease_factor = 2.5, repetitions = 0, lapses = 0, stability = 0.0, retrievability = 1.0, last_response_time_ms = 0, last_review_epoch = 0, next_review_epoch = 0, mastery_state = 'UNSEEN'"
            sqlite3_exec(db, sql, nil, nil, nil)
        }
    }
}
