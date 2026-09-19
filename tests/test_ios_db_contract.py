import os
import sqlite3
import unittest

class TestIosDbContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.db_path = "ios/Kotoba/Resources/kotoba.db"
        if not os.path.exists(cls.db_path):
            raise FileNotFoundError(f"Database not found at {cls.db_path}")
        cls.conn = sqlite3.connect(cls.db_path)
        cls.conn.row_factory = sqlite3.Row

    @classmethod
    def tearDownClass(cls):
        cls.conn.close()

    def test_total_dataset_count(self):
        c = self.conn.cursor()
        c.execute("SELECT COUNT(*) FROM learning_objects")
        count = c.fetchone()[0]
        self.assertEqual(count, 4363, f"Expected 4363 learning objects, found {count}")

    def test_chapters_count(self):
        c = self.conn.cursor()
        c.execute("SELECT COUNT(*) FROM chapters")
        count = c.fetchone()[0]
        self.assertEqual(count, 25, f"Expected 25 chapters, found {count}")

    def test_formal_kanji_count(self):
        c = self.conn.cursor()
        c.execute("SELECT COUNT(*) FROM learning_objects WHERE type = 'KANJI' AND id NOT LIKE 'kanji_add_%'")
        count = c.fetchone()[0]
        self.assertEqual(count, 613, f"Expected 613 formal kanji, found {count}")

    def test_additional_kanji_count(self):
        c = self.conn.cursor()
        c.execute("SELECT COUNT(*) FROM learning_objects WHERE id LIKE 'kanji_add_%'")
        count = c.fetchone()[0]
        self.assertEqual(count, 2119, f"Expected 2119 additional kanji, found {count}")

    def test_curriculum_vocab_count(self):
        c = self.conn.cursor()
        c.execute("SELECT COUNT(*) FROM learning_objects WHERE type = 'VOCABULARY' AND bab > 0")
        count = c.fetchone()[0]
        self.assertEqual(count, 863, f"Expected 863 curriculum vocabulary items, found {count}")

    def test_all_database_service_queries(self):
        c = self.conn.cursor()

        # 1. getAllChapters
        c.execute("SELECT bab_number, title_ja, title_id, theme, level, vocab_count, grammar_count FROM chapters ORDER BY bab_number ASC")
        chapters = c.fetchall()
        self.assertEqual(len(chapters), 25)

        # 2. getChapter
        c.execute("SELECT bab_number, title_ja, title_id, theme, level, vocab_count, grammar_count FROM chapters WHERE bab_number = ?", (1,))
        chap1 = c.fetchone()
        self.assertIsNotNone(chap1)
        self.assertEqual(chap1["bab_number"], 1)

        # 3. getVocabularyForBab
        lo_cols = "id, type, bab, level, japanese, reading, romaji, indonesian, badge_label, group_label, sort_order, details_json, word_type"
        c.execute(f"SELECT {lo_cols} FROM learning_objects WHERE type = 'VOCABULARY' AND bab = ? ORDER BY sort_order ASC", (1,))
        vocab1 = c.fetchall()
        self.assertTrue(len(vocab1) > 0)

        # 4. getVocabularyForBabs (multi-bab)
        c.execute(f"SELECT {lo_cols} FROM learning_objects WHERE type = 'VOCABULARY' AND bab IN (?,?) ORDER BY bab ASC, sort_order ASC", (1, 2))
        vocab1_2 = c.fetchall()
        self.assertTrue(len(vocab1_2) > len(vocab1))

        # 5. getFormalKanjiGroups
        c.execute("SELECT group_label FROM learning_objects WHERE type = 'KANJI' AND id NOT LIKE 'kanji_add_%' AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC")
        kanji_groups = c.fetchall()
        self.assertTrue(len(kanji_groups) > 0)

        # 6. getAdditionalKanjiGroups
        c.execute("SELECT group_label FROM learning_objects WHERE id LIKE 'kanji_add_%' AND group_label IS NOT NULL GROUP BY group_label ORDER BY min(sort_order) ASC")
        add_groups = c.fetchall()
        self.assertTrue(len(add_groups) > 0)

        # 7. queryVocabulary filter
        c.execute(f"SELECT {lo_cols} FROM learning_objects lo WHERE lo.type = 'VOCABULARY' AND lo.word_type = ? AND (lo.japanese LIKE ? OR lo.reading LIKE ? OR lo.romaji LIKE ? OR lo.indonesian LIKE ?) ORDER BY lo.bab ASC, lo.sort_order ASC", ("KATA_BENDA", "%orang%", "%orang%", "%orang%", "%orang%"))
        res = c.fetchall()
        self.assertTrue(len(res) > 0)

        # 8. getMasteryCounts
        c.execute("SELECT mastery_state, count(*) FROM user_progress GROUP BY mastery_state")
        counts = dict(c.fetchall())
        self.assertIn("UNSEEN", counts)

    def test_kanji_701_and_data_integrity(self):
        c = self.conn.cursor()
        # Test kanji_add_0701
        c.execute("SELECT japanese, reading, romaji, indonesian FROM learning_objects WHERE id = 'kanji_add_0701'")
        row = c.fetchone()
        self.assertIsNotNone(row)
        self.assertEqual(row["japanese"], "釣")
        self.assertEqual(row["reading"], "つり")
        self.assertEqual(row["romaji"], "tsuri")
        self.assertIn("mancing", row["indonesian"].lower())

        # Test no empty readings across all 4363 objects
        c.execute("SELECT COUNT(*) FROM learning_objects WHERE reading IS NULL OR trim(reading) = ''")
        empty_readings = c.fetchone()[0]
        self.assertEqual(empty_readings, 0, f"Found {empty_readings} empty readings in DB")

        # Test no hiragana corruptions in canonical kanji romaji
        c.execute("SELECT id, romaji FROM learning_objects WHERE type = 'KANJI' AND id NOT LIKE 'kanji_add_%' AND romaji LIKE '%も%'")
        corrupted = c.fetchall()
        self.assertEqual(len(corrupted), 0, f"Found corrupted romaji with 'も': {corrupted}")

if __name__ == "__main__":
    unittest.main()
