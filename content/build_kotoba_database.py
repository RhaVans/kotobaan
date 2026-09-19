#!/usr/bin/env python3
"""
Kotoba.app Database Builder
Generates canonical pre-compiled SQLite database `kotoba.db` in app/src/main/assets/databases/
Strictly enforces chapter integrity, domain isolation, and zero-null invariants.
Ingests:
- 863 Bab 1–25 IM JAPAN Vocabulary items with linguistic classification (word_type)
- 613 Kanji entries (JFT A2 / Irodori) in groups of 50
- 41 Grammar patterns with real-world examples
- 549 JFT Verbs (KATA KERJA) in 11 groups of 50
- 173 JFT Adjectives (KATA SIFAT NA & I) in groups of 50
"""

import os
import json
import sqlite3
from pathlib import Path

PROJECT_ROOT = Path("/storage/emulated/0/download/PROJECT/KTB")
ASSETS_DIR = PROJECT_ROOT / "app" / "src" / "main" / "assets"
DB_DIR = ASSETS_DIR / "databases"
DB_DIR.mkdir(parents=True, exist_ok=True)
DB_PATH = DB_DIR / "kotoba.db"

if DB_PATH.exists():
    DB_PATH.unlink()

conn = sqlite3.connect(DB_PATH)
cursor = conn.cursor()

print("1. Creating SQLite schema...")
cursor.executescript("""
PRAGMA foreign_keys = ON;
PRAGMA user_version = 3;

CREATE TABLE chapters (
    bab_number INTEGER PRIMARY KEY,
    title_ja TEXT NOT NULL,
    title_id TEXT NOT NULL,
    theme TEXT NOT NULL,
    level TEXT NOT NULL DEFAULT 'N5',
    vocab_count INTEGER DEFAULT 0,
    grammar_count INTEGER DEFAULT 0
);

CREATE TABLE learning_objects (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL, -- 'VOCABULARY', 'KANJI', 'GRAMMAR'
    word_type TEXT NOT NULL DEFAULT 'LAINNYA', -- 'KATA_BENDA', 'KATA_KERJA', 'KATA_SIFAT_I', 'KATA_SIFAT_NA', 'KATA_KETERANGAN', 'KATA_SAMBUNG', 'PARTIKEL', 'UNGKAPAN', 'LAINNYA'
    bab INTEGER,
    level TEXT NOT NULL,
    japanese TEXT NOT NULL,
    reading TEXT NOT NULL,
    romaji TEXT,
    indonesian TEXT NOT NULL,
    badge_label TEXT NOT NULL,
    group_label TEXT,
    sort_order INTEGER NOT NULL,
    details_json TEXT
);

CREATE TABLE examples (
    example_id INTEGER PRIMARY KEY AUTOINCREMENT,
    object_id TEXT NOT NULL REFERENCES learning_objects(id),
    japanese TEXT NOT NULL,
    reading TEXT NOT NULL,
    indonesian TEXT NOT NULL
);

CREATE TABLE user_progress (
    object_id TEXT PRIMARY KEY REFERENCES learning_objects(id),
    interval_days INTEGER DEFAULT 0,
    ease_factor REAL DEFAULT 2.5,
    repetitions INTEGER DEFAULT 0,
    lapses INTEGER DEFAULT 0,
    stability REAL DEFAULT 0.0,
    retrievability REAL DEFAULT 1.0,
    last_response_time_ms INTEGER DEFAULT 0,
    last_review_epoch INTEGER DEFAULT 0,
    next_review_epoch INTEGER DEFAULT 0,
    mastery_state TEXT DEFAULT 'UNSEEN' -- 'UNSEEN', 'LEARNING', 'WEAK', 'STABLE', 'MASTERED'
);

CREATE TABLE review_attempts (
    attempt_id INTEGER PRIMARY KEY AUTOINCREMENT,
    object_id TEXT NOT NULL REFERENCES learning_objects(id),
    timestamp_epoch INTEGER NOT NULL,
    response_time_ms INTEGER NOT NULL,
    is_correct INTEGER NOT NULL,
    error_category TEXT, -- 'NONE', 'MEANING', 'READING', 'GRAMMAR', 'CONFUSION'
    chosen_answer TEXT,
    correct_answer TEXT
);

CREATE TABLE custom_cards (
    card_id TEXT PRIMARY KEY,
    japanese TEXT NOT NULL,
    reading TEXT NOT NULL,
    romaji TEXT,
    indonesian TEXT NOT NULL,
    notes TEXT,
    created_epoch INTEGER NOT NULL
);

-- Short-term Active-Recall Learning Cycle Persistence
CREATE TABLE learning_cycles (
    cycle_id TEXT PRIMARY KEY,
    source TEXT NOT NULL, -- 'CURRICULUM', 'LIBRARY', 'JFT_VERB', 'JFT_ADJ', 'KANJI'
    material_type TEXT NOT NULL, -- 'KOTOBA', 'KANJI', 'KATA_KERJA', 'KATA_SIFAT'
    configured_amount INTEGER NOT NULL,
    display_mode TEXT NOT NULL DEFAULT 'KANJI', -- 'KANJI', 'KANA', 'INDONESIA'
    pool_definition_json TEXT,
    started_at_epoch INTEGER NOT NULL,
    completed_at_epoch INTEGER,
    status TEXT NOT NULL DEFAULT 'ACTIVE' -- 'ACTIVE', 'COMPLETED', 'ABANDONED'
);

CREATE TABLE cycle_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cycle_id TEXT NOT NULL REFERENCES learning_cycles(cycle_id),
    object_id TEXT NOT NULL REFERENCES learning_objects(id),
    pool_type TEXT NOT NULL, -- 'NORMAL', 'RECOVERY'
    cycle_iteration INTEGER NOT NULL DEFAULT 1,
    response TEXT NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'INGAT', 'LUPA'
    response_time_ms INTEGER DEFAULT 0,
    attempt_count INTEGER DEFAULT 0
);

CREATE INDEX idx_obj_type ON learning_objects(type);
CREATE INDEX idx_obj_wtype ON learning_objects(word_type);
CREATE INDEX idx_obj_bab ON learning_objects(bab);
CREATE INDEX idx_obj_group ON learning_objects(group_label);
CREATE INDEX idx_progress_state ON user_progress(mastery_state);
CREATE INDEX idx_progress_next ON user_progress(next_review_epoch);
CREATE INDEX idx_cycle_status ON learning_cycles(status);
CREATE INDEX idx_cycle_items_cid ON cycle_items(cycle_id);
""")

print("2. Populating Chapters (Bab 1–25 IM JAPAN)...")
chapters_data = [
    (1, "わたしはインドネシア人です", "Perkenalan Diri & Profesi", "Tata Bahasa Dasar & Salam", "N5"),
    (2, "これは なんですか", "Benda Sekitar & Kepemilikan", "Kata Tunjuk Benda (Kore, Sore, Are)", "N5"),
    (3, "ここは きょうしつです", "Tempat & Lokasi Fasilitas", "Kata Tunjuk Tempat (Koko, Soko, Asoko)", "N5"),
    (4, "いま なんじですか", "Waktu, Jam & Hari Kerja", "Waktu dan Bentuk Kata Kerja Masu/Masen", "N5"),
    (5, "スーパーへ いきます", "Arah, Tujuan & Transportasi", "Perpindahan & Partikel He/De/To", "N5"),
    (6, "ごはんを たべます", "Aktivitas Harian & Objek", "Partikel O Transitif & Tempat Aktivitas", "N5"),
    (7, "はしで たべます", "Alat, Sarana & Pemberian", "Partikel De (Alat) & Agemasu/Moraimasu", "N5"),
    (8, "さくらは きれいです", "Ciri & Kesan Lingkungan", "Kata Sifat i-keiyoushi & na-keiyoushi", "N5"),
    (9, "どんな スポーツが すきですか", "Kesukaan, Minat & Alasan", "Partikel Ga (Suki/Kirai/Jouzu) & Karu", "N5"),
    (10, "つくえの うえに あります", "Letak & Posisi Keberadaan", "Keberadaan Arimasu/Imasu & Preposisi", "N5"),
    (11, "りんごを みっつ かいました", "Jumlah, Bilangan & Jangka Waktu", "Bilangan Satuan Counter & Durasi", "N5"),
    (12, "きのうは あめでした", "Perbandingan & Cuaca Lampau", "Bentuk Lampau Kata Sifat & Yori/Hou ga", "N5"),
    (13, "カメラが ほしいです", "Keinginan & Maksud Tujuan", "Pola Hoshii, Bentuk Tai & Tujuan Ni", "N5"),
    (14, "ちょっと まって ください", "Permohonan & Instruksi Kerja", "Konjugasi Bentuk-TE & ~te kudasai", "N5"),
    (15, "しゃしんを とっても いいです", "Izin & Larangan Formal", "Pola ~te mo ii desu & ~te wa ikemasen", "N5"),
    (16, "あさ おきて、さんぽします", "Rangkaian Urutan Kegiatan", "Perangkaian Bentuk-TE & ~te kara", "N5"),
    (17, "くすりを のまなければ なりません", "Keharusan & Hal Terlarang", "Konjugasi Bentuk-NAI & ~nakereba narimasen", "N5"),
    (18, "ピアノを ひくことが できます", "Kemampuan & Hobi Pribadi", "Bentuk Kamus (Jishokei) & ~koto ga dekimasu", "N5"),
    (19, "すしを たべたことが あります", "Pengalaman & Variasi Kegiatan", "Bentuk-TA (Lampau Biasa) & ~ta koto ga aru", "N5"),
    (20, "いっしょに いかない？", "Gaya Bicara Santai", "Bentuk Biasa (Futsuukei) vs Bentuk Sopan", "N5"),
    (21, "あしたは あめだと おもいます", "Pendapat & Informasi Dengar", "Pola ~to omoimasu & ~to iimashita", "N5"),
    (22, "これは わたしが つくった ケーキです", "Modifikasi Kata Benda", "Anak Kalimat Menerangkan Kata Benda", "N5"),
    (23, "みちを わたる とき", "Waktu Situasi & Kondisi Pasti", "Pola ~toki (Ketika) & Partikel To (Syarat)", "N5"),
    (24, "ともだちが てつだって くれました", "Bantuan & Kebaikan Sosial", "Pola Pemberian Jasa (~te morau, ~te kureru)", "N5"),
    (25, "あめが ふったら いきません", "Pengandaian & Kondisional Waktu", "Pola ~tara (Jika/Setelah) & ~temo (Meskipun)", "N5")
]

cursor.executemany("INSERT INTO chapters (bab_number, title_ja, title_id, theme, level) VALUES (?, ?, ?, ?, ?)", chapters_data)

print("3. Ingesting Chapter Vocabulary Dataset (863 items with word_type)...")
vocab_file = ASSETS_DIR / "kotoba_dataset.json"
if not vocab_file.exists():
    vocab_file = PROJECT_ROOT / "kotoba_dataset.json"

with open(vocab_file, "r", encoding="utf-8") as f:
    vocab_data = json.load(f)

# Linguistic classification rules
na_adj_set = {
    "きれい", "しずか", "にぎやか", "ゆうめい", "しんせつ", "げんき", "ひま", "べんり", 
    "すてき", "すき", "きらい", "じょうず", "へた", "たいへん", "かんたん", "いろいろ", 
    "たいせつ", "だいじょうぶ", "むだ", "ふべん", "じゅうぶん", "しんぱい", "ざんねん"
}
i_adj_set = {
    "おおきい", "ちいさい", "あたらしい", "ふるい", "いい", "よい", "わるい", "あつい", "さむい", 
    "つめたい", "むずかしい", "やさしい", "たかい", "やすい", "ひくい", "おもしろい", "おいしい", 
    "いそがしい", "たのしい", "しろい", "くろい", "あかい", "あおい", "ちかい", "とおい", 
    "はやい", "おそい", "おおい", "すくない", "あたたかい", "すずしい", "あまい", "からい", 
    "おもい", "かるい", "ひろい", "せまい", "あかるい", "くらいい", "あたまがいい", "かわいい",
    "かっこいい", "ねむい", "つよい", "よわい", "ほしい", "わかい", "ながい", "みじかい", "こまかい"
}
adverbs = {
    "あまり", "ぜんぜん", "いつも", "ときどき", "だいたい", "たくさん", "すこし", "ぜんぶ",
    "もっと", "ずっと", "はじめて", "また", "もう", "まだ", "これから", "そろそろ", "ゆっくり",
    "すぐ", "あとで", "まっすぐ", "なかなか", "ぜひ", "とくに", "たぶん", "きっと", "ほんとうに",
    "いちばん", "いっしょに", "ひとりで", "だんだん", "おおぜい"
}
conjunctions = {
    "そして", "それから", "でも", "しかし", "ですから", "それで", "それに", "ところで"
}
expressions = {
    "おはようございます", "こんにちは", "こんばんは", "おやすみなさい", "さようなら",
    "ありがとうございます", "どうも", "どういたしまして", "すみません", "ごめんなさい",
    "おねがいします", "どうぞ", "はじめまして", "どうぞよろしく", "よろしく",
    "いただきます", "ごちそうさまでした", "ごちそうさま", "ごめんください",
    "いってきます", "いってらっしゃい", "ただいま", "おかえりなさい",
    "おめでとうございます", "かんぱい", "いらっしゃいませ", "ようこそ"
}

vocab_rows = []
bab_vocab_counts = {}
for entry in vocab_data:
    bab = entry.get("bab")
    if bab is None or bab < 1 or bab > 25:
        continue
    v_id = f"vocab_{entry['id']:04d}"
    bab_vocab_counts[bab] = bab_vocab_counts.get(bab, 0) + 1
    badge = f"Bab {bab}"
    
    jap = entry["japanese"]
    furi = entry["furigana"]
    raw = entry.get("raw_display", "")
    indo = entry["indonesian"]
    
    # Classify word type
    if furi.endswith("ます") or jap.endswith("ます") or "〜ます" in raw or "ます" in raw.split("(")[0]:
        wtype = "KATA_KERJA"
    elif "[な]" in raw or "(な)" in raw or furi in na_adj_set or jap in na_adj_set:
        wtype = "KATA_SIFAT_NA"
    elif furi in i_adj_set or jap in i_adj_set or (furi.endswith("い") and any(indo.lower().startswith(p) for p in ["besar", "kecil", "baru", "lama", "panas", "dingin", "mahal", "murah", "tinggi", "rendah", "menarik", "enak", "sibuk", "senang", "putih", "hitam", "merah", "biru", "dekat", "jauh", "cepat", "lambat", "banyak", "sedikit", "hangat", "sejuk", "manis", "pedas", "berat", "ringan", "luas", "sempit", "terang", "gelap", "muda", "panjang", "pendek", "ingin"])):
        wtype = "KATA_SIFAT_I"
    elif furi in adverbs or jap in adverbs:
        wtype = "KATA_KETERANGAN"
    elif furi in conjunctions or jap in conjunctions:
        wtype = "KATA_SAMBUNG"
    elif furi in expressions or jap in expressions or (any(g in indo.lower() for g in ["selamat", "sampai jumpa", "terima kasih", "permisi", "maaf", "sama-sama"]) and "keselamatan" not in indo.lower()):
        wtype = "UNGKAPAN"
    elif furi in ["は", "が", "を", "に", "へ", "で", "と", "も", "から", "まで", "より", "や", "ね", "よ"]:
        wtype = "PARTIKEL"
    else:
        wtype = "KATA_BENDA"
        
    vocab_rows.append((
        v_id,
        "VOCABULARY",
        wtype,
        bab,
        "N5",
        jap,
        furi,
        entry.get("romaji", ""),
        indo,
        badge,
        None,
        entry["id"],
        json.dumps({
            "raw_display": entry.get("raw_display", jap),
            "source_bab": bab,
            "source": "CURRICULUM_CHAPTER"
        }, ensure_ascii=False)
    ))

cursor.executemany("""
INSERT INTO learning_objects (
    id, type, word_type, bab, level, japanese, reading, romaji, indonesian, badge_label, group_label, sort_order, details_json
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""", vocab_rows)

print(f"   -> Inserted {len(vocab_rows)} chapter vocabulary items.")

print("4. Ingesting Kanji Dataset (613 items)...")
kanji_file = ASSETS_DIR / "kanji_dataset.json"
with open(kanji_file, "r", encoding="utf-8") as f:
    kanji_data = json.load(f)

kanji_rows = []
for entry in kanji_data:
    k_id = f"kanji_{entry['id']:04d}"
    group_lbl = entry.get("group_label", "01–50")
    badge = f"Kanji #{entry['id']}"
    kanji_rows.append((
        k_id,
        "KANJI",
        "LAINNYA", # Kanji characters isolated from vocabulary nouns
        None,
        "N5",
        entry["kanji"],
        entry["reading"],
        entry.get("romaji", ""),
        entry["indonesian"],
        badge,
        group_lbl,
        entry["id"],
        json.dumps({
            "group": entry.get("group", 1),
            "group_label": group_lbl,
            "section": "KANJI"
        }, ensure_ascii=False)
    ))

cursor.executemany("""
INSERT INTO learning_objects (
    id, type, word_type, bab, level, japanese, reading, romaji, indonesian, badge_label, group_label, sort_order, details_json
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""", kanji_rows)

print(f"   -> Inserted {len(kanji_rows)} Kanji items.")

print("4b. Ingesting Additional Kanji Dataset (2,119 items in groups of 50)...")
add_kanji_file = ASSETS_DIR / "kanji_additional_dataset.json"
if add_kanji_file.exists():
    with open(add_kanji_file, "r", encoding="utf-8") as f:
        add_kanji_data = json.load(f)

    radical_kana_map = {
        '亻': ('にんべん', 'ninben', 'Radikal Orang (Varian 人)'),
        '耂': ('おいかんむり', 'oikanmuri', 'Radikal Tua (Varian 老)'),
        '巜': ('かわ', 'kawa', 'Radikal Aliran Sungai (Varian 川)'),
        '戶': ('と', 'to', 'Pintu, Radikal Rumah (Varian 戸)'),
        '户': ('と', 'to', 'Pintu, Rumah (Varian 戸)'),
        '歺': ('がつへん', 'gatsuhen', 'Radikal Tulang Hancur (Varian 歹)'),
        '爫': ('つめかんむり', 'tsumekanmuri', 'Radikal Cakar (Varian 爪)'),
        '丬': ('しょうへん', 'shouhen', 'Radikal Belahan Kayu (Varian 爿)'),
        '牜': ('うしへん', 'ushihen', 'Radikal Sapi (Varian 牛)'),
        '犭': ('けものへん', 'kemonohen', 'Radikal Hewan (Varian 犬)'),
        '玊': ('たま', 'tama', 'Batu Giok Berbintik (Varian 玉)'),
        '糹': ('いとへん', 'itohen', 'Radikal Benang (Varian 糸)'),
        '艹': ('くさかんむり', 'kusakanmuri', 'Radikal Rumput (Varian 草)'),
        '覀': ('にし', 'nishi', 'Radikal Barat / Penutup (Varian 西/襾)'),
        '訁': ('ごんべん', 'gonben', 'Radikal Kata / Ucapan (Varian 言)'),
        '釒': ('かねへん', 'kanehen', 'Radikal Logam / Emas (Varian 金)'),
        '靑': ('あお', 'ao', 'Biru / Muda (Bentuk Tradisional 青)'),
        '飠': ('しょくへん', 'shokuhen', 'Radikal Makanan (Varian 食)'),
        '髙': ('たかい', 'takai', 'Tinggi (Bentuk Tradisional 高)'),
        '鬥': ('とうがまえ', 'tougamae', 'Radikal Pertarungan (Varian 闘)'),
        '别': ('べつ', 'betsu', 'Pisah, Lain (Varian 別)')
    }

    add_rows = []
    for entry in add_kanji_data:
        k_id = entry["kanji_id"]
        group_lbl = entry["group_label"]
        badge = f"Tambahan #{entry['id']}"
        sort_order = 1000 + entry["id"]
        kj = entry["kanji"]
        if kj in radical_kana_map:
            rd, rm, indo = radical_kana_map[kj]
        else:
            rd = entry["reading"]
            rm = entry.get("romaji", "")
            indo = entry["indonesian"]
        add_rows.append((
            k_id,
            "KANJI",
            "LAINNYA",
            None,
            entry.get("jlpt", "Tambahan"),
            kj,
            rd,
            rm,
            indo,
            badge,
            group_lbl,
            sort_order,
            json.dumps({
                "section": "ADDITIONAL",
                "batch_index": entry["batch_index"],
                "source": entry["source"],
                "page": entry["page"],
                "occurrences": entry["occurrences"]
            }, ensure_ascii=False)
        ))
    cursor.executemany("""
    INSERT INTO learning_objects (
        id, type, word_type, bab, level, japanese, reading, romaji, indonesian, badge_label, group_label, sort_order, details_json
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, add_rows)
    print(f"   -> Inserted {len(add_rows)} Additional Kanji items.")

print("5. Ingesting Prioritized JFT Verbs (549 items in groups of 50)...")
verbs_file = ASSETS_DIR / "kata_kerja_dataset.json"
with open(verbs_file, "r", encoding="utf-8") as f:
    verbs_data = json.load(f)

verb_rows = []
for entry in verbs_data:
    v_id = f"verb_jft_{entry['id']:04d}"
    g_label = entry["group_label"]
    badge = f"Kata Kerja #{entry['id']}"
    verb_rows.append((
        v_id,
        "VOCABULARY",
        "KATA_KERJA",
        None,
        "N5",
        entry["japanese"],
        entry["reading"],
        entry.get("romaji", ""),
        entry["indonesian"],
        badge,
        g_label,
        entry["id"],
        json.dumps({
            "kanji": entry.get("kanji", ""),
            "group": entry["group"],
            "group_label": g_label,
            "source": "JFT_KATA_KERJA",
            "section": "KATA_KERJA"
        }, ensure_ascii=False)
    ))

cursor.executemany("""
INSERT INTO learning_objects (
    id, type, word_type, bab, level, japanese, reading, romaji, indonesian, badge_label, group_label, sort_order, details_json
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""", verb_rows)

print(f"   -> Inserted {len(verb_rows)} JFT Verb items.")

print("6. Ingesting Prioritized JFT Adjectives (173 items in groups of 50)...")
adj_file = ASSETS_DIR / "kata_sifat_dataset.json"
with open(adj_file, "r", encoding="utf-8") as f:
    adj_data = json.load(f)

adj_rows = []
for entry in adj_data:
    a_id = f"adj_jft_{entry['id']:04d}"
    g_label = entry["group_label"]
    wtype = entry["word_type"]
    badge = f"Kata Sifat #{entry['id']}"
    adj_rows.append((
        a_id,
        "VOCABULARY",
        wtype,
        None,
        "N5",
        entry["japanese"],
        entry["reading"],
        entry.get("romaji", ""),
        entry["indonesian"],
        badge,
        g_label,
        entry["id"],
        json.dumps({
            "kanji": entry.get("kanji", ""),
            "group": entry["group"],
            "group_label": g_label,
            "subgroup_label": entry.get("subgroup_label", ""),
            "source": "JFT_KATA_SIFAT",
            "section": "KATA_SIFAT"
        }, ensure_ascii=False)
    ))

cursor.executemany("""
INSERT INTO learning_objects (
    id, type, word_type, bab, level, japanese, reading, romaji, indonesian, badge_label, group_label, sort_order, details_json
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""", adj_rows)

print(f"   -> Inserted {len(adj_rows)} JFT Adjective items.")

print("7. Ingesting Grammar Patterns (Bab 1–25 IM JAPAN)...")
grammar_data = [
    # Bab 1
    (1, "〜は〜です", "Partikel Wa & Desu", "KB1 は KB2 です", "KB1 adalah KB2 (kalimat ekuatif sopan)",
     "わたしは インドネシアじんです。", "わたしは インドネシアじんです。", "Saya adalah orang Indonesia."),
    (1, "〜は〜じゃありません", "Sangkalan Santai/Sopan", "KB1 は KB2 じゃありません / ではありません", "KB1 bukan KB2 (bentuk sangkalan)",
     "サントスさんは がくせいじゃ ありません。", "サントスさんは がくせいじゃ ありません。", "Sdr. Santos bukan seorang pelajar."),
    (1, "〜は〜ですか", "Kalimat Tanya Partikel Ka", "Kalimat + か", "Apakah ...? (akhiran tanya non-intonasi)",
     "ミラさんは アメリカじんですか。", "ミラさんは アメリカじんですか。", "Apakah Sdr. Miller orang Amerika?"),
    (1, "〜の〜", "Partikel Kepemilikan & Kualifikasi", "KB1 の KB2", "KB2 milik KB1 / KB2 tentang KB1",
     "これは わたしのはんです。", "これは わたしのほんです。", "Ini adalah buku milik saya."),
    # Bab 2
    (2, "これ・それ・あれ", "Kata Ganti Penunjuk Benda", "これ／それ／あれ は KB です", "Ini (dekat pembicara) / Itu (dekat lawan) / Itu (jauh dari keduanya)",
     "これは じしょです。", "これは じしょです。", "Ini adalah kamus."),
    (2, "この・その・あの〜", "Penunjuk Benda Menerangkan Nomina", "この／その／あの + KB", "Benda ini / Benda itu",
     "この かさは あなたのですか。", "この かさは あなたのですか。", "Apakah payung ini kepunyaan Anda?"),
    (2, "〜そうです／〜そうじゃありません", "Konfirmasi Kesesuaian Fakta", "はい、そうです／いいえ、ちがいます", "Ya, benar demikian / Bukan, tidak benar",
     "それは テレホンカードですか。はい、そうです。", "それは テレホンカードですか。はい、そうです。", "Apakah itu kartu telepon? Ya, betul."),
    # Bab 3
    (3, "ここ・そこ・あそこ", "Kata Tunjuk Tempat", "ここ／そこ／あそこ は Tempat です", "Di sini / Di situ / Di sana adalah ...",
     "うけつけは あそこです。", "うけつけは あそこです。", "Bagian penerima tamu ada di sebelah sana."),
    (3, "どこ・どちら", "Kata Tanya Tempat & Arah Sopan", "Tempat/Fasilitas は どこ／どちら ですか", "Di mana ...? (Dochira lebih sopan/formal)",
     "おてあらいは どちらですか。", "おてあらいは どちらですか。", "Kamar kecil ada di sebelah mana?"),
    # Bab 4
    (4, "いま〜じ〜ふんです", "Menyatakan Jam dan Menit Waktu", "いま [Jam]じ [Menit]ふんです", "Sekarang jam ... lewat ... menit",
     "いま なんじですか。くじ はんです。", "いま なんじですか。くじ はんです。", "Sekarang jam berapa? Jam sembilan lewat tiga puluh menit."),
    (4, "〜ます／〜ません／〜ました／〜ませんでした", "Konjugasi Waktu Kata Kerja Masu", "KK-ます (Positif) / KK-ません (Negatif) / KK-ました (Lampau)", "Bentuk sopan aktivitas rutin/lampau",
     "まいあさ ６じに おきます。", "まいあさ ろくじに おきます。", "Setiap pagi saya bangun pada jam enam."),
    (4, "〜から〜まで", "Titik Awal dan Batas Akhir Rentang", "Titik/Waktu から Titik/Waktu まで", "Dari ... sampai/hingga ...",
     "ぎんこうは くじから さんじまでです。", "ぎんこうは くじから さんじまでです。", "Bank buka dari jam sembilan sampai jam tiga."),
    # Bab 5
    (5, "〜へ いきます／きます／かえります", "Arah Tujuan Perpindahan Gerak", "Tempat へ いきます／きます／かえります", "Pergi / Datang / Pulang ke (tempat)",
     "らいしゅう とうきょうへ いきます。", "らいしゅう とうきょうへ いきます。", "Minggu depan saya akan pergi ke Tokyo."),
    (5, "〜で いきます", "Sarana Transportasi Perpindahan", "Kendaraan で いきます", "Pergi dengan menaiki / menggunakan (alat transportasi)",
     "ひこうきで きょうとへ いきました。", "ひこうきで きょうとへ いきました。", "Saya telah pergi ke Kyoto dengan pesawat terbang."),
    (5, "〜と いきます", "Mitra Pengiring Aktivitas", "Orang/Teman と いきます", "Pergi bersama (orang lain)",
     "かぞくと にほんへ きました。", "かぞくと にほんへ きました。", "Saya datang ke Jepang bersama keluarga."),
    # Bab 6
    (6, "〜を KK-ます", "Partikel Objek Penderita Transitif", "Objek/Benda を KK-ます", "Melakukan aktivitas terhadap objek (makan, minum, beli, baca)",
     "ジュースを のみます。", "ジュースを のみます。", "Saya minum jus."),
    (6, "〜で KK-ます", "Tempat Berlangsungnya Aktivitas Dinamis", "Tempat で KK-ます", "Melakukan kegiatan di suatu lokasi",
     "えきで しんぶんを かいました。", "えきで しんぶんを かいました。", "Saya membeli koran di stasiun."),
    (6, "いっしょに〜ませんか／〜ましょう", "Ajakan Halus dan Kesepakatan Bersama", "KK-ませんか (Maukah bersama?) / KK-ましょう (Mari kita!)", "Bentuk ajakan ramah dan respon setuju",
     "いっしょに ひるごはんを たべませんか。", "いっしょに ひるごはんを たべませんか。", "Maukah Anda makan siang bersama saya?"),
    # Bab 7
    (7, "〜で たべます／きります", "Alat atau Sarana Tindakan", "Alat で KK-ます", "Melakukan sesuatu menggunakan peralatan",
     "はしで たべます。", "はしで たべます。", "Makan menggunakan sumpit."),
    (7, "〜は 〜ごで なんですか", "Padanan Kata Antar-Bahasa", "Kata は [Bahasa]ごで なんですか", "Apa sebutan kata ini dalam bahasa ...?",
     "「ありがとう」は えいごで なんですか。", "「ありがとう」は えいごで なんですか。", "Apa bahasa Inggris dari kata 'arigatou'?"),
    (7, "〜に あげます／もらいます", "Pemberian dan Penerimaan Objek", "Orang に Benda を あげます／もらいます", "Memberi kepada ... / Menerima dari ...",
     "きむらさんに はなを あげました。", "きむらさんに はなを あげました。", "Saya telah memberi bunga kepada Sdri. Kimura."),
    # Bab 8
    (8, "〜は い-形容詞／な-形容詞 です", "Predikat Kalimat Sifat Kondisi", "KB は Kata Sifat です", "Subjek memiliki sifat/karakteristik tersebut",
     "ふじさんは たかいです。", "ふじさんは たかいです。", "Gunung Fuji tinggi."),
    (8, "Kata Sifat Menerangkan Kata Benda", "Modifikasi Nomina Adjektiva", "い-KS + KB / な-KS + な + KB", "Frasa benda yang disifati",
     "これは きれいな はなです。", "これは きれいな はなです。", "Ini adalah bunga yang indah."),
    # Bab 9
    (9, "〜が すきです／きらいです／じょうずです", "Objek Minat, Kesukaan dan Kemahiran", "KB が すき／きらい／じょうず／へた です", "Menyukai / Tidak suka / Mahir dalam hal ...",
     "わたしは にほんりょうりが すきです。", "わたしは にほんりょうりが すきです。", "Saya menyukai masakan Jepang."),
    (9, "〜から、〜", "Penyebutan Alasan Kausalitas", "Kalimat Alasan から、Kalimat Hasil", "Karena / Oleh sebab itu ...",
     "じかんが ありませんから、タクシーで いきます。", "じかんが ありませんから、タクシーで いきます。", "Karena tidak ada waktu, saya akan pergi naik taksi."),
    # Bab 10
    (10, "〜に 〜が あります／います", "Keberadaan Benda Mati vs Makhluk Hidup", "Tempat に Benda が あります / Makhluk が います", "Ada (terdapat) benda/makhluk di tempat tersebut",
     "ロビーに さとうさんが います。", "ロビーに さとうさんが います。", "Ada Sdr. Sato di lobi."),
    (10, "Preposisi Spasial Relatif (うえ・した・まえ・うしろ)", "Lokasi Posisi Relatif", "KB1 の [Posisi] に KB2 が あります", "Di atas/bawah/depan/belakang benda tersebut",
     "つくえの うえに しゃしんが あります。", "つくえの うえに しゃしんが あります。", "Di atas meja ada foto."),
    # Bab 11
    (11, "Bilangan Satuan (Kata Bantu Hitung)", "Penempatan Kata Bantu Hitung", "KB を [Angka+Satuan] KK-ます", "Membeli/memiliki sejumlah unit barang",
     "りんごを みっつ かいました。", "りんごを みっつ かいました。", "Saya telah membeli tiga buah apel."),
    # Bab 12
    (12, "Perbandingan Dua Entitas (より／のほうが)", "Taraf Komparatif", "KB1 は KB2 より [Sifat] です", "KB1 lebih ... daripada KB2",
     "しんかんせんは ひこうきより はやいですか。", "しんかんせんは ひこうきより はやいですか。", "Apakah Shinkansen lebih cepat daripada pesawat?"),
    (12, "Bentuk Lampau Kata Sifat", "Predikat Adjektiva Masa Lampau", "い-KS: 〜かったです / な-KS: 〜でした", "Sifat/kondisi di masa lalu",
     "きのうの パーティーは たのしかったです。", "きのうの パーティーは たのしかったです。", "Pesta kemarin sangat menyenangkan."),
    # Bab 13
    (13, "〜が ほしいです", "Keinginan Memiliki Objek Nomina", "KB が ほしいです", "Saya menginginkan (benda)",
     "わたしは あたらしい くるまが ほしいです。", "わたしは あたらしい くるまが ほしいです。", "Saya menginginkan sebuah mobil baru."),
    (13, "〜たいです", "Keinginan Melakukan Suatu Tindakan", "KK(Bentuk Masu tanpa masu) + たいです", "Saya ingin (melakukan aktivitas)",
     "にほんへ いきたいです。", "にほんへ いきたいです。", "Saya ingin pergi ke Jepang."),
    # Bab 14
    (14, "〜てください", "Permohonan Instruksi Sopan Bentuk-TE", "KK(Bentuk-TE) + ください", "Tolong silakan lakukan ...",
     "ここに なまえを かいて ください。", "ここに なまえを かいて ください。", "Tolong tuliskan nama di sini."),
    (14, "〜ています (Sedang Berlangsung)", "Aspek Progresif Aktivitas Sekarang", "KK(Bentuk-TE) + います", "Sedang melakukan tindakan",
     "いま あめが ふって います。", "いま あめが ふって います。", "Sekarang sedang turun hujan."),
    # Bab 15
    (15, "〜ても いいです", "Pemberian Izin Formal", "KK(Bentuk-TE) + も いいです", "Boleh (diizinkan) melakukan ...",
     "ここで しゃしんを とっても いいですか。", "ここで しゃしんを とっても いいですか。", "Bolehkah mengambil foto di sini?"),
    (15, "〜ては いけません", "Larangan Tegas Aturan Hukum", "KK(Bentuk-TE) + は いけません", "Tidak boleh / Dilarang melakukan ...",
     "ここで たばこを すっては いけません。", "ここで たばこを すっては いけません。", "Dilarang merokok di sini."),
    # Bab 16
    (16, "Rangkaian Urutan Tindakan Bentuk-TE", "Kronologi Beruntun Tindakan", "KK1-て、KK2-て、KK3-ます", "Melakukan KK1, lalu KK2, lalu KK3",
     "あさ おきて、さんぽして、シャワーを あびました。", "あさ おきて、さんぽして、シャワーを あびました。", "Pagi bangun tidur, berjalan-jalan, lalu mandi air hangat."),
    # Bab 17
    (17, "〜なければ なりません", "Kewajiban Mutlak Bentuk-NAI", "KK(Bentuk-NAI tanpa i) + ければ なりません", "Harus / Wajib melakukan ...",
     "くすりを のまなければ なりません。", "くすりを のまなければ なりません。", "Harus minum obat."),
    # Bab 18
    (18, "〜ことが できます", "Potensi Kemampuan Bentuk Kamus", "KK(Bentuk Kamus/Jishokei) + ことが できます", "Dapat / Mampu / Bisa melakukan ...",
     "ミラーさんは かんじを よむことが できます。", "ミラーさんは かんじを よむことが できます。", "Sdr. Miller dapat membaca huruf Kanji."),
    # Bab 19
    (19, "〜たことが あります", "Riwayat Pengalaman Masa Lalu", "KK(Bentuk-TA) + ことが あります", "Pernah mengalami / melakukan ...",
     "うまに のったことが あります。", "うまに のったことが あります。", "Saya pernah menunggang kuda."),
    # Bab 20
    (20, "Percakapan Gaya Biasa (Futsuukei)", "Gaya Santai Kasual Antar-Sahabat", "Bentuk Kamus, Bentuk-NAI, Bentuk-TA tanpa desu/masu", "Bahasa akrab dalam situasi santai",
     "あした とうきょうへ いく？ ううん、いかない。", "あした とうきょうへ いく？ ううん、いかない。", "Besok pergi ke Tokyo? Nggak, nggak pergi."),
    # Bab 21
    (21, "〜と おもいます", "Pengutaraan Opini dan Dugaan", "Kalimat Bentuk Biasa + と おもいます", "Saya kira / Menurut hemat saya bahwa ...",
     "あしたは あめが ふると おもいます。", "あしたは あめが ふると おもいます。", "Saya mengira besok akan hujan."),
    # Bab 22
    (22, "Modifikasi Nomina Kalimat Penjelas", "Klausa Relatif Menerangkan Benda", "Klausa Bentuk Biasa + Kata Benda", "Benda yang di-[kata kerja]-kan",
     "これは わたしが つくった りょうりです。", "これは わたしが つくった りょうりです。", "Ini adalah masakan yang saya buat."),
    # Bab 23
    (23, "〜とき、〜", "Penetapan Temporal Waktu Kejadian", "KK/KS/KB + とき、〜", "Pada waktu / Saat / Ketika ...",
     "みちを わたるとき、くるまに きを つけます。", "みちを わたるとき、くるまに きを つけます。", "Ketika menyeberang jalan, berhati-hatilah terhadap mobil."),
    # Bab 24
    (24, "Pemberian & Penerimaan Jasa Bantuan", "Ekspresi Nilai Budaya Berbalas Budi", "KK(Bentuk-TE) + もらいます／くれます", "Mendapat pertolongan dari ... / Ditolong oleh ...",
     "ともだちが ひっこしを てつだって くれました。", "ともだちが ひっこしを てつだって くれました。", "Teman saya telah membantu proses pindah rumah saya."),
    # Bab 25
    (25, "〜たら、〜", "Kondisional Pengandaian & Waktu Pasti", "KK/KS(Bentuk-TA) + ら、〜", "Kalau / Jika / Seandainya ...",
     "あした あめが ふったら、でかけません。", "あした あめが ふったら、でかけません。", "Kalau besok hujan, saya tidak akan bepergian.")
]

grammar_rows = []
example_rows = []
bab_grammar_counts = {}
for idx, item in enumerate(grammar_data, 1):
    bab, pattern, name, struct, expl, ex_ja, ex_reading, ex_id = item
    bab_grammar_counts[bab] = bab_grammar_counts.get(bab, 0) + 1
    g_id = f"grammar_{idx:04d}"
    badge = f"Bab {bab}"
    details = {
        "structure": struct,
        "explanation": expl,
        "source_bab": bab,
        "section": "GRAMMAR"
    }
    grammar_rows.append((
        g_id,
        "GRAMMAR",
        "LAINNYA",
        bab,
        "N5",
        pattern,
        pattern,
        "",
        name,
        badge,
        None,
        idx,
        json.dumps(details, ensure_ascii=False)
    ))
    example_rows.append((
        g_id,
        ex_ja,
        ex_reading,
        ex_id
    ))

cursor.executemany("""
INSERT INTO learning_objects (
    id, type, word_type, bab, level, japanese, reading, romaji, indonesian, badge_label, group_label, sort_order, details_json
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""", grammar_rows)

cursor.executemany("""
INSERT INTO examples (
    object_id, japanese, reading, indonesian
) VALUES (?, ?, ?, ?)
""", example_rows)

print(f"   -> Inserted {len(grammar_rows)} Grammar patterns and examples.")

print("8. Updating Chapter counts...")
for bab in range(1, 26):
    vc = bab_vocab_counts.get(bab, 0)
    gc = bab_grammar_counts.get(bab, 0)
    cursor.execute("UPDATE chapters SET vocab_count = ?, grammar_count = ? WHERE bab_number = ?", (vc, gc, bab))

print("9. Seeding initial user progress records for ALL learning objects...")
cursor.execute("""
INSERT INTO user_progress (object_id, interval_days, ease_factor, repetitions, lapses, stability, retrievability, last_response_time_ms, last_review_epoch, next_review_epoch, mastery_state)
SELECT id, 0, 2.5, 0, 0, 0.0, 1.0, 0, 0, 0, 'UNSEEN' FROM learning_objects;
""")

conn.commit()

# Print statistics summary
cursor.execute("SELECT count(*) FROM learning_objects")
total_lo = cursor.fetchone()[0]
cursor.execute("SELECT count(*) FROM learning_objects WHERE type = 'VOCABULARY' AND bab IS NOT NULL")
curr_vocab = cursor.fetchone()[0]
cursor.execute("SELECT count(*) FROM learning_objects WHERE type = 'KANJI'")
total_kanji = cursor.fetchone()[0]
cursor.execute("SELECT count(*) FROM learning_objects WHERE type = 'GRAMMAR'")
total_grammar = cursor.fetchone()[0]
cursor.execute("SELECT count(*) FROM learning_objects WHERE id LIKE 'verb_jft_%'")
jft_verbs = cursor.fetchone()[0]
cursor.execute("SELECT count(*) FROM learning_objects WHERE id LIKE 'adj_jft_%'")
jft_adjs = cursor.fetchone()[0]
cursor.execute("SELECT count(*) FROM user_progress")
total_prog = cursor.fetchone()[0]

# 10. Data Integrity Audit Assertions
cursor.execute("SELECT id, japanese, reading, romaji, indonesian FROM learning_objects WHERE type IN ('VOCABULARY', 'KANJI') AND (reading IS NULL OR TRIM(reading) = '' OR reading = '—')")
empty_readings = cursor.fetchall()
assert len(empty_readings) == 0, f"Integrity Failure: Found {len(empty_readings)} empty readings in database!"

cursor.execute("SELECT id, japanese, reading, romaji FROM learning_objects WHERE romaji LIKE '%も%' OR romaji GLOB '*[一-龯ぁ-んァ-ン]*'")
corrupt_romaji = cursor.fetchall()
assert len(corrupt_romaji) == 0, f"Integrity Failure: Found {len(corrupt_romaji)} corrupted romaji entries!"

cursor.execute("SELECT japanese, reading, romaji FROM learning_objects WHERE id = 'kanji_add_0701'")
k701 = cursor.fetchone()
assert k701 is not None and k701[0] == '釣' and k701[1] == 'つり' and k701[2] == 'tsuri', f"Integrity Failure: Kanji 701 mismatch: {k701}"

print("   -> Data Integrity Assertions: PASSED (0 empty readings, 0 corrupt romaji, 701 verified).")

conn.close()

# Sync to iOS
for ios_root in [PROJECT_ROOT / "ios" / "Kotoba" / "Resources", Path("/root/projects/GERNI/ios/Kotoba/Resources")]:
    if ios_root.exists():
        import shutil
        target = ios_root / "kotoba.db"
        shutil.copy2(DB_PATH, target)
        print(f"   -> Synchronized SQLite database to iOS: {target}")

db_size = os.path.getsize(DB_PATH)
print("============================================================")
print(f"DATABASE GENERATION COMPLETE: {DB_PATH} ({db_size:,} bytes)")
print(f"Total Learning Objects: {total_lo:,}")
print(f"  - Curriculum Vocabulary (Bab 1..25): {curr_vocab}")
print(f"  - Prioritized JFT Verbs (groups 50): {jft_verbs}")
print(f"  - Prioritized JFT Adjectives (groups 50): {jft_adjs}")
print(f"  - Kanji Flashcards (groups 50): {total_kanji}")
print(f"  - Grammar Patterns: {total_grammar}")
print(f"Total User Progress Records: {total_prog:,}")
print("============================================================")
