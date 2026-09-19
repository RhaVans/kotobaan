#!/usr/bin/env python3
"""
tools/build_user_guide_pdf.py
Generates the official Kotoba Visual User Guide PDF in Indonesian.
Designed to match Kotoba's royal violet and parchment aesthetic.
"""

import os
import sys
from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.units import inch, cm
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Image, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

# Register Fonts
pdfmetrics.registerFont(TTFont('KotobaSans', '/system/fonts/DroidSans.ttf'))
pdfmetrics.registerFont(TTFont('KotobaSansBold', '/system/fonts/DroidSans-Bold.ttf'))

# Output Paths
PDF_OUT = "/storage/emulated/0/Download/PROJECT/KTB/KOTOBA_PANDUAN_PENGGUNA.pdf"
IMG_DIR = "/storage/emulated/0/Download/PROJECT/KTB/docs/guide_screenshots"

# Palette
C_PRIMARY = colors.HexColor("#6D28D9")       # Royal Violet
C_PRIMARY_DARK = colors.HexColor("#5B21B6")
C_SECONDARY = colors.HexColor("#7C3AED")
C_BG_WARM = colors.HexColor("#FAF7F2")
C_SURFACE = colors.HexColor("#FFFFFF")
C_BORDER = colors.HexColor("#E8E0D5")
C_INK = colors.HexColor("#1A161E")
C_INK_SOFT = colors.HexColor("#574F60")
C_INK_MUTED = colors.HexColor("#82788D")
C_SUCCESS = colors.HexColor("#15803D")
C_ERROR = colors.HexColor("#B91C1C")
C_BADGE_BG = colors.HexColor("#EDE9FE")
C_BADGE_TXT = colors.HexColor("#5B21B6")
C_CARD_BG = colors.HexColor("#F8F5EE")

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        canvas.Canvas.__init__(self, *args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            canvas.Canvas.showPage(self)
        canvas.Canvas.save(self)

    def draw_page_decorations(self, total_pages):
        if self._pageNumber == 1:
            # Suppress header/footer on cover page
            return

        self.saveState()
        self.setFont("KotobaSans", 9)
        self.setFillColor(C_INK_MUTED)

        # Header
        self.drawString(40, A4[1] - 32, "Kotoba — Panduan Pengguna & Catatan Pembaruan")
        self.setStrokeColor(C_BORDER)
        self.setLineWidth(0.5)
        self.line(40, A4[1] - 38, A4[0] - 40, A4[1] - 38)

        # Footer
        self.line(40, 42, A4[0] - 40, 42)
        page_str = f"Halaman {self._pageNumber} dari {total_pages}"
        self.drawRightString(A4[0] - 40, 28, page_str)
        self.drawString(40, 28, "Aplikasi Kotoba • Versi 2.0.0-FINAL")
        self.restoreState()

def create_callout_card(text, title="PENTING", border_color=C_PRIMARY, bg_color=C_BADGE_BG):
    style_title = ParagraphStyle(
        'CardTitle',
        fontName='KotobaSansBold',
        fontSize=11,
        leading=14,
        textColor=border_color
    )
    style_body = ParagraphStyle(
        'CardBody',
        fontName='KotobaSans',
        fontSize=10,
        leading=14,
        textColor=C_INK
    )

    story = [
        Paragraph(f"<b>{title}</b>", style_title),
        Spacer(1, 4),
        Paragraph(text, style_body)
    ]
    t = Table([[story]], colWidths=[A4[0] - 80])
    t.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), bg_color),
        ('BOX', (0, 0), (-1, -1), 1.2, border_color),
        ('ROUNDEDCORNERS', [8, 8, 8, 8]),
        ('TOPPADDING', (0, 0), (-1, -1), 10),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 10),
        ('LEFTPADDING', (0, 0), (-1, -1), 14),
        ('RIGHTPADDING', (0, 0), (-1, -1), 14),
    ]))
    return t

def build_pdf():
    doc = SimpleDocTemplate(
        PDF_OUT,
        pagesize=A4,
        leftMargin=40,
        rightMargin=40,
        topMargin=50,
        bottomMargin=50
    )

    styles = getSampleStyleSheet()

    # Custom typography styles
    style_cover_title = ParagraphStyle(
        'CoverTitle',
        fontName='KotobaSansBold',
        fontSize=32,
        leading=38,
        textColor=C_PRIMARY,
        alignment=1
    )
    style_cover_subtitle = ParagraphStyle(
        'CoverSubtitle',
        fontName='KotobaSans',
        fontSize=15,
        leading=20,
        textColor=C_INK_SOFT,
        alignment=1
    )
    style_cover_meta = ParagraphStyle(
        'CoverMeta',
        fontName='KotobaSansBold',
        fontSize=10,
        leading=14,
        textColor=C_PRIMARY_DARK,
        alignment=1
    )

    style_h1 = ParagraphStyle(
        'Header1',
        fontName='KotobaSansBold',
        fontSize=20,
        leading=24,
        textColor=C_PRIMARY,
        spaceBefore=14,
        spaceAfter=8
    )
    style_h2 = ParagraphStyle(
        'Header2',
        fontName='KotobaSansBold',
        fontSize=14,
        leading=18,
        textColor=C_INK,
        spaceBefore=12,
        spaceAfter=6
    )
    style_body = ParagraphStyle(
        'Body',
        fontName='KotobaSans',
        fontSize=10.5,
        leading=15,
        textColor=C_INK,
        spaceAfter=6
    )
    style_body_bold = ParagraphStyle(
        'BodyBold',
        fontName='KotobaSansBold',
        fontSize=10.5,
        leading=15,
        textColor=C_INK,
        spaceAfter=6
    )
    style_caption = ParagraphStyle(
        'Caption',
        fontName='KotobaSans',
        fontSize=9,
        leading=12,
        textColor=C_INK_MUTED,
        alignment=1,
        spaceBefore=4,
        spaceAfter=8
    )

    story = []

    # =========================================================================
    # HALAMAN 1: COVER
    # =========================================================================
    story.append(Spacer(1, 40))

    # Hero Badge
    badge_table = Table([[Paragraph("<b>PANDUAN RESMI PENGGUNA</b>", ParagraphStyle('Bdg', fontName='KotobaSansBold', fontSize=10, textColor=C_PRIMARY, alignment=1))]], colWidths=[200])
    badge_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), C_BADGE_BG),
        ('BOX', (0,0), (-1,-1), 1, C_PRIMARY),
        ('ROUNDEDCORNERS', [12, 12, 12, 12]),
        ('ALIGN', (0,0), (-1,-1), 'CENTER'),
        ('TOPPADDING', (0,0), (-1,-1), 6),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
    ]))
    story.append(badge_table)
    story.append(Spacer(1, 25))

    story.append(Paragraph("KOTOBA", style_cover_title))
    story.append(Spacer(1, 10))
    story.append(Paragraph("Panduan Penggunaan Lengkap & Panduan Pembaruan", style_cover_subtitle))
    story.append(Spacer(1, 20))

    # Decorative Line
    story.append(HRFlowable(width="60%", thickness=2, color=C_PRIMARY, spaceAfter=25, spaceBefore=5))

    # Cover Hero Image (Main Screen)
    img_cover_path = os.path.join(IMG_DIR, "screen_01_main_light.png")
    if os.path.exists(img_cover_path):
        story.append(Image(img_cover_path, width=2.4*inch, height=4.9*inch))
        story.append(Paragraph("Tampilan Antarmuka Baru Kotoba (Versi 2.0.0)", style_caption))
    story.append(Spacer(1, 15))

    # Meta box
    meta_text = """
    <b>Versi Rilis:</b> 2.0.0-FINAL &bull; <b>Berkas:</b> KOTOBAAN.apk / KOTOBA FINAL.apk<br/>
    <b>Target Platform:</b> Android &amp; iOS &bull; <b>Diperbarui:</b> September 2026<br/>
    <b>Dokumentasi:</b> Tim Pengembang Kotoba &bull; Bahasa Indonesia
    """
    meta_table = Table([[Paragraph(meta_text, style_cover_meta)]], colWidths=[A4[0] - 120])
    meta_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), C_CARD_BG),
        ('BOX', (0,0), (-1,-1), 1, C_BORDER),
        ('ROUNDEDCORNERS', [8, 8, 8, 8]),
        ('TOPPADDING', (0,0), (-1,-1), 10),
        ('BOTTOMPADDING', (0,0), (-1,-1), 10),
        ('ALIGN', (0,0), (-1,-1), 'CENTER'),
    ]))
    story.append(meta_table)

    story.append(PageBreak())

    # =========================================================================
    # HALAMAN 2: MULAI DALAM 1 MENIT (QUICK START)
    # =========================================================================
    story.append(Paragraph("Mulai dalam 1 Menit", style_h1))
    story.append(Paragraph("Panduan kilat agar Anda dapat langsung menghafal kosakata dan kanji tanpa hambatan:", style_body))
    story.append(Spacer(1, 8))

    quick_steps = [
        ("1", "Buka Aplikasi Kotoba", "Aplikasi langsung menampilkan kartu belajar pada putaran aktif."),
        ("2", "Pilih Materi Belajar", "Ketuk tombol <b>Bab</b> di pojok kanan atas untuk memilih Bab 1–25 atau paket kosakata khusus."),
        ("3", "Amati Sisi Depan Kartu", "Lihat kanji atau kata bahasa Jepang. Uji ingatan Anda sebelum melihat jawabannya."),
        ("4", "Ketuk Kartu untuk Membalik", "Kartu akan berputar dan menampilkan cara baca hiragana serta arti bahasa Indonesia."),
        ("5", "Dengarkan Pelafalan Suara", "Ketuk ikon pengeras suara (🔊) untuk mendengarkan pengucapan fasih bahasa Jepang."),
        ("6", "Tentukan Penilaian Belajar", "Tekan tombol hijau <b>Ingat</b> jika hafal, atau tombol merah <b>Lupa</b> jika masih perlu diulang."),
        ("7", "Periksa Kartu Sebelumnya", "Tekan tombol <b>← Prev</b> kapan saja jika ingin memeriksa kembali kartu yang baru saja lewat.")
    ]

    qs_data = []
    for num, title, desc in quick_steps:
        num_cell = Paragraph(f"<b>{num}</b>", ParagraphStyle('NumC', fontName='KotobaSansBold', fontSize=14, textColor=colors.white, alignment=1))
        desc_cell = [
            Paragraph(f"<b>{title}</b>", style_body_bold),
            Paragraph(desc, style_body)
        ]
        qs_data.append([num_cell, desc_cell])

    qs_table = Table(qs_data, colWidths=[36, A4[0] - 120])
    qs_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (0,-1), C_PRIMARY),
        ('ALIGN', (0,0), (0,-1), 'CENTER'),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('BOTTOMPADDING', (0,0), (-1,-1), 8),
        ('TOPPADDING', (0,0), (-1,-1), 8),
        ('LINEBELOW', (0,0), (-1,-1), 0.5, C_BORDER),
    ]))
    story.append(qs_table)
    story.append(Spacer(1, 15))

    story.append(create_callout_card(
        "Aplikasi Kotoba dirancang untuk belajar mandiri yang efisien. Kartu yang Anda tandai <b>Lupa</b> akan otomatis diulang pada putaran berikutnya hingga Anda benar-benar menguasainya.",
        title="TIPS BELAJAR EFEKTIF",
        border_color=C_SUCCESS,
        bg_color=colors.HexColor("#f0fdf4")
    ))

    story.append(PageBreak())

    # =========================================================================
    # HALAMAN 3: APA YANG BARU? (PERBANDINGAN LENGKAP)
    # =========================================================================
    story.append(Paragraph("Apa yang Baru di Versi Ini?", style_h1))
    story.append(Paragraph(
        "Pembaruan besar versi 2.0.0 membawa perbaikan mendasar pada keandalan sistem belajar, akurasi suara, dan kemudahan navigasi:",
        style_body
    ))
    story.append(Spacer(1, 8))

    comparison_data = [
        [
            Paragraph("<b>Fitur / Bagian</b>", style_body_bold),
            Paragraph("<b>Versi Lama</b>", style_body_bold),
            Paragraph("<b>Versi Sekarang (Baru)</b>", ParagraphStyle('Brd', fontName='KotobaSansBold', fontSize=10, textColor=C_PRIMARY))
        ],
        [
            Paragraph("<b>Navigasi Kartu (Prev)</b>", style_body),
            Paragraph("Terblokir popup notifikasi; tombol tidak bisa digunakan mundur.", style_body),
            Paragraph("<b>Bisa mundur bebas</b> ke kartu sebelumnya dalam putaran tanpa notifikasi penghalang.", style_body)
        ],
        [
            Paragraph("<b>Pelafalan Suara (TTS)</b>", style_body),
            Paragraph("Tanpa suara atau hanya lafal terbatas tanpa sinkronisasi teks.", style_body),
            Paragraph("<b>Pelafalan Jepang fasih</b>, 100% sinkron dengan apa yang terlihat pada kartu.", style_body)
        ],
        [
            Paragraph("<b>Integritas Cara Baca</b>", style_body),
            Paragraph("Saat furigana dimatikan, cara baca di belakang kartu hilang.", style_body),
            Paragraph("<b>Cara baca selalu tampil</b> di belakang kartu sebagai jawaban utama.", style_body)
        ],
        [
            Paragraph("<b>Pemilihan Materi</b>", style_body),
            Paragraph("Hanya bisa memilih satu bab tunggal.", style_body),
            Paragraph("<b>Multi-Bab</b> (pilih beberapa bab sekaligus) + paket per 50 kata.", style_body)
        ],
        [
            Paragraph("<b>Koleksi Materi</b>", style_body),
            Paragraph("Terbatas kosakata bab dasar.", style_body),
            Paragraph("<b>4.363 materi</b> lengkap: Kosakata, Kanji 613, Kanji 2.119, Verba, Adjektiva.", style_body)
        ],
        [
            Paragraph("<b>Pilihan Tema</b>", style_body),
            Paragraph("Warna gelap kurang optimal dan kontras tidak seragam.", style_body),
            Paragraph("<b>Mode Gelap Obsidian Violet</b> yang nyaman dan kontras tinggi.", style_body)
        ]
    ]

    comp_table = Table(comparison_data, colWidths=[120, 180, A4[0] - 380])
    comp_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), C_BADGE_BG),
        ('BOX', (0,0), (-1,-1), 1, C_BORDER),
        ('INNERGRID', (0,0), (-1,-1), 0.5, C_BORDER),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('TOPPADDING', (0,0), (-1,-1), 6),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
        ('LEFTPADDING', (0,0), (-1,-1), 8),
        ('RIGHTPADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(comp_table)
    story.append(Spacer(1, 14))

    story.append(create_callout_card(
        "Seluruh perbaikan di atas tersedia di kedua varian aplikasi: <b>KOTOBAAN.apk</b> (versi ringan 829 KB dengan suara sistem Android) dan <b>KOTOBA FINAL.apk</b> (versi suara neural).",
        title="INFORMASI DISTRIBUSI",
        border_color=C_PRIMARY,
        bg_color=C_BADGE_BG
    ))

    story.append(PageBreak())

    # =========================================================================
    # HALAMAN 4: SISTEM FLASHCARD — ANATOMI & DEPAN-BELAKANG
    # =========================================================================
    story.append(Paragraph("Sistem Flashcard & Hubungan Informasi", style_h1))
    story.append(Paragraph(
        "Kartu belajar Kotoba kini menerapkan kebijakan tampilan independen yang menjaga keutuhan data. Setiap kartu memiliki sisi <b>Depan</b> untuk menguji hafalan dan sisi <b>Belakang</b> sebagai kunci jawaban lengkap.",
        style_body
    ))
    story.append(Spacer(1, 8))

    img_f_path = os.path.join(IMG_DIR, "screen_01_main_light.png")
    img_b_path = os.path.join(IMG_DIR, "screen_02_card_back_reading.png")

    card_imgs = []
    if os.path.exists(img_f_path) and os.path.exists(img_b_path):
        f_elem = [Image(img_f_path, width=2.5*inch, height=5.1*inch), Paragraph("<b>Sisi Depan (Uji Daya Ingat)</b>", style_caption)]
        b_elem = [Image(img_b_path, width=2.5*inch, height=5.1*inch), Paragraph("<b>Sisi Belakang (Kunci Jawaban)</b>", style_caption)]
        card_table = Table([[f_elem, b_elem]], colWidths=[(A4[0]-80)/2, (A4[0]-80)/2])
        card_table.setStyle(TableStyle([
            ('ALIGN', (0,0), (-1,-1), 'CENTER'),
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ]))
        story.append(card_table)

    story.append(Spacer(1, 10))
    story.append(Paragraph("<b>Aturan Tampilan Sisi Depan &amp; Belakang:</b>", style_body_bold))
    story.append(Paragraph(
        "&bull; <b>Sisi Depan:</b> Menampilkan Kanji utama (misal: <i>食べる</i>). Jika sakelar Furigana dinyalakan, cara baca kecil akan muncul di atas kanji sebagai panduan.<br/>"
        "&bull; <b>Sisi Belakang:</b> Saat kartu diketuk, sisi belakang akan memperlihatkan cara baca hiragana (<i>たべる</i>) dengan huruf besar yang tegas, referensi kanji, serta terjemahan bahasa Indonesia (<i>makan</i>).",
        style_body
    ))

    story.append(PageBreak())

    # =========================================================================
    # HALAMAN 5: MODE HIRAGANA, KATAKANA, DAN FURIGANA TOGGLE
    # =========================================================================
    story.append(Paragraph("Representasi Fleksibel &amp; Sakelar Furigana", style_h1))
    story.append(Paragraph(
        "Kotoba mendukung berbagai jenis kata bahasa Jepang dan fleksibilitas mode tampilan tanpa merusak data lain:",
        style_body
    ))
    story.append(Spacer(1, 6))

    img_h_path = os.path.join(IMG_DIR, "screen_03_hiragana_front.png")
    img_k_path = os.path.join(IMG_DIR, "screen_04_katakana_card.png")

    if os.path.exists(img_h_path) and os.path.exists(img_k_path):
        h_elem = [Image(img_h_path, width=2.4*inch, height=4.9*inch), Paragraph("<b>Mode Depan Hiragana</b>", style_caption)]
        k_elem = [Image(img_k_path, width=2.4*inch, height=4.9*inch), Paragraph("<b>Kata Serapan Katakana</b>", style_caption)]
        hk_table = Table([[h_elem, k_elem]], colWidths=[(A4[0]-80)/2, (A4[0]-80)/2])
        hk_table.setStyle(TableStyle([
            ('ALIGN', (0,0), (-1,-1), 'CENTER'),
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ]))
        story.append(hk_table)

    story.append(Spacer(1, 10))

    story.append(create_callout_card(
        "<b>Cara Kerja Sakelar Furigana yang Benar:</b><br/>"
        "Mematikan tombol <b>Furigana</b> di bagian bawah berfungsi agar Anda dapat menguji apakah Anda benar-benar hafal bacaan kanji tanpa ada contekan di sisi depan. "
        "Pada versi sebelumnya, mematikan furigana menyebabkan cara baca hilang di belakang kartu. Pada versi sekarang, cara baca <b>TETAP AKAN SELALU MUNCUL DI BELAKANG KARTU</b> sebagai jawaban.",
        title="JAMINAN KEUTUHAN INFORMASI",
        border_color=C_PRIMARY,
        bg_color=C_BADGE_BG
    ))

    story.append(PageBreak())

    # =========================================================================
    # HALAMAN 6: NAVIGASI KARTU PREVIOUS (← PREV)
    # =========================================================================
    story.append(Paragraph("Navigasi Kartu Sebelumnya (← Prev)", style_h1))
    story.append(Paragraph(
        "Salah satu permintaan terbesar pengguna adalah kemudahan meninjau kembali kartu yang baru saja dipelajari. Pada versi ini, tombol <b>← Prev</b> telah disempurnakan sepenuhnya:",
        style_body
    ))
    story.append(Spacer(1, 6))

    img_p_path = os.path.join(IMG_DIR, "screen_05_prev_button_flow.png")
    if os.path.exists(img_p_path):
        story.append(Image(img_p_path, width=2.4*inch, height=4.9*inch))
        story.append(Paragraph("Tombol ← Prev aktif di pojok kiri bawah", style_caption))

    story.append(Spacer(1, 8))
    story.append(Paragraph("<b>Perilaku Tombol Prev:</b>", style_body_bold))
    prev_points = [
        "<b>Kembali Bebas:</b> Anda dapat menekan Prev kapan saja untuk mundur ke kartu sebelumnya dalam putaran hafalan yang sama.",
        "<b>Otomatis Kembali ke Sisi Depan:</b> Kartu yang kembali akan langsung direset ke sisi depan sehingga Anda dapat menguji ingatan kembali.",
        "<b>Putar Ulang Suara:</b> Saat kembali ke kartu sebelumnya, audio pelafalan akan otomatis diputar ulang.",
        "<b>Tanpa Notifikasi Penghalang:</b> Tidak ada lagi popup atau pesan toast yang mengganggu kenyamanan belajar Anda.",
        "<b>Senyap di Kartu Pertama:</b> Jika Anda sudah berada di kartu pertama, tombol Prev akan meredup dan diam tanpa pesan error."
    ]
    for pt in prev_points:
        story.append(Paragraph(f"&bull; {pt}", style_body))

    story.append(PageBreak())

    # =========================================================================
    # HALAMAN 7: FITUR PELAFALAN SUARA (TTS)
    # =========================================================================
    story.append(Paragraph("Fitur Pelafalan Suara Bahasa Jepang", style_h1))
    story.append(Paragraph(
        "Aplikasi Kotoba kini dilengkapi dengan pembacaan suara audio asli Jepang. Anda dapat mendengar pengucapan yang fasih dan tepat untuk setiap kata yang dipelajari.",
        style_body
    ))
    story.append(Spacer(1, 6))

    img_v_path = os.path.join(IMG_DIR, "screen_07_voice_settings.png")
    if os.path.exists(img_v_path):
        story.append(Image(img_v_path, width=2.5*inch, height=5.1*inch))
        story.append(Paragraph("Menu Dialog Pengaturan Pelafalan Suara", style_caption))

    story.append(Spacer(1, 8))
    story.append(Paragraph("<b>Ketentuan &amp; Penggunaan Suara:</b>", style_body_bold))
    story.append(Paragraph(
        "1. <b>Sinkronisasi Tampilan:</b> Kotoba menjamin bahwa teks yang Anda dengar 100% sama dengan teks yang sedang ditampilkan di layar kartu.<br/>"
        "2. <b>Dua Tombol Suara:</b> Tombol suara di sisi depan melafalkan kata uji (kanji/kana), sedangkan tombol suara di sisi belakang melafalkan cara baca jawaban.<br/>"
        "3. <b>Tekan Lama untuk Pengaturan:</b> Tekan dan tahan (long press) tombol pengeras suara untuk membuka jendela pengaturan kecepatan (0.75x hingga 1.10x) serta tinggi nada bicara.",
        style_body
    ))

    story.append(PageBreak())

    # =========================================================================
    # HALAMAN 8: PUSTAKA & PEMILIHAN MATERI (MULTI-BAB)
    # =========================================================================
    story.append(Paragraph("Pustaka Materi &amp; Multi-Bab", style_h1))
    story.append(Paragraph(
        "Belajar kini lebih leluasa dengan kemampuan memilih beberapa bab sekaligus serta mengakses katalog lengkap 4.363 materi.",
        style_body
    ))
    story.append(Spacer(1, 6))

    img_m_path = os.path.join(IMG_DIR, "screen_06_bab_selector_multi.png")
    img_l_path = os.path.join(IMG_DIR, "screen_08_library_filter.png")

    if os.path.exists(img_m_path) and os.path.exists(img_l_path):
        m_elem = [Image(img_m_path, width=2.4*inch, height=4.9*inch), Paragraph("<b>Pemilihan Multi-Bab</b>", style_caption)]
        l_elem = [Image(img_l_path, width=2.4*inch, height=4.9*inch), Paragraph("<b>Pustaka &amp; Kamus Cari</b>", style_caption)]
        ml_table = Table([[m_elem, l_elem]], colWidths=[(A4[0]-80)/2, (A4[0]-80)/2])
        ml_table.setStyle(TableStyle([
            ('ALIGN', (0,0), (-1,-1), 'CENTER'),
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ]))
        story.append(ml_table)

    story.append(Spacer(1, 8))
    story.append(Paragraph("<b>Fitur Pilihan Materi Baru:</b>", style_body_bold))
    story.append(Paragraph(
        "&bull; <b>Multi-Bab Bebas:</b> Anda dapat mencentang Bab 1, 2, dan 5 secara bersamaan untuk latihan campuran.<br/>"
        "&bull; <b>Paket per 50 Kata:</b> Untuk materi besar seperti Kata Kerja JFT, Kata Sifat, dan Kanji, materi dibagi rapi menjadi paket 50 kata agar tidak membebani konsentrasi belajar.<br/>"
        "&bull; <b>Pencarian Cepat di Pustaka:</b> Ketik kata dalam huruf romaji, hiragana, atau terjemahan Indonesia untuk menemukan kosakata seketika.",
        style_body
    ))

    story.append(PageBreak())

    # =========================================================================
    # HALAMAN 9: TEMA TERANG & GELAP (DARK MODE)
    # =========================================================================
    story.append(Paragraph("Pilihan Tema: Terang &amp; Gelap", style_h1))
    story.append(Paragraph(
        "Kotoba menyajikan palet warna yang dirancang khusus untuk kenyamanan membaca dalam berbagai kondisi pencahayaan.",
        style_body
    ))
    story.append(Spacer(1, 6))

    img_d_path = os.path.join(IMG_DIR, "screen_09_dark_theme.png")
    if os.path.exists(img_d_path):
        story.append(Image(img_d_path, width=2.5*inch, height=5.1*inch))
        story.append(Paragraph("Tampilan Mode Gelap (Obsidian Violet)", style_caption))

    story.append(Spacer(1, 8))
    story.append(Paragraph("<b>Kelebihan Mode Tampilan:</b>", style_body_bold))
    story.append(Paragraph(
        "&bull; <b>Mode Terang (Parchment Warm):</b> Menggunakan nuansa kertas krem yang lembut di mata dan tidak menyilaukan saat belajar siang hari.<br/>"
        "&bull; <b>Mode Gelap (Obsidian Violet):</b> Menggunakan latar belakang gelap elegan dengan aksen violet dan teks ber-kontras tinggi, sempurna untuk belajar di ruangan temaram tanpa membuat mata lelah.<br/>"
        "&bull; <b>Beralih Instan:</b> Cukup ketuk ikon bulan (☾) atau matahari (☼) pada bilah navigasi atas untuk beralih mode kapan saja.",
        style_body
    ))

    story.append(PageBreak())

    # =========================================================================
    # HALAMAN 10: PERBANDINGAN SEBELUM VS SEKARANG
    # =========================================================================
    story.append(Paragraph("Perbandingan Visual: Sebelum vs Sekarang", style_h1))
    story.append(Paragraph(
        "Berikut bukti nyata peningkatan kualitas kartu belajar pada pembaruan ini:",
        style_body
    ))
    story.append(Spacer(1, 8))

    img_comp_path = os.path.join(IMG_DIR, "screen_10_before_after_comparison.png")
    if os.path.exists(img_comp_path):
        story.append(Image(img_comp_path, width=A4[0] - 80, height=3.1*inch))
        story.append(Paragraph("Perbandingan Tampilan Belakang Kartu saat Furigana Dimatikan", style_caption))

    story.append(Spacer(1, 10))
    story.append(Paragraph("<b>Apa yang Sebenarnya Diperbaiki?</b>", style_body_bold))
    story.append(Paragraph(
        "Pada versi lama (kiri), jika pengguna mematikan furigana di depan kartu untuk menguji hafalan kanji, "
        "aplikasi secara keliru ikut menyembunyikan cara baca di belakang kartu saat dibalik. Pengguna hanya disuguhi kanji lagi dan arti Indonesia tanpa tahu cara bacanya.<br/><br/>"
        "Pada versi sekarang (kanan), logika tampilan telah diperbaiki secara tuntas. Sisi belakang kartu adalah kunci jawaban, sehingga <b>cara baca selalu tampil dengan ukuran besar dan jelas</b> tanpa terpengaruh sakelar depan.",
        style_body
    ))

    story.append(PageBreak())

    # =========================================================================
    # HALAMAN 11: APA YANG PERLU DIBIASAKAN? (PENGGUNA LAMA)
    # =========================================================================
    story.append(Paragraph("Apa yang Perlu Dibiasakan?", style_h1))
    story.append(Paragraph(
        "Bagi Anda yang telah terbiasa menggunakan Kotoba versi lama, berikut poin-poin penyesuaian praktis yang perlu diketahui:",
        style_body
    ))
    story.append(Spacer(1, 8))

    habits = [
        ("Gunakan Tombol Prev Tanpa Ragu", "Anda kini tidak perlu takut menekan tombol ← Prev. Aplikasi tidak akan memunculkan popup larangan dan akan langsung membawa Anda ke kartu sebelumnya."),
        ("Jangan Ragu Mematikan Furigana", "Matikan sakelar Furigana di depan kartu untuk melatih daya ingat membaca kanji. Jawaban cara baca tetap aman tersedia di belakang kartu saat Anda membaliknya."),
        ("Manfaatkan Ikon Audio 🔊", "Tekan ikon suara pada setiap kartu untuk membiasakan telinga dengan intonasi dan pengucapan alami penutur asli Jepang."),
        ("Kombinasikan Beberapa Bab", "Tidak perlu lagi bolak-balik mengganti bab satu per satu. Centang semua bab yang ingin Anda uji dalam satu sesi hafalan."),
        ("Materi Terbagi per 50 Kata", "Pada daftar kata kerja dan kanji besar, gunakan pembagian paket 50 kata agar sesi belajar terasa ringan dan terukur.")
    ]

    for title, desc in habits:
        h_table = Table([[
            Paragraph(f"<b>{title}</b>", ParagraphStyle('HTit', fontName='KotobaSansBold', fontSize=12, textColor=C_PRIMARY)),
        ], [
            Paragraph(desc, style_body)
        ]], colWidths=[A4[0] - 80])
        h_table.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,-1), C_CARD_BG),
            ('BOX', (0,0), (-1,-1), 1, C_BORDER),
            ('ROUNDEDCORNERS', [6, 6, 6, 6]),
            ('TOPPADDING', (0,0), (-1,-1), 8),
            ('BOTTOMPADDING', (0,0), (-1,-1), 8),
            ('LEFTPADDING', (0,0), (-1,-1), 12),
            ('RIGHTPADDING', (0,0), (-1,-1), 12),
        ]))
        story.append(h_table)
        story.append(Spacer(1, 8))

    story.append(PageBreak())

    # =========================================================================
    # HALAMAN 12: JIKA MENEMUKAN MASALAH (TROUBLESHOOTING)
    # =========================================================================
    story.append(Paragraph("Jika Menemukan Masalah", style_h1))
    story.append(Paragraph("Solusi ringkas untuk kendala teknis yang mungkin Anda temui saat belajar:", style_body))
    story.append(Spacer(1, 8))

    troubles = [
        (
            "Suara Pelafalan Tidak Terdengar",
            "1. Pastikan volume media pada perangkat Android Anda tidak dalam keadaan bisu (mute).<br/>"
            "2. Pastikan paket data suara bahasa Jepang telah terpasang di perangkat. Buka <b>Pengaturan HP &rarr; Sistem &rarr; Aksesibilitas &rarr; Keluaran Text-to-Speech &rarr; Mesin Google TTS &rarr; Pasang Data Suara Bahasa Jepang</b>."
        ),
        (
            "Kartu Menampilkan Tulisan 'Kosong'",
            "Hal ini terjadi jika tidak ada bab atau materi yang dicentang. Ketuk tombol <b>Bab</b> di kanan atas, centang minimal satu Bab atau Paket Kata, lalu tekan <b>Mulai Belajar</b>."
        ),
        (
            "Urutan Kartu Terlalu Mudah Ditebak",
            "Ketuk tombol acak (<b>⇄</b>) pada bilah navigasi atas untuk mengocok ulang seluruh kartu sehingga urutan tampil tidak berurutan."
        ),
        (
            "Ingin Menyesuaikan Kecepatan Bicara",
            "Ketuk dan tahan ikon pengeras suara (🔊) selama 1 detik untuk membuka menu pengaturan nada dan kecepatan bicara (misal: 0.85x untuk pemula)."
        )
    ]

    for issue, sol in troubles:
        t_box = Table([[
            Paragraph(f"<b>❓ Masalah: {issue}</b>", ParagraphStyle('TrbQ', fontName='KotobaSansBold', fontSize=11, textColor=C_ERROR)),
        ], [
            Paragraph(f"<b>Solusi:</b><br/>{sol}", style_body)
        ]], colWidths=[A4[0] - 80])
        t_box.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,-1), colors.HexColor("#fffdfa")),
            ('BOX', (0,0), (-1,-1), 1, C_BORDER),
            ('ROUNDEDCORNERS', [6, 6, 6, 6]),
            ('TOPPADDING', (0,0), (-1,-1), 8),
            ('BOTTOMPADDING', (0,0), (-1,-1), 8),
            ('LEFTPADDING', (0,0), (-1,-1), 12),
            ('RIGHTPADDING', (0,0), (-1,-1), 12),
        ]))
        story.append(t_box)
        story.append(Spacer(1, 8))

    story.append(Spacer(1, 10))
    story.append(create_callout_card(
        "<b>Kotoba — Belajar Bahasa Jepang Jadi Lebih Nyaman &amp; Terarah.</b><br/>"
        "Selamat menggunakan Kotoba versi terbaru! Kuasai ribuan kosakata, kanji, dan percakapan dengan metode pengulangan berjarak (Spaced Repetition) yang teruji.",
        title="CATATAN PENUTUP",
        border_color=C_SUCCESS,
        bg_color=colors.HexColor("#f0fdf4")
    ))

    # Build Document
    print("Building Kotoba User Guide PDF...")
    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"PDF successfully created at: {PDF_OUT}")

if __name__ == "__main__":
    build_pdf()
