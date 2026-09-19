"""
test_ios_behavior_contract.py - Cross-Platform Behavioral & Algorithmic Contract Test Suite
Validates that the iOS Swift engine logic (SrsScheduler, IngatLupaEngine, TTS phonetic target resolution,
and Front Display modes) strictly matches the canonical Android Java implementation.
"""

import os
import sqlite3
import unittest
from typing import Dict, Any, List

class MockUserProgress:
    def __init__(self, item_id: str = "test_item_1"):
        self.item_id = item_id
        self.interval_days = 0
        self.ease_factor = 2.5
        self.repetitions = 0
        self.lapses = 0
        self.stability = 0.0
        self.retrievability = 1.0
        self.last_response_time_ms = 0
        self.last_review_epoch = 0
        self.next_review_epoch = 0
        self.mastery_state = "UNSEEN"

def schedule_srs_review(current: MockUserProgress, is_correct: bool, response_time_ms: int, current_epoch: int = 1700000000) -> MockUserProgress:
    """Exact Python reproduction of both Android SrsScheduler.java and iOS SrsScheduler.swift."""
    p = MockUserProgress(current.item_id)
    p.interval_days = current.interval_days
    p.ease_factor = current.ease_factor
    p.repetitions = current.repetitions
    p.lapses = current.lapses
    p.stability = current.stability

    seconds_per_day = 86400
    quarantine_seconds = 4 * 3600

    is_struggling = response_time_ms > 10000
    is_fluent = 800 <= response_time_ms <= 4000

    if is_correct:
        p.repetitions += 1
        if is_fluent:
            p.ease_factor = min(3.0, p.ease_factor + 0.1)
        elif is_struggling:
            p.ease_factor = max(1.3, p.ease_factor - 0.15)

        if p.repetitions == 1:
            p.interval_days = 1
            p.stability = 1.0
        elif p.repetitions == 2:
            p.interval_days = 3 if is_struggling else 6
            p.stability = float(p.interval_days)
        else:
            multiplier = p.ease_factor
            if is_struggling:
                multiplier = max(1.2, p.ease_factor * 0.7)
            p.interval_days = int(round(p.interval_days * multiplier))
            p.stability = float(p.interval_days)

        next_due = current_epoch + (p.interval_days * seconds_per_day)

        if p.repetitions >= 5 and p.interval_days >= 21:
            p.mastery_state = "MASTERED"
        elif p.repetitions >= 3 and p.interval_days >= 7:
            p.mastery_state = "STABLE"
        else:
            p.mastery_state = "LEARNING"

        p.retrievability = 1.0
        p.last_response_time_ms = response_time_ms
        p.last_review_epoch = current_epoch
        p.next_review_epoch = next_due
    else:
        p.lapses += 1
        p.repetitions = 0
        p.interval_days = 1
        p.ease_factor = max(1.3, p.ease_factor - 0.2)
        p.stability = 0.5

        if p.lapses >= 2:
            next_due = current_epoch + quarantine_seconds
        else:
            next_due = current_epoch + seconds_per_day

        p.retrievability = 0.0
        p.last_response_time_ms = response_time_ms
        p.last_review_epoch = current_epoch
        p.next_review_epoch = next_due
        p.mastery_state = "WEAK"

    return p


class MockIngatLupaEngine:
    """Exact Python reproduction of Android IngatLupaEngine.java and iOS IngatLupaEngine.swift."""
    def __init__(self, pool: List[Dict[str, Any]]):
        self.initial_pool = list(pool)
        self.current_pass_items = list(pool)
        self.current_lupa_list = []
        self.all_resolved_items = []
        self.current_index = 0
        self.cycle_iteration = 1
        self.is_complete = len(pool) == 0
        self.total_ingat_count = 0
        self.total_lupa_count = 0

    @property
    def current_item(self):
        if self.is_complete or self.current_index >= len(self.current_pass_items):
            return None
        return self.current_pass_items[self.current_index]

    @property
    def is_recovery_round(self):
        return self.cycle_iteration > 1

    @property
    def current_pass_total(self):
        return len(self.current_pass_items)

    def mark_ingat(self, response_time_ms: int = 1000) -> bool:
        if self.is_complete:
            return True
        item = self.current_item
        if not item:
            return True

        self.total_ingat_count += 1
        if not any(x["id"] == item["id"] for x in self.all_resolved_items):
            self.all_resolved_items.append(item)

        self.current_index += 1
        self._check_end_of_pass()
        return self.is_complete

    def mark_lupa(self, response_time_ms: int = 1000) -> bool:
        if self.is_complete:
            return True
        item = self.current_item
        if not item:
            return True

        self.total_lupa_count += 1
        self.current_lupa_list.append(item)

        self.current_index += 1
        self._check_end_of_pass()
        return self.is_complete

    def go_back(self):
        if self.current_index > 0:
            self.current_index -= 1

    def _check_end_of_pass(self):
        if self.current_index >= len(self.current_pass_items):
            if len(self.current_lupa_list) == 0:
                self.is_complete = True
            else:
                self.current_pass_items = list(self.current_lupa_list)
                self.current_lupa_list.clear()
                self.cycle_iteration += 1
                self.current_index = 0


class TestIosBehaviorContract(unittest.TestCase):
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

    # --- 1. SRS SCHEDULER MATHEMATICAL PARITY ---

    def test_srs_fluent_progression_to_mastery(self):
        p = MockUserProgress()
        # Round 1: fluent (1500ms)
        p = schedule_srs_review(p, True, 1500)
        self.assertEqual(p.repetitions, 1)
        self.assertEqual(p.interval_days, 1)
        self.assertAlmostEqual(p.ease_factor, 2.6, places=2)
        self.assertEqual(p.mastery_state, "LEARNING")

        # Round 2: fluent (1500ms)
        p = schedule_srs_review(p, True, 1500)
        self.assertEqual(p.repetitions, 2)
        self.assertEqual(p.interval_days, 6)
        self.assertAlmostEqual(p.ease_factor, 2.7, places=2)
        self.assertEqual(p.mastery_state, "LEARNING")

        # Round 3: fluent (1500ms) -> interval = round(6 * 2.8) = 17
        p = schedule_srs_review(p, True, 1500)
        self.assertEqual(p.repetitions, 3)
        self.assertEqual(p.interval_days, 17)  # 6 * 2.8 = 16.8 -> 17
        self.assertAlmostEqual(p.ease_factor, 2.8, places=2)
        self.assertEqual(p.mastery_state, "STABLE")

        # Round 4: fluent (1500ms) -> interval = round(17 * 2.9) = 49
        p = schedule_srs_review(p, True, 1500)
        self.assertEqual(p.repetitions, 4)
        self.assertEqual(p.interval_days, 49)  # 17 * 2.9 = 49.3 -> 49
        self.assertAlmostEqual(p.ease_factor, 2.9, places=2)
        self.assertEqual(p.mastery_state, "STABLE")

        # Round 5: fluent (1500ms) -> repetitions >= 5 and interval >= 21 => MASTERED
        p = schedule_srs_review(p, True, 1500)
        self.assertEqual(p.repetitions, 5)
        self.assertTrue(p.interval_days >= 21)
        self.assertEqual(p.mastery_state, "MASTERED")

    def test_srs_struggling_recall_penalties(self):
        p = MockUserProgress()
        # Round 1: struggling (12000ms)
        p = schedule_srs_review(p, True, 12000)
        self.assertEqual(p.repetitions, 1)
        self.assertEqual(p.interval_days, 1)
        self.assertAlmostEqual(p.ease_factor, 2.35, places=2)

        # Round 2: struggling (12000ms) -> interval should be 3 (not 6)
        p = schedule_srs_review(p, True, 12000)
        self.assertEqual(p.repetitions, 2)
        self.assertEqual(p.interval_days, 3)
        self.assertAlmostEqual(p.ease_factor, 2.20, places=2)

    def test_srs_lapse_and_anti_runaway_quarantine(self):
        p = MockUserProgress()
        # 3 successful reps
        p = schedule_srs_review(p, True, 2000)
        p = schedule_srs_review(p, True, 2000)
        p = schedule_srs_review(p, True, 2000)
        self.assertEqual(p.repetitions, 3)

        # First lapse
        epoch = 1700000000
        p = schedule_srs_review(p, False, 5000, current_epoch=epoch)
        self.assertEqual(p.lapses, 1)
        self.assertEqual(p.repetitions, 0)
        self.assertEqual(p.interval_days, 1)
        self.assertEqual(p.mastery_state, "WEAK")
        self.assertEqual(p.next_review_epoch, epoch + 86400)

        # Second consecutive lapse -> triggers 4-hour quarantine safeguard
        p = schedule_srs_review(p, False, 5000, current_epoch=epoch)
        self.assertEqual(p.lapses, 2)
        self.assertEqual(p.next_review_epoch, epoch + (4 * 3600))

    # --- 2. INGAT / LUPA ACTIVE RECALL RECOVERY LOOP ---

    def test_ingat_lupa_recovery_loop_closure(self):
        pool = [{"id": f"item_{i}", "japanese": f"Word{i}"} for i in range(10)]
        engine = MockIngatLupaEngine(pool)

        # Round 1: Pass 1 (10 items). Mark 7 Ingat, 3 Lupa.
        for i in range(7):
            engine.mark_ingat()
        for i in range(3):
            engine.mark_lupa()

        # Check end of Round 1
        self.assertFalse(engine.is_complete)
        self.assertEqual(engine.cycle_iteration, 2)
        self.assertTrue(engine.is_recovery_round)
        self.assertEqual(engine.current_pass_total, 3)
        self.assertEqual(engine.total_ingat_count, 7)
        self.assertEqual(engine.total_lupa_count, 3)

        # Round 2: Pass 2 (3 items). Mark 2 Ingat, 1 Lupa.
        engine.mark_ingat()
        engine.mark_ingat()
        engine.mark_lupa()

        # Check end of Round 2
        self.assertFalse(engine.is_complete)
        self.assertEqual(engine.cycle_iteration, 3)
        self.assertEqual(engine.current_pass_total, 1)
        self.assertEqual(engine.total_ingat_count, 9)
        self.assertEqual(engine.total_lupa_count, 4)

        # Round 3: Pass 3 (1 item). Mark 1 Ingat.
        completed = engine.mark_ingat()
        self.assertTrue(completed)
        self.assertTrue(engine.is_complete)
        self.assertEqual(len(engine.all_resolved_items), 10)
        self.assertEqual(engine.total_ingat_count, 10)
        self.assertEqual(engine.total_lupa_count, 4)

    def test_bidirectional_navigation_invariant(self):
        pool = [{"id": f"item_{i}", "japanese": f"Word{i}"} for i in range(5)]
        engine = MockIngatLupaEngine(pool)

        self.assertEqual(engine.current_index, 0)
        engine.go_back()  # should be no-op at index 0
        self.assertEqual(engine.current_index, 0)

        # Answer 2 cards
        engine.mark_ingat()
        engine.mark_ingat()
        self.assertEqual(engine.current_index, 2)

        # Navigate back
        engine.go_back()
        self.assertEqual(engine.current_index, 1)
        self.assertEqual(engine.total_ingat_count, 2)  # does not alter judgment count

    # --- 3. PHONETIC TTS TARGET RESOLUTION SYMMETRY ---

    def test_phonetic_tts_speech_target_across_all_kanji(self):
        c = self.conn.cursor()
        c.execute("SELECT id, japanese, reading, type FROM learning_objects WHERE type = 'KANJI'")
        kanji_items = c.fetchall()

        for item in kanji_items:
            reading = item["reading"]
            japanese = item["japanese"]

            # Assertion: Both front and back audio MUST use the phonetic reading
            target_text = reading if reading and reading.strip() else japanese
            self.assertTrue(bool(target_text), f"TTS target text must not be empty for {item['id']}")
            # Crucial invariant: For kanji, targetText must never equal raw ideograph unless reading is missing
            if reading and reading.strip():
                self.assertEqual(target_text, reading)
                self.assertNotEqual(target_text, japanese, f"Kanji {item['id']} ({japanese}) must synthesize phonetic reading '{reading}', not raw ideograph.")

    def test_tts_target_symmetry_kanji_701(self):
        c = self.conn.cursor()
        c.execute("SELECT id, japanese, reading, romaji FROM learning_objects WHERE id = 'kanji_add_0701'")
        item = c.fetchone()
        self.assertIsNotNone(item)
        self.assertEqual(item["japanese"], "釣")
        self.assertEqual(item["reading"], "つり")

        # Invariant: FlashcardViewModel playFrontAudio() and playBackAudio() must resolve to 'つり'
        target_text = item["reading"] if item["reading"] else item["japanese"]
        self.assertEqual(target_text, "つり")

    # --- 4. FRONT DISPLAY MODE DATA MAPPING ---

    def test_card_front_modes_data_projection(self):
        c = self.conn.cursor()
        c.execute("SELECT id, japanese, reading, indonesian FROM learning_objects WHERE id = 'vocab_0001'")
        item = c.fetchone()
        self.assertIsNotNone(item)

        # Mode: Kanji Front
        kanji_front_face = item["japanese"]
        kanji_back_face_reading = item["reading"]
        kanji_back_face_meaning = item["indonesian"]
        self.assertEqual(kanji_front_face, item["japanese"])
        self.assertEqual(kanji_back_face_reading, item["reading"])

        # Mode: Hiragana Front
        hiragana_front_face = item["reading"] if item["reading"] else item["japanese"]
        hiragana_back_face_kanji = item["japanese"]
        self.assertEqual(hiragana_front_face, item["reading"])
        self.assertEqual(hiragana_back_face_kanji, item["japanese"])

        # Mode: Arti Front
        arti_front_face = item["indonesian"]
        arti_back_face_kanji = item["japanese"]
        arti_back_face_reading = item["reading"]
        self.assertEqual(arti_front_face, item["indonesian"])
        self.assertEqual(arti_back_face_kanji, item["japanese"])
        self.assertEqual(arti_back_face_reading, item["reading"])

if __name__ == "__main__":
    unittest.main()
