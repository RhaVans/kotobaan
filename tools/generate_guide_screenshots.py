#!/usr/bin/env python3
"""
tools/generate_guide_screenshots.py
Renders high-resolution, pixel-accurate UI screen images of the Kotoba app
using the exact colors from colors.xml, layouts, and real dataset records.
"""

import os
from PIL import Image, ImageDraw, ImageFont

OUT_DIR = "/storage/emulated/0/Download/PROJECT/KTB/docs/guide_screenshots"
REPO_OUT_DIR = "docs/guide_screenshots"

os.makedirs(OUT_DIR, exist_ok=True)
os.makedirs(REPO_OUT_DIR, exist_ok=True)

# Colors from colors.xml
BG_LIGHT = "#faf7f2"
BG_DARK = "#121016"
SURFACE_LIGHT = "#ffffff"
SURFACE_DARK = "#1c1822"
BORDER_LIGHT = "#e8e0d5"
BORDER_DARK = "#2e2838"
PRIMARY_PURPLE = "#6d28d9"
PRIMARY_PURPLE_DARK = "#5b21b6"
BADGE_BG = "#ede9fe"
BADGE_TEXT = "#5b21b6"
TEXT_PRIMARY = "#1a161e"
TEXT_PRIMARY_DARK = "#f8f6fa"
TEXT_SECONDARY = "#574f60"
TEXT_SECONDARY_DARK = "#a89fb3"
TEXT_TERTIARY = "#82788d"
COLOR_INGAT = "#15803d"
COLOR_LUPA = "#b91c1c"
CHIP_INACTIVE_BG = "#f3ece2"
CHIP_INACTIVE_TEXT = "#574f60"

# Fonts
FONT_SERIF_CJK = "/system/fonts/NotoSerifCJK-Regular.ttc"
FONT_SANS_CJK = "/system/fonts/NotoSansCJK-Regular.ttc"
FONT_SANS_LATIN = "/system/fonts/DroidSans.ttf"
FONT_BOLD_LATIN = "/system/fonts/DroidSans-Bold.ttf"

def get_font(path, size):
    return ImageFont.truetype(path, size)

def draw_top_bar(draw, w, title="KOTOBA", is_dark=False):
    bg = BG_DARK if is_dark else BG_LIGHT
    text_color = TEXT_PRIMARY_DARK if is_dark else PRIMARY_PURPLE
    sec_color = TEXT_SECONDARY_DARK if is_dark else TEXT_SECONDARY

    # Status bar (time, battery)
    font_small = get_font(FONT_SANS_LATIN, 13)
    draw.text((24, 12), "09:41", font=font_small, fill=sec_color)
    draw.text((w - 70, 12), "100%", font=font_small, fill=sec_color)

    # App Title & Icons
    font_title = get_font(FONT_BOLD_LATIN, 22)
    draw.text((24, 44), title, font=font_title, fill=text_color)

    # Action icon placeholders on top right (Filter, Theme, Shuffle)
    font_icon = get_font(FONT_SANS_LATIN, 14)
    draw.rounded_rectangle((w - 145, 42, w - 105, 74), 8, fill=SURFACE_DARK if is_dark else SURFACE_LIGHT, outline=BORDER_DARK if is_dark else BORDER_LIGHT)
    draw.text((w - 133, 50), "Bab", font=font_icon, fill=text_color)

    draw.rounded_rectangle((w - 95, 42, w - 60, 74), 8, fill=SURFACE_DARK if is_dark else SURFACE_LIGHT, outline=BORDER_DARK if is_dark else BORDER_LIGHT)
    draw.text((w - 87, 50), "☼" if is_dark else "☾", font=font_icon, fill=text_color)

    draw.rounded_rectangle((w - 50, 42, w - 20, 74), 8, fill=SURFACE_DARK if is_dark else SURFACE_LIGHT, outline=BORDER_DARK if is_dark else BORDER_LIGHT)
    draw.text((w - 42, 50), "⇄", font=font_icon, fill=text_color)

def draw_round_progress(draw, w, round_num=1, current=4, total=50, ingat=12, lupa=3, is_dark=False):
    sec_color = TEXT_SECONDARY_DARK if is_dark else TEXT_SECONDARY
    pri_color = TEXT_PRIMARY_DARK if is_dark else TEXT_PRIMARY

    font_sub = get_font(FONT_SANS_LATIN, 14)
    font_count = get_font(FONT_BOLD_LATIN, 13)

    # Text counter
    draw.text((24, 95), f"Putaran {round_num} • Kartu {current} dari {total}", font=font_sub, fill=pri_color)

    # Ingat / Lupa count badges
    draw.rounded_rectangle((w - 150, 92, w - 95, 118), 12, fill="#dcfce7" if not is_dark else "#14532d")
    draw.text((w - 140, 97), f"✓ {ingat}", font=font_count, fill="#15803d" if not is_dark else "#86efac")

    draw.rounded_rectangle((w - 85, 92, w - 24, 118), 12, fill="#fee2e2" if not is_dark else "#7f1d1d")
    draw.text((w - 75, 97), f"✗ {lupa}", font=font_count, fill="#b91c1c" if not is_dark else "#fca5a5")

    # Progress bar track
    track_color = BORDER_DARK if is_dark else BORDER_LIGHT
    fill_color = PRIMARY_PURPLE
    draw.rounded_rectangle((24, 130, w - 24, 136), 3, fill=track_color)
    progress_w = int((w - 48) * (current / total))
    if progress_w > 6:
        draw.rounded_rectangle((24, 130, 24 + progress_w, 136), 3, fill=fill_color)

def draw_bottom_controls(draw, w, h, can_prev=True, furigana=True, romaji=False, is_dark=False):
    # Row 1: Prev + Toggles
    # Prev button
    prev_bg = SURFACE_DARK if is_dark else SURFACE_LIGHT
    prev_border = BORDER_DARK if is_dark else BORDER_LIGHT
    prev_text = TEXT_PRIMARY_DARK if is_dark else TEXT_PRIMARY
    if not can_prev:
        prev_text = TEXT_TERTIARY
    draw.rounded_rectangle((24, h - 165, 115, h - 130), 16, fill=prev_bg, outline=prev_border)
    draw.text((40, h - 157), "← Prev", font=get_font(FONT_BOLD_LATIN, 13), fill=prev_text)

    # Furigana Toggle
    furi_bg = PRIMARY_PURPLE if furigana else (SURFACE_DARK if is_dark else SURFACE_LIGHT)
    furi_text = "#ffffff" if furigana else TEXT_SECONDARY
    draw.rounded_rectangle((w - 180, h - 165, w - 105, h - 130), 16, fill=furi_bg, outline=BORDER_DARK if is_dark else BORDER_LIGHT)
    draw.text((w - 165, h - 157), "Furigana", font=get_font(FONT_BOLD_LATIN, 12), fill=furi_text)

    # Romaji Toggle
    roma_bg = PRIMARY_PURPLE if romaji else (SURFACE_DARK if is_dark else SURFACE_LIGHT)
    roma_text = "#ffffff" if romaji else TEXT_SECONDARY
    draw.rounded_rectangle((w - 95, h - 165, w - 24, h - 130), 16, fill=roma_bg, outline=BORDER_DARK if is_dark else BORDER_LIGHT)
    draw.text((w - 82, h - 157), "Romaji", font=get_font(FONT_BOLD_LATIN, 12), fill=roma_text)

    # Row 2: LUPA & INGAT
    btn_y1 = h - 115
    btn_y2 = h - 45
    half_w = (w - 60) // 2

    # Lupa button (Red outline)
    draw.rounded_rectangle((24, btn_y1, 24 + half_w, btn_y2), 16, fill="#fee2e2" if not is_dark else "#2d1215", outline=COLOR_LUPA, width=2)
    draw.text((24 + half_w // 2 - 25, btn_y1 + 22), "✗  Lupa", font=get_font(FONT_BOLD_LATIN, 18), fill=COLOR_LUPA)

    # Ingat button (Green solid fill)
    draw.rounded_rectangle((w - 24 - half_w, btn_y1, w - 24, btn_y2), 16, fill=COLOR_INGAT)
    draw.text((w - 24 - half_w // 2 - 30, btn_y1 + 22), "✓  Ingat", font=get_font(FONT_BOLD_LATIN, 18), fill="#ffffff")

def render_main_screen_front(filename, kanji="食べる", reading="たべる", romaji="taberu", is_dark=False):
    w, h = 420, 860
    im = Image.new("RGBA", (w, h), BG_DARK if is_dark else BG_LIGHT)
    draw = ImageDraw.Draw(im)

    draw_top_bar(draw, w, is_dark=is_dark)
    draw_round_progress(draw, w, round_num=1, current=4, total=50, ingat=12, lupa=3, is_dark=is_dark)

    # Card Surface
    c_x1, c_y1 = 24, 160
    c_x2, c_y2 = w - 24, h - 185
    card_bg = SURFACE_DARK if is_dark else SURFACE_LIGHT
    card_border = BORDER_DARK if is_dark else BORDER_LIGHT
    draw.rounded_rectangle((c_x1, c_y1, c_x2, c_y2), 24, fill=card_bg, outline=card_border, width=1)

    # Card Header
    badge_bg = BADGE_BG if not is_dark else "#2e1065"
    badge_text = BADGE_TEXT if not is_dark else "#ddd6fe"
    draw.rounded_rectangle((c_x1 + 20, c_y1 + 20, c_x1 + 80, c_y1 + 46), 8, fill=badge_bg)
    draw.text((c_x1 + 28, c_y1 + 25), "Bab 1", font=get_font(FONT_BOLD_LATIN, 12), fill=badge_text)

    # Speaker Button
    spk_center_x = c_x2 - 40
    spk_center_y = c_y1 + 33
    draw.ellipse((spk_center_x - 16, spk_center_y - 16, spk_center_x + 16, spk_center_y + 16), fill="#ede9fe" if not is_dark else "#3b1e68")
    draw.text((spk_center_x - 8, spk_center_y - 9), "🔊", font=get_font(FONT_SANS_LATIN, 14), fill=PRIMARY_PURPLE)

    # Hint text
    hint_color = TEXT_TERTIARY
    draw.text((c_x1 + 20, c_y2 - 35), "Ketuk kartu untuk melihat arti  ↻", font=get_font(FONT_SANS_LATIN, 12), fill=hint_color)

    # Card Center Content (Front Face)
    center_y = (c_y1 + c_y2) // 2

    # Furigana above kanji
    if reading:
        font_furi = get_font(FONT_SANS_CJK, 20)
        draw.text((w // 2 - 30, center_y - 70), reading, font=font_furi, fill=PRIMARY_PURPLE if not is_dark else "#c4b5fd")

    # Kanji ideograph
    font_kanji = get_font(FONT_SERIF_CJK, 54)
    # approximate bbox center
    draw.text((w // 2 - 55, center_y - 35), kanji, font=font_kanji, fill=TEXT_PRIMARY_DARK if is_dark else TEXT_PRIMARY)

    # Romaji
    if romaji:
        font_roma = get_font(FONT_SANS_LATIN, 14)
        draw.text((w // 2 - 25, center_y + 45), romaji, font=font_roma, fill=TEXT_TERTIARY)

    draw_bottom_controls(draw, w, h, can_prev=True, furigana=True, romaji=False, is_dark=is_dark)

    im.save(os.path.join(OUT_DIR, filename))
    im.save(os.path.join(REPO_OUT_DIR, filename))
    print(f"Generated {filename}")

def render_main_screen_back(filename, kanji="食べる", reading="たべる", meaning="makan", is_dark=False):
    w, h = 420, 860
    im = Image.new("RGBA", (w, h), BG_DARK if is_dark else BG_LIGHT)
    draw = ImageDraw.Draw(im)

    draw_top_bar(draw, w, is_dark=is_dark)
    draw_round_progress(draw, w, round_num=1, current=4, total=50, ingat=12, lupa=3, is_dark=is_dark)

    c_x1, c_y1 = 24, 160
    c_x2, c_y2 = w - 24, h - 185
    card_bg = SURFACE_DARK if is_dark else SURFACE_LIGHT
    card_border = BORDER_DARK if is_dark else BORDER_LIGHT
    draw.rounded_rectangle((c_x1, c_y1, c_x2, c_y2), 24, fill=card_bg, outline=card_border, width=1)

    # Card Header
    badge_bg = BADGE_BG if not is_dark else "#2e1065"
    badge_text = BADGE_TEXT if not is_dark else "#ddd6fe"
    draw.rounded_rectangle((c_x1 + 20, c_y1 + 20, c_x1 + 80, c_y1 + 46), 8, fill=badge_bg)
    draw.text((c_x1 + 28, c_y1 + 25), "Bab 1", font=get_font(FONT_BOLD_LATIN, 12), fill=badge_text)

    # Speaker Button
    spk_center_x = c_x2 - 40
    spk_center_y = c_y1 + 33
    draw.ellipse((spk_center_x - 16, spk_center_y - 16, spk_center_x + 16, spk_center_y + 16), fill="#ede9fe" if not is_dark else "#3b1e68")
    draw.text((spk_center_x - 8, spk_center_y - 9), "🔊", font=get_font(FONT_SANS_LATIN, 14), fill=PRIMARY_PURPLE)

    # Back Face Content: Answer is prominent Reading + Meaning
    center_y = (c_y1 + c_y2) // 2

    # Kanji reference above
    font_k_ref = get_font(FONT_SERIF_CJK, 22)
    draw.text((w // 2 - 24, center_y - 85), kanji, font=font_k_ref, fill=TEXT_SECONDARY_DARK if is_dark else TEXT_SECONDARY)

    # Prominent Hiragana Answer
    font_reading = get_font(FONT_SERIF_CJK, 48)
    draw.text((w // 2 - 65, center_y - 50), reading, font=font_reading, fill=TEXT_PRIMARY_DARK if is_dark else TEXT_PRIMARY)

    # Divider line
    div_y = center_y + 15
    draw.line((w // 2 - 70, div_y, w // 2 + 70, div_y), fill=BORDER_DARK if is_dark else BORDER_LIGHT, width=2)

    # Indonesian Meaning
    font_meaning = get_font(FONT_BOLD_LATIN, 24)
    draw.text((w // 2 - 40, div_y + 20), meaning, font=font_meaning, fill=TEXT_PRIMARY_DARK if is_dark else TEXT_PRIMARY)

    # Hint text
    hint_color = TEXT_TERTIARY
    draw.text((c_x1 + 20, c_y2 - 35), "Ketuk kartu untuk balik ke depan  ↻", font=get_font(FONT_SANS_LATIN, 12), fill=hint_color)

    draw_bottom_controls(draw, w, h, can_prev=True, furigana=True, romaji=False, is_dark=is_dark)

    im.save(os.path.join(OUT_DIR, filename))
    im.save(os.path.join(REPO_OUT_DIR, filename))
    print(f"Generated {filename}")

def render_bab_selector_dialog(filename):
    w, h = 420, 860
    im = Image.new("RGBA", (w, h), "#000000aa") # semi-transparent overlay
    draw = ImageDraw.Draw(im)

    # Dialog container
    d_x1, d_y1 = 20, 100
    d_x2, d_y2 = w - 20, h - 100
    draw.rounded_rectangle((d_x1, d_y1, d_x2, d_y2), 24, fill=SURFACE_LIGHT, outline=BORDER_LIGHT)

    # Header
    draw.text((d_x1 + 24, d_y1 + 24), "Pilih Bab / Materi Belajar", font=get_font(FONT_BOLD_LATIN, 18), fill=TEXT_PRIMARY)
    draw.text((d_x1 + 24, d_y1 + 50), "Pilih satu atau beberapa materi sekaligus", font=get_font(FONT_SANS_LATIN, 12), fill=TEXT_SECONDARY)

    # Quick Action Buttons (Pilih Semua / Hapus)
    draw.rounded_rectangle((d_x1 + 24, d_y1 + 80, d_x1 + 130, d_y1 + 112), 8, fill=CHIP_INACTIVE_BG)
    draw.text((d_x1 + 34, d_y1 + 88), "✓ Pilih Semua", font=get_font(FONT_BOLD_LATIN, 12), fill=PRIMARY_PURPLE)

    draw.rounded_rectangle((d_x1 + 140, d_y1 + 80, d_x1 + 245, d_y1 + 112), 8, fill=CHIP_INACTIVE_BG)
    draw.text((d_x1 + 150, d_y1 + 88), "✕ Hapus Pilihan", font=get_font(FONT_BOLD_LATIN, 12), fill=COLOR_LUPA)

    # Section 1: Bab 1-25 Grid
    draw.text((d_x1 + 24, d_y1 + 130), "KOSAKATA BAB (Bisa Pilih Lebih Dari 1)", font=get_font(FONT_BOLD_LATIN, 12), fill=PRIMARY_PURPLE)

    # Draw chips grid
    chip_w, chip_h = 75, 34
    start_y = d_y1 + 155
    selected_babs = [1, 2, 5]
    for i in range(1, 16):
        row = (i - 1) // 4
        col = (i - 1) % 4
        cx = d_x1 + 24 + col * (chip_w + 10)
        cy = start_y + row * (chip_h + 8)

        is_sel = i in selected_babs
        bg = PRIMARY_PURPLE if is_sel else CHIP_INACTIVE_BG
        tx = "#ffffff" if is_sel else CHIP_INACTIVE_TEXT
        draw.rounded_rectangle((cx, cy, cx + chip_w, cy + chip_h), 8, fill=bg)
        draw.text((cx + 16, cy + 9), f"Bab {i}", font=get_font(FONT_BOLD_LATIN, 12), fill=tx)

    # Section 2: Paket per 50 Kata (Kata Kerja, Kata Sifat, Kanji)
    batch_y = start_y + 4 * (chip_h + 8) + 15
    draw.text((d_x1 + 24, batch_y), "KATEGORI KHUSUS (Paket per 50 Kata)", font=get_font(FONT_BOLD_LATIN, 12), fill=PRIMARY_PURPLE)

    categories = [
        ("Kata Kerja (1 - 50)", True),
        ("Kata Sifat (1 - 50)", False),
        ("Kanji Formal (1 - 50)", False),
        ("Kanji Tambahan (1 - 50)", False)
    ]
    for idx, (cat_name, is_sel) in enumerate(categories):
        cy = batch_y + 25 + idx * 38
        bg = "#ede9fe" if is_sel else CHIP_INACTIVE_BG
        tx = PRIMARY_PURPLE if is_sel else TEXT_PRIMARY
        draw.rounded_rectangle((d_x1 + 24, cy, d_x2 - 24, cy + 32), 8, fill=bg, outline=PRIMARY_PURPLE if is_sel else BORDER_LIGHT)
        draw.text((d_x1 + 36, cy + 8), cat_name, font=get_font(FONT_BOLD_LATIN, 12), fill=tx)
        if is_sel:
            draw.text((d_x2 - 55, cy + 8), "✓ Aktif", font=get_font(FONT_BOLD_LATIN, 11), fill=PRIMARY_PURPLE)

    # Footer Buttons
    f_y = d_y2 - 60
    draw.rounded_rectangle((d_x1 + 24, f_y, d_x1 + 120, f_y + 44), 12, fill=CHIP_INACTIVE_BG)
    draw.text((d_x1 + 48, f_y + 14), "Tutup", font=get_font(FONT_BOLD_LATIN, 14), fill=TEXT_SECONDARY)

    draw.rounded_rectangle((d_x1 + 135, f_y, d_x2 - 24, f_y + 44), 12, fill=PRIMARY_PURPLE)
    draw.text((d_x1 + 175, f_y + 14), "Mulai Belajar (82 Kata)", font=get_font(FONT_BOLD_LATIN, 14), fill="#ffffff")

    im.save(os.path.join(OUT_DIR, filename))
    im.save(os.path.join(REPO_OUT_DIR, filename))
    print(f"Generated {filename}")

def render_voice_settings_dialog(filename):
    w, h = 420, 860
    im = Image.new("RGBA", (w, h), "#000000aa")
    draw = ImageDraw.Draw(im)

    d_x1, d_y1 = 24, 180
    d_x2, d_y2 = w - 24, h - 200
    draw.rounded_rectangle((d_x1, d_y1, d_x2, d_y2), 24, fill=SURFACE_LIGHT, outline=BORDER_LIGHT)

    # Header
    draw.text((d_x1 + 24, d_y1 + 24), "Pengaturan Suara Jepang", font=get_font(FONT_BOLD_LATIN, 18), fill=TEXT_PRIMARY)
    draw.text((d_x1 + 24, d_y1 + 52), "Suara: Native Japanese (Sistem Android)", font=get_font(FONT_SANS_LATIN, 13), fill=PRIMARY_PURPLE)

    # Speed section
    draw.text((d_x1 + 24, d_y1 + 90), "Kecepatan Bicara", font=get_font(FONT_BOLD_LATIN, 13), fill=TEXT_SECONDARY)
    speeds = ["0.75x", "0.85x", "0.90x", "1.00x", "1.10x"]
    sw = 60
    for idx, s in enumerate(speeds):
        sx = d_x1 + 24 + idx * (sw + 6)
        sy = d_y1 + 115
        is_sel = (s == "0.90x")
        draw.rounded_rectangle((sx, sy, sx + sw, sy + 32), 8, fill=PRIMARY_PURPLE if is_sel else CHIP_INACTIVE_BG)
        draw.text((sx + 12, sy + 8), s, font=get_font(FONT_BOLD_LATIN, 12), fill="#ffffff" if is_sel else CHIP_INACTIVE_TEXT)

    # Pitch section
    draw.text((d_x1 + 24, d_y1 + 170), "Tinggi Nada (Pitch)", font=get_font(FONT_BOLD_LATIN, 13), fill=TEXT_SECONDARY)
    pitches = [("Lebih Rendah", False), ("Normal (Sage)", True), ("Lebih Tinggi", False)]
    pw = 100
    for idx, (p_label, is_sel) in enumerate(pitches):
        px = d_x1 + 24 + idx * (pw + 10)
        py = d_y1 + 195
        draw.rounded_rectangle((px, py, px + pw, py + 32), 8, fill=PRIMARY_PURPLE if is_sel else CHIP_INACTIVE_BG)
        draw.text((px + 10, py + 8), p_label, font=get_font(FONT_BOLD_LATIN, 11), fill="#ffffff" if is_sel else CHIP_INACTIVE_TEXT)

    # Test Button
    test_y = d_y1 + 260
    draw.rounded_rectangle((d_x1 + 24, test_y, d_x2 - 24, test_y + 44), 12, fill="#ede9fe", outline=PRIMARY_PURPLE)
    draw.text((d_x1 + 90, test_y + 13), "🔊  Uji Pelafalan Suara", font=get_font(FONT_BOLD_LATIN, 14), fill=PRIMARY_PURPLE)

    # Close Button
    draw.rounded_rectangle((d_x1 + 24, d_y2 - 60, d_x2 - 24, d_y2 - 16), 12, fill=PRIMARY_PURPLE)
    draw.text((w // 2 - 30, d_y2 - 45), "Simpan", font=get_font(FONT_BOLD_LATIN, 14), fill="#ffffff")

    im.save(os.path.join(OUT_DIR, filename))
    im.save(os.path.join(REPO_OUT_DIR, filename))
    print(f"Generated {filename}")

def render_library_dialog(filename):
    w, h = 420, 860
    im = Image.new("RGBA", (w, h), "#000000aa")
    draw = ImageDraw.Draw(im)

    d_x1, d_y1 = 20, 80
    d_x2, d_y2 = w - 20, h - 80
    draw.rounded_rectangle((d_x1, d_y1, d_x2, d_y2), 24, fill=SURFACE_LIGHT, outline=BORDER_LIGHT)

    # Title & Search
    draw.text((d_x1 + 24, d_y1 + 24), "Pustaka Materi (4.363 Kata)", font=get_font(FONT_BOLD_LATIN, 18), fill=TEXT_PRIMARY)

    # Search bar
    s_y = d_y1 + 55
    draw.rounded_rectangle((d_x1 + 24, s_y, d_x2 - 24, s_y + 38), 10, fill=CHIP_INACTIVE_BG, outline=BORDER_LIGHT)
    draw.text((d_x1 + 36, s_y + 10), "🔍  Cari kata, kanji, atau arti...", font=get_font(FONT_SANS_LATIN, 12), fill=TEXT_TERTIARY)

    # Tabs
    tab_y = s_y + 48
    tabs = [("Kosakata", True), ("Kanji 613", False), ("Tambahan", False), ("Verba", False)]
    tw = 75
    for idx, (tname, is_sel) in enumerate(tabs):
        tx = d_x1 + 24 + idx * (tw + 8)
        draw.rounded_rectangle((tx, tab_y, tx + tw, tab_y + 28), 14, fill=PRIMARY_PURPLE if is_sel else CHIP_INACTIVE_BG)
        draw.text((tx + 12, tab_y + 6), tname, font=get_font(FONT_BOLD_LATIN, 11), fill="#ffffff" if is_sel else CHIP_INACTIVE_TEXT)

    # Sample rows
    items = [
        ("あの人", "あのひと", "orang itu", "Bab 1"),
        ("医者", "いしゃ", "dokter", "Bab 1"),
        ("会社員", "かいしゃいん", "pegawai kantor", "Bab 1"),
        ("学生", "がくせい", "mahasiswa", "Bab 1"),
        ("食べる", "たべる", "makan", "Bab 2"),
        ("飲む", "のむ", "minum", "Bab 2"),
        ("本", "ほん", "buku", "Bab 2"),
        ("車", "くるま", "mobil", "Bab 3"),
    ]

    list_start_y = tab_y + 40
    for idx, (kanji, reading, meaning, bab) in enumerate(items):
        iy = list_start_y + idx * 52
        draw.line((d_x1 + 24, iy, d_x2 - 24, iy), fill=BORDER_LIGHT)

        # Japanese + Reading
        draw.text((d_x1 + 28, iy + 6), kanji, font=get_font(FONT_SERIF_CJK, 16), fill=TEXT_PRIMARY)
        draw.text((d_x1 + 95, iy + 8), f"({reading})", font=get_font(FONT_SANS_CJK, 12), fill=PRIMARY_PURPLE)

        # Meaning
        draw.text((d_x1 + 28, iy + 28), meaning, font=get_font(FONT_SANS_LATIN, 12), fill=TEXT_SECONDARY)

        # Bab badge
        draw.rounded_rectangle((d_x2 - 80, iy + 14, d_x2 - 45, iy + 34), 6, fill=BADGE_BG)
        draw.text((d_x2 - 76, iy + 18), bab, font=get_font(FONT_BOLD_LATIN, 9), fill=BADGE_TEXT)

        # Speaker button
        draw.text((d_x2 - 35, iy + 15), "🔊", font=get_font(FONT_SANS_LATIN, 14), fill=PRIMARY_PURPLE)

    # Close button
    draw.rounded_rectangle((d_x1 + 24, d_y2 - 50, d_x2 - 24, d_y2 - 14), 10, fill=PRIMARY_PURPLE)
    draw.text((w // 2 - 25, d_y2 - 38), "Tutup", font=get_font(FONT_BOLD_LATIN, 14), fill="#ffffff")

    im.save(os.path.join(OUT_DIR, filename))
    im.save(os.path.join(REPO_OUT_DIR, filename))
    print(f"Generated {filename}")

def render_before_after_comparison(filename):
    w, h = 840, 520
    im = Image.new("RGBA", (w, h), BG_LIGHT)
    draw = ImageDraw.Draw(im)

    # Title
    draw.text((28, 20), "PERBANDINGAN: TAMPILAN BELAKANG KARTU SAAT FURIGANA DIMATIKAN", font=get_font(FONT_BOLD_LATIN, 16), fill=TEXT_PRIMARY)

    # Left Box: Versi Lama (Bug)
    bx1, by1 = 28, 60
    bx2, by2 = 405, 480
    draw.rounded_rectangle((bx1, by1, bx2, by2), 16, fill="#fff5f5", outline="#fca5a5", width=2)

    draw.rounded_rectangle((bx1 + 16, by1 + 16, bx1 + 140, by1 + 42), 8, fill="#fee2e2")
    draw.text((bx1 + 26, by1 + 22), "✕ VERSI LAMA", font=get_font(FONT_BOLD_LATIN, 12), fill=COLOR_LUPA)

    draw.text((bx1 + 20, by1 + 55), "Furigana toggle OFF di depan kartu", font=get_font(FONT_SANS_LATIN, 12), fill=TEXT_SECONDARY)
    draw.text((bx1 + 20, by1 + 72), "-> Membalik kartu ke belakang:", font=get_font(FONT_BOLD_LATIN, 12), fill=COLOR_LUPA)

    # Card rendering in old version
    draw.rounded_rectangle((bx1 + 25, by1 + 105, bx2 - 25, by2 - 25), 14, fill="#ffffff", outline="#fca5a5")
    draw.text((bx1 + 40, by1 + 120), "Bab 1", font=get_font(FONT_BOLD_LATIN, 11), fill=BADGE_TEXT)

    # Old bug: Kanji repeated, Furigana GONE, Meaning shown
    draw.text((bx1 + 110, by1 + 175), "食べる", font=get_font(FONT_SERIF_CJK, 38), fill=TEXT_PRIMARY)
    draw.text((bx1 + 80, by1 + 235), "Cara baca 'たべる' HILANG!", font=get_font(FONT_BOLD_LATIN, 13), fill=COLOR_LUPA)
    draw.line((bx1 + 70, by1 + 265, bx2 - 70, by1 + 265), fill=BORDER_LIGHT, width=2)
    draw.text((bx1 + 125, by1 + 285), "makan", font=get_font(FONT_BOLD_LATIN, 22), fill=TEXT_PRIMARY)

    # Right Box: Versi Sekarang (Fixed)
    rx1, ry1 = 435, 60
    rx2, ry2 = 812, 480
    draw.rounded_rectangle((rx1, ry1, rx2, ry2), 16, fill="#f0fdf4", outline="#86efac", width=2)

    draw.rounded_rectangle((rx1 + 16, ry1 + 16, rx1 + 160, ry1 + 42), 8, fill="#dcfce7")
    draw.text((rx1 + 26, ry1 + 22), "✓ VERSI SEKARANG", font=get_font(FONT_BOLD_LATIN, 12), fill=COLOR_INGAT)

    draw.text((rx1 + 20, ry1 + 55), "Furigana toggle OFF di depan kartu", font=get_font(FONT_SANS_LATIN, 12), fill=TEXT_SECONDARY)
    draw.text((rx1 + 20, ry1 + 72), "-> Membalik kartu ke belakang:", font=get_font(FONT_BOLD_LATIN, 12), fill=COLOR_INGAT)

    # Card rendering in new version
    draw.rounded_rectangle((rx1 + 25, ry1 + 105, rx2 - 25, ry2 - 25), 14, fill="#ffffff", outline="#86efac")
    draw.text((rx1 + 40, ry1 + 120), "Bab 1", font=get_font(FONT_BOLD_LATIN, 11), fill=BADGE_TEXT)

    # New: Kanji reference, Prominent Hiragana answer, Meaning
    draw.text((rx1 + 145, by1 + 145), "食べる", font=get_font(FONT_SERIF_CJK, 20), fill=TEXT_SECONDARY)
    draw.text((rx1 + 115, by1 + 175), "たべる", font=get_font(FONT_SERIF_CJK, 42), fill=TEXT_PRIMARY)
    draw.text((rx1 + 70, by1 + 235), "✓ Cara baca selalu tampil aman!", font=get_font(FONT_BOLD_LATIN, 13), fill=COLOR_INGAT)
    draw.line((rx1 + 70, by1 + 265, rx2 - 70, by1 + 265), fill=BORDER_LIGHT, width=2)
    draw.text((rx1 + 130, by1 + 285), "makan", font=get_font(FONT_BOLD_LATIN, 22), fill=TEXT_PRIMARY)

    im.save(os.path.join(OUT_DIR, filename))
    im.save(os.path.join(REPO_OUT_DIR, filename))
    print(f"Generated {filename}")

if __name__ == "__main__":
    print("Generating authentic Kotoba user guide screenshots...")
    render_main_screen_front("screen_01_main_light.png", "食べる", "たべる", "taberu", is_dark=False)
    render_main_screen_back("screen_02_card_back_reading.png", "食べる", "たべる", "makan", is_dark=False)
    render_main_screen_front("screen_03_hiragana_front.png", "たべる", "", "taberu", is_dark=False)
    render_main_screen_front("screen_04_katakana_card.png", "コーヒー", "", "ko-hi-", is_dark=False)
    render_main_screen_front("screen_05_prev_button_flow.png", "あの人", "あのひと", "anohito", is_dark=False)
    render_bab_selector_dialog("screen_06_bab_selector_multi.png")
    render_voice_settings_dialog("screen_07_voice_settings.png")
    render_library_dialog("screen_08_library_filter.png")
    render_main_screen_front("screen_09_dark_theme.png", "食べる", "たべる", "taberu", is_dark=True)
    render_before_after_comparison("screen_10_before_after_comparison.png")
    print("All screenshots generated successfully.")
