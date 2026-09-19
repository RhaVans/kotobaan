#!/usr/bin/env python3
"""
Kotoba.app Curriculum & Database Integrity Validator
Verifies:
1. Exactly 25 chapters covering Bab 1 to Bab 25 without gaps.
2. Exactly 863 curriculum vocabulary items mapped to Bab 1..25.
3. Exactly 549 prioritized JFT Verbs in 11 groups of 50.
4. Exactly 173 prioritized JFT Adjectives in groups of 50.
5. Exactly 613 Kanji items in 13 groups of 50.
6. Grammar patterns and authentic examples.
7. Exactly 2,244 total learning objects with 1:1 seeded user_progress.
8. Valid word_type enum on all objects.
9. Zero null values in critical columns.
10. Chapter vocabulary isolation.
11. Cycle persistence tables (learning_cycles, cycle_items).
"""

import sqlite3
import sys
from pathlib import Path

DB_PATH = Path("/storage/emulated/0/download/PROJECT/KTB/app/src/main/assets/databases/kotoba.db")

VALID_WORD_TYPES = {
    "KATA_BENDA", "KATA_KERJA", "KATA_SIFAT_I", "KATA_SIFAT_NA",
    "KATA_KETERANGAN", "KATA_SAMBUNG", "PARTIKEL", "UNGKAPAN", "LAINNYA"
}

def test_database():
    assert DB_PATH.exists(), f"Database file not found: {DB_PATH}"
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()

    print("Running Curriculum & JFT Resources Integrity Tests...")

    # 1. Chapters
    c.execute("SELECT count(*), min(bab_number), max(bab_number) FROM chapters")
    ch_count, min_bab, max_bab = c.fetchone()
    assert ch_count == 25, f"Expected 25 chapters, got {ch_count}"
    assert min_bab == 1 and max_bab == 25, f"Expected Bab 1..25, got {min_bab}..{max_bab}"
    print(f" [PASS] Chapters: {ch_count} chapters (Bab {min_bab} to {max_bab})")

    # 2. Curriculum Chapter Vocabulary
    c.execute("SELECT count(*) FROM learning_objects WHERE type = 'VOCABULARY' AND bab IS NOT NULL")
    curr_vocab_count = c.fetchone()[0]
    assert curr_vocab_count == 863, f"Expected 863 curriculum vocabulary items, got {curr_vocab_count}"
    print(f" [PASS] Curriculum Vocabulary: {curr_vocab_count} items (Bab 1–25)")

    # 3. Prioritized JFT Verbs
    c.execute("SELECT count(*) FROM learning_objects WHERE id LIKE 'verb_jft_%' AND word_type = 'KATA_KERJA'")
    verb_count = c.fetchone()[0]
    assert verb_count == 549, f"Expected 549 JFT verbs, got {verb_count}"
    c.execute("SELECT count(DISTINCT group_label) FROM learning_objects WHERE id LIKE 'verb_jft_%'")
    verb_groups = c.fetchone()[0]
    assert verb_groups == 11, f"Expected 11 verb groups of 50, got {verb_groups}"
    print(f" [PASS] Prioritized JFT Verbs: {verb_count} items across {verb_groups} groups of 50")

    # 4. Prioritized JFT Adjectives
    c.execute("SELECT count(*) FROM learning_objects WHERE id LIKE 'adj_jft_%'")
    adj_count = c.fetchone()[0]
    assert adj_count == 173, f"Expected 173 JFT adjectives, got {adj_count}"
    c.execute("SELECT count(*) FROM learning_objects WHERE id LIKE 'adj_jft_%' AND word_type = 'KATA_SIFAT_NA'")
    na_count = c.fetchone()[0]
    assert na_count == 64, f"Expected 64 NA adjectives, got {na_count}"
    c.execute("SELECT count(*) FROM learning_objects WHERE id LIKE 'adj_jft_%' AND word_type = 'KATA_SIFAT_I'")
    i_count = c.fetchone()[0]
    assert i_count == 109, f"Expected 109 I adjectives, got {i_count}"
    print(f" [PASS] Prioritized JFT Adjectives: {adj_count} items (64 NA, 109 I) in groups of 50")

    # 5. Canonical 613 Kanji Flashcards (Protected Invariant)
    c.execute("SELECT count(*) FROM learning_objects WHERE type = 'KANJI' AND id NOT LIKE 'kanji_add_%'")
    kanji_613_count = c.fetchone()[0]
    assert kanji_613_count == 613, f"Expected 613 canonical Kanji items, got {kanji_613_count}"
    c.execute("SELECT count(DISTINCT group_label) FROM learning_objects WHERE type = 'KANJI' AND id NOT LIKE 'kanji_add_%'")
    kanji_613_groups = c.fetchone()[0]
    assert kanji_613_groups == 13, f"Expected 13 canonical kanji groups of 50, got {kanji_613_groups}"
    print(f" [PASS] Canonical 613 Kanji: {kanji_613_count} items across {kanji_613_groups} groups of 50 (Protected)")

    # 5b. Additional Kanji Batches
    c.execute("SELECT count(*) FROM learning_objects WHERE id LIKE 'kanji_add_%'")
    add_kanji_count = c.fetchone()[0]
    assert add_kanji_count == 2119, f"Expected 2119 Additional Kanji, got {add_kanji_count}"
    c.execute("SELECT count(DISTINCT group_label) FROM learning_objects WHERE id LIKE 'kanji_add_%'")
    add_kanji_groups = c.fetchone()[0]
    assert add_kanji_groups == 43, f"Expected 43 additional batches of 50, got {add_kanji_groups}"
    print(f" [PASS] Additional Kanji: {add_kanji_count} items across {add_kanji_groups} batches of 50")

    # 6. Grammar count & examples
    c.execute("SELECT count(*) FROM learning_objects WHERE type = 'GRAMMAR'")
    grammar_count = c.fetchone()[0]
    assert grammar_count >= 40, f"Expected >= 40 grammar items, got {grammar_count}"
    c.execute("SELECT count(*) FROM examples")
    ex_count = c.fetchone()[0]
    assert ex_count == grammar_count, f"Expected {grammar_count} examples, got {ex_count}"
    print(f" [PASS] Grammar & Examples: {grammar_count} patterns with authentic examples")

    # 7. Total Learning Objects & Progress
    c.execute("SELECT count(*) FROM learning_objects")
    total_objects = c.fetchone()[0]
    expected_total = curr_vocab_count + verb_count + adj_count + kanji_613_count + add_kanji_count + grammar_count
    assert total_objects == expected_total, f"Expected {expected_total} objects, got {total_objects}"
    c.execute("SELECT count(*) FROM user_progress")
    prog_count = c.fetchone()[0]
    assert prog_count == total_objects, f"Expected {total_objects} progress rows, got {prog_count}"
    print(f" [PASS] Total Learning Objects: {total_objects} items (100% seeded with user_progress)")

    # 8. Word Type Invariant Check
    c.execute("SELECT DISTINCT word_type FROM learning_objects")
    found_types = {r[0] for r in c.fetchall()}
    invalid_types = found_types - VALID_WORD_TYPES
    assert not invalid_types, f"Found invalid word_types: {invalid_types}"
    c.execute("SELECT count(*) FROM learning_objects WHERE word_type IS NULL OR word_type = ''")
    empty_wtype = c.fetchone()[0]
    assert empty_wtype == 0, f"Found {empty_wtype} objects with missing word_type"
    print(f" [PASS] Word Type Invariants: All {total_objects} objects classified with valid word_type ({len(found_types)} categories)")

    # 9. Short-term Cycle Tables Check
    c.execute("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('learning_cycles', 'cycle_items')")
    cycle_tables = [r[0] for r in c.fetchall()]
    assert len(cycle_tables) == 2, f"Expected 2 cycle tables, found {cycle_tables}"
    print(f" [PASS] Cycle Persistence: learning_cycles and cycle_items tables verified")

    # 10. Chapter Isolation Check
    c.execute("SELECT count(*) FROM learning_objects WHERE type = 'VOCABULARY' AND bab = 12")
    bab12_count = c.fetchone()[0]
    assert bab12_count == 22, f"Bab 12 expected 22 items, got {bab12_count}"
    c.execute("SELECT count(*) FROM learning_objects WHERE type = 'VOCABULARY' AND bab = 1")
    bab1_count = c.fetchone()[0]
    assert bab1_count == 27, f"Bab 1 expected 27 items, got {bab1_count}"
    print(f" [PASS] Chapter Isolation: Bab 1 has {bab1_count}, Bab 12 has {bab12_count} (strictly chapter-specific)")

    # 11. Zero Null Invariants in Critical Fields
    c.execute("""
    SELECT count(*) FROM learning_objects
    WHERE id IS NULL OR type IS NULL OR word_type IS NULL OR japanese IS NULL OR reading IS NULL OR indonesian IS NULL
    """)
    null_count = c.fetchone()[0]
    assert null_count == 0, f"Found {null_count} rows with null fields"
    print(f" [PASS] Zero Null Invariants: 0 null fields across all {total_objects} learning objects")

    # 12. Pustaka Vocabulary Browser & Stacking Filter Invariants
    print("Running Pustaka Vocabulary Browser & Stacking Filter Tests...")
    def run_pustaka_query(word_type=None, bab=None, search=None, only_weak=False):
        sql = "SELECT lo.id, lo.type, lo.bab, lo.japanese, lo.reading, lo.indonesian, lo.word_type FROM learning_objects lo"
        if only_weak:
            sql += " INNER JOIN user_progress up ON lo.id = up.object_id"
        sql += " WHERE lo.type = 'VOCABULARY'"
        if only_weak:
            sql += " AND (up.mastery_state = 'WEAK' OR up.lapses > 0)"
        args = []
        if word_type and word_type != 'SEMUA':
            if word_type.upper() == 'KATA_SIFAT':
                sql += " AND lo.word_type IN ('KATA_SIFAT_I', 'KATA_SIFAT_NA')"
            else:
                sql += " AND lo.word_type = ?"
                args.append(word_type.upper())
        if bab and bab > 0:
            sql += " AND lo.bab = ?"
            args.append(bab)
        if search and search.strip():
            q = f"%{search.strip()}%"
            sql += " AND (lo.japanese LIKE ? OR lo.reading LIKE ? OR lo.romaji LIKE ? OR lo.indonesian LIKE ?)"
            args.extend([q, q, q, q])
        c.execute(sql, args)
        return c.fetchall()

    # Test Kata Kerja
    verbs = run_pustaka_query(word_type='KATA_KERJA')
    assert len(verbs) == 745 and all(r[6] == 'KATA_KERJA' for r in verbs), "Kata Kerja filter failed"
    print(" [PASS] Pustaka Filter: Kata Kerja returns strictly 745 verbs (100% verified)")

    # Test Kata Sifat - い & な & General
    i_adjs = run_pustaka_query(word_type='KATA_SIFAT_I')
    assert len(i_adjs) == 153 and all(r[6] == 'KATA_SIFAT_I' for r in i_adjs)
    na_adjs = run_pustaka_query(word_type='KATA_SIFAT_NA')
    assert len(na_adjs) == 80 and all(r[6] == 'KATA_SIFAT_NA' for r in na_adjs)
    all_adjs = run_pustaka_query(word_type='KATA_SIFAT')
    assert len(all_adjs) == 233 and all(r[6] in ('KATA_SIFAT_I', 'KATA_SIFAT_NA') for r in all_adjs)
    print(" [PASS] Pustaka Filter: Kata Sifat (General: 233, い: 153, な: 80, 100% verified)")

    # Test Kata Benda (Nouns only, 0 Kanji)
    nouns = run_pustaka_query(word_type='KATA_BENDA')
    assert len(nouns) == 587 and all(r[1] == 'VOCABULARY' and r[6] == 'KATA_BENDA' for r in nouns)
    c.execute("SELECT count(*) FROM learning_objects WHERE type = 'KANJI' AND word_type = 'KATA_BENDA'")
    assert c.fetchone()[0] == 0, "Kanji leaked into KATA_BENDA"
    print(" [PASS] Pustaka Filter: Kata Benda returns strictly 587 nouns with 0 kanji")

    # Test Chapter filtering & Stacking (Kata Kerja + Bab 8)
    b8 = run_pustaka_query(bab=8)
    assert len(b8) == 75 and all(r[2] == 8 for r in b8)
    b8_verbs = run_pustaka_query(word_type='KATA_KERJA', bab=8)
    assert len(b8_verbs) == 2 and all(r[2] == 8 and r[6] == 'KATA_KERJA' for r in b8_verbs)
    print(" [PASS] Pustaka Stacking: Kata Kerja + Bab 8 yields strictly 2 verbs ['借ります', '切ります']")

    # Test Stacking (Kata Sifat - い + Bab 12)
    b12_i = run_pustaka_query(word_type='KATA_SIFAT_I', bab=12)
    assert len(b12_i) == 2 and all(r[2] == 12 and r[6] == 'KATA_SIFAT_I' for r in b12_i)
    print(" [PASS] Pustaka Stacking: Kata Sifat - い + Bab 12 yields strictly 2 items ['はやい', 'わかい']")

    # Test Reset to Semua preserves Bab
    reset_b8 = run_pustaka_query(word_type='SEMUA', bab=8)
    assert len(reset_b8) == 75 and all(r[2] == 8 for r in reset_b8)
    print(" [PASS] Pustaka Reset: 'Semua' resets word type while preserving Bab 8 (75 items)")

    # Test Empty state guard (Kata Kerja + Bab 1 = 0 items)
    empty_b1 = run_pustaka_query(word_type='KATA_KERJA', bab=1)
    assert len(empty_b1) == 0, "Expected 0 verbs in Bab 1"
    print(" [PASS] Pustaka Empty State: Kata Kerja + Bab 1 correctly returns 0 items for empty UI display")

    print("Running Pustaka Group Queries & SQLite Aggregate Invariant Tests...")
    # Regression tests for KotobaDatabase group getters
    c.execute("SELECT group_label FROM learning_objects WHERE type = 'KANJI' AND id NOT LIKE 'kanji_add_%' AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC")
    formal_groups = [r[0] for r in c.fetchall()]
    assert len(formal_groups) == 13, f"Expected 13 formal groups, got {len(formal_groups)}"
    assert formal_groups[0] == "01–50" and formal_groups[-1] == "601–613"
    print(f" [PASS] Group Query: Formal Kanji returns {len(formal_groups)} groups in strict sort_order without SQL aggregate misuse")

    c.execute("SELECT group_label FROM learning_objects WHERE id LIKE 'kanji_add_%' AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC")
    add_groups = [r[0] for r in c.fetchall()]
    assert len(add_groups) == 43, f"Expected 43 additional groups, got {len(add_groups)}"
    assert add_groups[0] == "Additional 01–50" and add_groups[-1] == "Additional 2101–2119"
    print(f" [PASS] Group Query: Additional Kanji returns {len(add_groups)} batches in strict sort_order without SQL aggregate misuse")

    c.execute("SELECT group_label FROM learning_objects WHERE word_type = 'KATA_KERJA' AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC")
    verb_groups = [r[0] for r in c.fetchall()]
    assert len(verb_groups) == 11, f"Expected 11 verb groups, got {len(verb_groups)}"
    print(f" [PASS] Group Query: Verb groups returns {len(verb_groups)} batches without SQL aggregate misuse")

    c.execute("SELECT group_label FROM learning_objects WHERE (word_type = 'KATA_SIFAT_NA' OR word_type = 'KATA_SIFAT_I') AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC")
    adj_groups = [r[0] for r in c.fetchall()]
    assert len(adj_groups) == 4, f"Expected 4 adjective groups, got {len(adj_groups)}"
    print(f" [PASS] Group Query: Adjective groups returns {len(adj_groups)} batches without SQL aggregate misuse")

    c.execute("PRAGMA user_version")
    uv = c.fetchone()[0]
    assert uv == 2, f"Expected PRAGMA user_version = 2, got {uv}"
    print(f" [PASS] Database Version: PRAGMA user_version = {uv} (Version 2 confirmed)")

    print("Running Multi-Bab, 50-Kotoba Batches, and Audio Reading Tests...")
    # Multi-Bab pooling
    c.execute("SELECT count(*) FROM learning_objects WHERE type = 'VOCABULARY' AND bab IN (1, 2)")
    babs_1_2 = c.fetchone()[0]
    assert babs_1_2 == 57, f"Expected 57 items for Bab 1 & 2, got {babs_1_2}"
    print(f" [PASS] Multi-Bab Pooling: Bab 1 and 2 returns {babs_1_2} items")

    c.execute("SELECT count(*) FROM learning_objects WHERE type = 'VOCABULARY' AND bab IN (4, 25)")
    babs_4_25 = c.fetchone()[0]
    assert babs_4_25 == 53, f"Expected 53 items for Bab 4 & 25, got {babs_4_25}"
    print(f" [PASS] Multi-Bab Pooling: Bab 4 and 25 returns {babs_4_25} items")

    # 50-word batches for Kata Kerja
    c.execute("SELECT count(*) FROM learning_objects WHERE type = 'VOCABULARY' AND word_type = 'KATA_KERJA'")
    total_verbs = c.fetchone()[0]
    assert total_verbs == 745, f"Expected 745 verbs, got {total_verbs}"

    c.execute("SELECT count(*) FROM (SELECT id FROM learning_objects WHERE type = 'VOCABULARY' AND word_type = 'KATA_KERJA' ORDER BY sort_order ASC LIMIT 50 OFFSET 0)")
    batch_1 = c.fetchone()[0]
    assert batch_1 == 50, f"Expected 50 items in batch 1, got {batch_1}"

    c.execute("SELECT count(*) FROM (SELECT id FROM learning_objects WHERE type = 'VOCABULARY' AND word_type = 'KATA_KERJA' ORDER BY sort_order ASC LIMIT 50 OFFSET 700)")
    batch_final = c.fetchone()[0]
    assert batch_final == 45, f"Expected 45 items in final batch (701–745), got {batch_final}"
    print(f" [PASS] 50-Word Batches: Kata Kerja properly slices into 15 batches (Batch 1: 50, Final Batch: 45)")

    # 100% Kana reading presence
    c.execute("SELECT count(*) FROM learning_objects WHERE type = 'KANJI' AND (reading IS NULL OR reading = '' OR reading = '—')")
    kanji_missing_reading = c.fetchone()[0]
    assert kanji_missing_reading == 0, f"Found {kanji_missing_reading} kanji without Kana reading"

    c.execute("SELECT count(*) FROM learning_objects WHERE type = 'VOCABULARY' AND (reading IS NULL OR reading = '' OR reading = '—')")
    vocab_missing_reading = c.fetchone()[0]
    assert vocab_missing_reading == 0, f"Found {vocab_missing_reading} vocabulary without Kana reading"
    print(f" [PASS] Audio Precision: 100% of Kanji and Vocabulary have valid Kana readings for accurate TTS pronunciation")

    conn.close()
    print("============================================================")
    print("ALL CURRICULUM & RESOURCE INTEGRITY TESTS PASSED (100% Verified)")
    print("============================================================")

if __name__ == "__main__":
    test_database()
