#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CLINEX – Generador de Diagrama de Flujo PDF
Genera diagrama completo del sistema en 3 páginas usando fpdf2.
Ejecutar: python generar_diagrama.py
"""

from fpdf import FPDF
import math, os

# ─── Paleta de colores ────────────────────────────────────────────────────────
DARK   = (0, 36, 68)
BLUE   = (46, 134, 222)
LBLUE  = (210, 230, 255)
PURP   = (110, 55, 190)
LPURP  = (230, 215, 255)
GREEN  = (34, 150, 75)
LGREEN = (210, 245, 225)
ORANG  = (205, 90, 15)
LORANG = (255, 232, 208)
GRAY   = (80, 88, 100)
LGRAY  = (232, 235, 242)
YELL   = (210, 158, 0)
LYELL  = (255, 248, 205)
RED    = (176, 26, 26)
LRED   = (255, 215, 215)
WHITE  = (255, 255, 255)
TEAL   = (16, 140, 130)
LTEAL  = (205, 245, 240)


# ─── Clase base PDF ───────────────────────────────────────────────────────────
FONT_DIR = "C:/Windows/Fonts"

class PDF(FPDF):
    _page_subtitle = ""

    def _add_fonts(self):
        self.add_font("Arial",  "",  f"{FONT_DIR}/arial.ttf",   )
        self.add_font("Arial",  "B", f"{FONT_DIR}/arialbd.ttf", )
        self.add_font("Arial",  "I", f"{FONT_DIR}/ariali.ttf",  )
        self.add_font("Arial",  "BI",f"{FONT_DIR}/arialbi.ttf", )

    def header(self):
        self.set_fill_color(*DARK)
        self.rect(0, 0, self.w, 17, "F")
        self.set_font("Arial", "B", 11)
        self.set_text_color(*WHITE)
        self.set_xy(0, 0)
        self.cell(self.w, 17, "CLINEX  -  Sistema Hospitalario  -  Diagrama de Flujo", align="C")
        if self._page_subtitle:
            self.set_fill_color(46, 90, 130)
            self.rect(0, 17, self.w, 8, "F")
            self.set_font("Arial", "B", 8)
            self.set_xy(0, 18)
            self.cell(self.w, 6, self._page_subtitle, align="C")
        self.set_text_color(0, 0, 0)

    def footer(self):
        self.set_y(-11)
        self.set_font("Arial", "I", 7)
        self.set_text_color(130, 130, 130)
        self.cell(0, 8, f"MediConsult Beta  -  Pagina {self.page_no()}", align="C")


# ─── Primitivas de dibujo ────────────────────────────────────────────────────

def box(pdf, x, y, w, h, lines, bg=BLUE, fg=WHITE, fs=7.5, lw=0.4):
    """Rectángulo relleno con texto centrado. lines puede ser str o list."""
    dark_bg = tuple(max(0, c - 55) for c in bg)
    pdf.set_fill_color(*bg)
    pdf.set_draw_color(*dark_bg)
    pdf.set_line_width(lw)
    pdf.rect(x, y, w, h, "FD")

    if isinstance(lines, str):
        lines = [lines]
    n = len(lines)
    lh = min(4.2, h / max(n, 1))
    sy = y + (h - n * lh) / 2

    for i, t in enumerate(lines):
        pdf.set_font("Arial", "B" if i == 0 else "", fs)
        pdf.set_text_color(*fg)
        pdf.set_xy(x + 1, sy + i * lh)
        pdf.cell(w - 2, lh, t, align="C")

    pdf.set_text_color(0, 0, 0)
    pdf.set_draw_color(0, 0, 0)
    pdf.set_line_width(0.2)


def diamond(pdf, cx, cy, rw, rh, lines, bg=YELL, fg=(55, 38, 0)):
    """Rombo (decisión) centrado en (cx, cy)."""
    pts = [(cx, cy - rh), (cx + rw, cy), (cx, cy + rh), (cx - rw, cy)]
    pdf.set_fill_color(*bg)
    pdf.set_draw_color(160, 120, 0)
    pdf.set_line_width(0.5)
    pdf.polygon(pts, style="FD")

    if isinstance(lines, str):
        lines = [lines]
    n = len(lines)
    lh = 4.0
    sy = cy - (n * lh) / 2
    for i, t in enumerate(lines):
        pdf.set_font("Arial", "B" if i == 0 else "", 6.5)
        pdf.set_text_color(*fg)
        pdf.set_xy(cx - rw + 2, sy + i * lh)
        pdf.cell((rw - 2) * 2, lh, t, align="C")

    pdf.set_text_color(0, 0, 0)
    pdf.set_draw_color(0, 0, 0)
    pdf.set_line_width(0.2)


def arr(pdf, x1, y1, x2, y2, color=GRAY, lw=0.5, label="", lpos="mid"):
    """Flecha con punta de (x1,y1) a (x2,y2)."""
    pdf.set_draw_color(*color)
    pdf.set_line_width(lw)
    pdf.line(x1, y1, x2, y2)

    dx, dy = x2 - x1, y2 - y1
    L = math.hypot(dx, dy)
    if L < 0.01:
        return
    ux, uy = dx / L, dy / L
    px, py = -uy, ux
    tip = (x2, y2)
    bx, by = x2 - ux * 3.2, y2 - uy * 3.2
    p1 = (bx + px * 1.6, by + py * 1.6)
    p2 = (bx - px * 1.6, by - py * 1.6)
    pdf.set_fill_color(*color)
    pdf.polygon([tip, p1, p2], style="F")

    if label:
        if lpos == "mid":
            lx, ly = (x1 + x2) / 2 + 1, (y1 + y2) / 2 - 3
        else:
            lx, ly = lpos
        pdf.set_font("Arial", "I", 6)
        pdf.set_text_color(*GRAY)
        pdf.set_xy(lx, ly)
        pdf.cell(28, 4, label)
        pdf.set_text_color(0, 0, 0)

    pdf.set_draw_color(0, 0, 0)
    pdf.set_line_width(0.2)


def line_seg(pdf, x1, y1, x2, y2, color=GRAY, lw=0.4):
    pdf.set_draw_color(*color)
    pdf.set_line_width(lw)
    pdf.line(x1, y1, x2, y2)
    pdf.set_draw_color(0, 0, 0)
    pdf.set_line_width(0.2)


def lbl(pdf, x, y, text, fs=7, bold=False, color=GRAY):
    pdf.set_font("Arial", "B" if bold else "", fs)
    pdf.set_text_color(*color)
    pdf.set_xy(x, y)
    pdf.cell(0, 4, text)
    pdf.set_text_color(0, 0, 0)


def section_bar(pdf, x, y, w, h, text, color=DARK):
    pdf.set_fill_color(*color)
    pdf.set_draw_color(*color)
    pdf.rect(x, y, w, h, "F")
    pdf.set_font("Arial", "B", 8)
    pdf.set_text_color(*WHITE)
    pdf.set_xy(x, y + (h - 5) / 2)
    pdf.cell(w, 5, text, align="C")
    pdf.set_text_color(0, 0, 0)


# ─── PÁGINA 1: Autenticación y Roles ─────────────────────────────────────────

def pagina_1(pdf):
    pdf._page_subtitle = "PÁGINA 1  ·  Autenticación y Navegación por Rol"
    pdf.add_page()

    # Fondo claro para el área de contenido
    pdf.set_fill_color(*LGRAY)
    pdf.rect(10, 27, 190, 265, "F")

    TOP = 30   # y inicio contenido
    CX  = 105  # centro horizontal

    # ── INICIO APP ───────────────────────────────────────────────────
    section_bar(pdf, 11, TOP, 188, 7, "(1) INICIO DE LA APLICACIÓN", DARK)
    bx, by = CX - 30, TOP + 11
    box(pdf, bx, by, 60, 10, ["AppFX · Main.java", "Arranca la JVM + JavaFX"], DARK, WHITE, 7)
    arr(pdf, CX, by + 10, CX, by + 18)

    # ── LOGIN ────────────────────────────────────────────────────────
    lx, ly = CX - 35, by + 18
    box(pdf, lx, ly, 70, 13, ["LOGIN", "login.fxml · LoginController"], BLUE, WHITE, 7.5)

    # Registrarse branch (izq)
    arr(pdf, lx, ly + 6, lx - 22, ly + 6, BLUE, 0.5, "Crear cuenta")
    box(pdf, lx - 44, ly, 22, 12, ["Registrar-", "se"], LBLUE, DARK, 6.5)

    # Recuperar (der)
    arr(pdf, lx + 70, ly + 6, lx + 93, ly + 6, BLUE, 0.5, "¿Olvidé clave?")
    box(pdf, lx + 93, ly, 27, 12, ["Recuperar", "Contraseña"], LBLUE, DARK, 6.5)

    arr(pdf, CX, ly + 13, CX, ly + 22)

    # ── DECISIÓN: credenciales ───────────────────────────────────────
    dy1 = ly + 22 + 9      # centro Y del rombo
    diamond(pdf, CX, dy1, 32, 9, ["¿Credenciales", "válidas?"])

    # NO -> error (derecha)
    arr(pdf, CX + 32, dy1, CX + 55, dy1, RED, 0.5)
    box(pdf, CX + 55, dy1 - 6, 42, 12,
        ["ERROR", "Credenciales incorrectas", "-> Intento registrado"], RED, WHITE, 6)
    lbl(pdf, CX + 33, dy1 - 3, "NO", 6, True, RED)

    # SÍ -> abajo
    arr(pdf, CX, dy1 + 9, CX, dy1 + 18, GREEN)
    lbl(pdf, CX + 1, dy1 + 11, "SÍ", 6, True, GREEN)

    # ── DECISIÓN: rol ────────────────────────────────────────────────
    dy2 = dy1 + 18 + 8
    diamond(pdf, CX, dy2, 28, 8, ["¿Cuál es", "el ROL?"])

    # ── 4 COLUMNAS DE ROL ────────────────────────────────────────────
    ROW_Y = dy2 + 8 + 6
    COL_W = 44
    GAP   = 2.5
    COLS  = [
        (12,                    "ADMINISTRADOR",   ORANG, LORANG, "ADMIN"),
        (12 + COL_W + GAP,      "MÉDICO GENERAL",  BLUE,  LBLUE,  "MEDICO/MEDICO_GENERAL"),
        (12 + (COL_W+GAP)*2,    "ESPECIALISTA",    PURP,  LPURP,  "MEDICO_ESPECIALISTA"),
        (12 + (COL_W+GAP)*3,    "PACIENTE",        GREEN, LGREEN, "PACIENTE"),
    ]

    # Líneas desde rombo a cada columna
    col_centers = [c[0] + COL_W / 2 for c in COLS]
    # Horizontal line at dy2+8 -> ROW_Y-4
    pivot_y = dy2 + 8 + 1
    line_seg(pdf, col_centers[0], pivot_y, col_centers[-1], pivot_y, GRAY, 0.5)
    for cx_ in col_centers:
        line_seg(pdf, cx_, pivot_y, cx_, pivot_y, GRAY, 0.5)
        arr(pdf, cx_, pivot_y, cx_, ROW_Y - 1, GRAY, 0.5)
    arr(pdf, CX, dy2 + 8, CX, pivot_y, GRAY, 0.5)

    # Encabezados de columna
    for (cx_, title, bg, lbg, role) in COLS:
        box(pdf, cx_, ROW_Y, COL_W, 10, [title, f"ROL: {role}"], bg, WHITE, 6)

    # Pantallas iniciales
    INIT_Y = ROW_Y + 14
    INIT_DATA = [
        ["Panel Admin", "(Pacientes.fxml)"],
        ["Panel Médico", "(panel_medico.fxml)"],
        ["Panel Especialista", "(panel_especialista.fxml)"],
        ["Portal Paciente", "(portal_paciente.fxml)"],
    ]
    BG2 = [LORANG, LBLUE, LPURP, LGREEN]
    FG2 = [ORANG,  BLUE,  PURP,  GREEN]
    for i, (cx_, *_) in enumerate(COLS):
        arr(pdf, cx_ + COL_W/2, ROW_Y + 10, cx_ + COL_W/2, INIT_Y - 1, COLS[i][2], 0.4)
        box(pdf, cx_, INIT_Y, COL_W, 11, INIT_DATA[i], BG2[i], FG2[i], 6.5)

    # Módulos accesibles
    MOD_Y = INIT_Y + 15
    MODULES = [
        [["Pacientes","Médicos","Citas"],["Diagnóstico IA"],["Remisiones"],["Lista Espera"],["Notificaciones"]],
        [["Citas Pendientes"],["-> Iniciar Consulta"],["Historia Clínica"],["Completar Cita"]],
        [["Remisiones\nAsignadas"],["-> Consulta\nEspecializada"],["Alta Médica"]],
        [["Autodiagnóstico IA"],["Mis Citas"],["Mis Órdenes"],["Notificaciones"],["Mi Historial"]],
    ]
    MOD_BG = [LORANG, LBLUE, LPURP, LGREEN]
    MOD_FG = [ORANG,  BLUE,  PURP,  GREEN]
    for i, (cx_, *_) in enumerate(COLS):
        mods = MODULES[i]
        arr(pdf, cx_ + COL_W/2, INIT_Y + 11, cx_ + COL_W/2, MOD_Y - 1, COLS[i][2], 0.4)
        current_y = MOD_Y
        for j, mod in enumerate(mods):
            if isinstance(mod, list) and len(mod) == 1 and "\n" in mod[0]:
                parts = mod[0].split("\n")
                mod = parts
            box(pdf, cx_, current_y, COL_W, 10, mod, MOD_BG[i], MOD_FG[i], 6)
            if j < len(mods) - 1:
                arr(pdf, cx_+COL_W/2, current_y+10, cx_+COL_W/2, current_y+13, COLS[i][2], 0.35)
            current_y += 14

    # Nota del bug corregido
    note_y = 278
    pdf.set_fill_color(255, 245, 200)
    pdf.set_draw_color(*YELL)
    pdf.set_line_width(0.5)
    pdf.rect(11, note_y, 188, 13, "FD")
    pdf.set_font("Arial", "B", 7)
    pdf.set_text_color(100, 70, 0)
    pdf.set_xy(13, note_y + 1.5)
    pdf.cell(0, 4, "[OK]  BUG CORREGIDO: Al hacer clic en 'Iniciar Consulta', el FXMLLoader fallaba por un </VBox> incorrecto en consulta_medica.fxml (debía ser </HBox>).")
    pdf.set_xy(13, note_y + 6)
    pdf.cell(0, 4, "    Esa etiqueta XML mal formada impedía parsear el FXML -> FXMLLoader lanzaba IOException -> aparecía el error de 'conexión a la base de datos'.")
    pdf.set_text_color(0, 0, 0)
    pdf.set_line_width(0.2)


# ─── PÁGINA 2: Flujo Detallado del Médico General ────────────────────────────

def pagina_2(pdf):
    pdf._page_subtitle = "PÁGINA 2  ·  Flujo Detallado: Médico General · Consulta Médica"
    pdf.add_page()

    pdf.set_fill_color(*LGRAY)
    pdf.rect(10, 27, 190, 265, "F")

    CX = 105
    y  = 30

    # ── PANEL MÉDICO ─────────────────────────────────────────────────
    section_bar(pdf, 11, y, 188, 7, "(1) PANEL MÉDICO  (panel_medico.fxml · PanelMedicoController)", DARK)
    y += 11

    box(pdf, 30, y, 150, 20,
        ["MIS CITAS PENDIENTES",
         "Lista de citas con: Fecha · Hora · Nombre del paciente · Motivo",
         "Resumen: Total Pendientes | Citas HOY"],
        BLUE, WHITE, 7.5)
    arr(pdf, CX, y + 20, CX, y + 28)
    y += 28

    # ── BOTÓN INICIAR CONSULTA ───────────────────────────────────────
    box(pdf, CX - 40, y, 80, 10,
        ["Click en [Iniciar Consulta ->]"],
        (20, 90, 180), WHITE, 7.5)
    arr(pdf, CX, y + 10, CX, y + 17)
    y += 17

    # ── FXMLLoader ───────────────────────────────────────────────────
    box(pdf, CX - 55, y, 110, 9,
        ["FXMLLoader carga consulta_medica.fxml", "Instancia ConsultaMedicaController + 6 servicios"],
        GRAY, WHITE, 7)
    arr(pdf, CX, y + 9, CX, y + 14)
    y += 14

    # ── GRAN CAJA DE CONSULTA MÉDICA ─────────────────────────────────
    section_bar(pdf, 11, y, 188, 7, "(2) CONSULTA MÉDICA  (consulta_medica.fxml · ConsultaMedicaController)", BLUE)
    y += 9

    CM_Y   = y
    CM_H   = 172
    CM_W   = 188

    # Marco exterior de la consulta
    pdf.set_fill_color(*LBLUE)
    pdf.set_draw_color(*BLUE)
    pdf.set_line_width(0.7)
    pdf.rect(11, CM_Y, CM_W, CM_H, "FD")
    pdf.set_line_width(0.2)

    # ── Panel izquierdo ──────────────────────────────────────────────
    PIZ_X = 13
    PIZ_W = 62
    PIZ_Y = CM_Y + 3

    section_bar(pdf, PIZ_X, PIZ_Y, PIZ_W, 6, "PANEL IZQUIERDO – Datos del Paciente", (16, 80, 160))

    rows_l = [
        ([" Nombre completo","Correo electrónico"], BLUE, WHITE),
        (["Edad · Tipo de Sangre"], BLUE, WHITE),
        (["Alergias registradas"], BLUE, WHITE),
        (["AUTODIAGNÓSTICOS IA", "(últimos 3 registros)","Síntomas · Gravedad IA"], LBLUE, BLUE),
        (["HISTORIAS CLÍNICAS","PREVIAS (últimas 3)","Fecha · Diagnóstico"], LBLUE, BLUE),
    ]
    ry = PIZ_Y + 9
    for (lines_, bg_, fg_) in rows_l:
        h_ = 7 + (len(lines_) - 1) * 3
        box(pdf, PIZ_X, ry, PIZ_W, h_, lines_, bg_, fg_, 6.5)
        ry += h_ + 2

    # ── Panel derecho ────────────────────────────────────────────────
    PDR_X = 81
    PDR_W = 116
    PDR_Y = CM_Y + 3

    section_bar(pdf, PDR_X, PDR_Y, PDR_W, 6, "PANEL DERECHO – Formulario de Historia Clínica", (16, 80, 160))

    campos = [
        ("(1) ANAMNESIS",      ["Relato del paciente: síntomas, evolución, antecedentes"]),
        ("(2) EXAMEN FÍSICO",  ["Signos vitales, inspección, auscultación, hallazgos"]),
        ("(3) DIAGNÓSTICO",    ["Conclusión clínica (campo de texto)"]),
        ("(4) TRATAMIENTO",    ["Medicamentos, dosis, duración, indicaciones"]),
        ("(5) OBSERVACIONES",  ["Notas adicionales, seguimiento, alertas"]),
    ]
    fy = PDR_Y + 9
    for (titulo, desc) in campos:
        box(pdf, PDR_X, fy, PDR_W, 6, titulo, (20, 60, 130), WHITE, 7)
        fy += 6
        box(pdf, PDR_X, fy, PDR_W, 7, desc, LBLUE, DARK, 6.5)
        fy += 9

    # ÓRDENES MÉDICAS
    section_bar(pdf, PDR_X, fy, PDR_W, 6, "(6) ÓRDENES MÉDICAS", (20, 60, 130))
    fy += 6
    OX = PDR_X
    OW3 = (PDR_W - 4) // 3
    tipos = [
        ([" MEDICAMENTO"],  (30, 60, 180)),
        ([" PROCEDIMIENTO"],(70, 30, 140)),
        ([" EXAMEN"],       (16, 110, 90)),
    ]
    for k, (lns, bg_) in enumerate(tipos):
        box(pdf, OX + k*(OW3+2), fy, OW3, 10, lns + ["Descripción","Observaciones"], bg_, WHITE, 6)
    fy += 12

    # REMISIONES
    section_bar(pdf, PDR_X, fy, PDR_W, 6, "(7) REMISIÓN A ESPECIALISTA", (20, 60, 130))
    fy += 6
    box(pdf, PDR_X, fy, PDR_W, 10,
        ["Especialidad destino · Motivo · Observaciones", "Estado inicial: PENDIENTE (admin la gestiona)"],
        LPURP, PURP, 6.5)
    fy += 12

    # ── Botones de acción ────────────────────────────────────────────
    BTN_Y = CM_Y + CM_H - 17
    box(pdf, PDR_X,            BTN_Y, 55, 12,
        [" GUARDAR", "Historia Clínica"], BLUE, WHITE, 7)
    box(pdf, PDR_X + 59,       BTN_Y, 57, 12,
        ["[OK] COMPLETAR CITA", "y Cerrar Consulta"], GREEN, WHITE, 7)

    # ── Separador vertical interior ──────────────────────────────────
    line_seg(pdf, 79, CM_Y + 3, 79, CM_Y + CM_H - 3, BLUE, 0.6)

    # ── Flujo post-consulta ──────────────────────────────────────────
    POST_Y = CM_Y + CM_H + 3
    section_bar(pdf, 11, POST_Y, 188, 6, "(3) RESULTADO / FIN DEL PROCESO", DARK)
    POST_Y += 9

    # Guardar path
    arr(pdf, PDR_X + 27, CM_Y + CM_H, PDR_X + 27, POST_Y - 1, BLUE, 0.5)
    box(pdf, 15, POST_Y, 78, 16,
        ["GUARDAR",
         "BD: INSERT / UPDATE historia_clinica",
         "Actualiza historial del paciente",
         "-> Permanece en la consulta"],
        BLUE, WHITE, 6.5)

    # Completar path
    arr(pdf, PDR_X + 87, CM_Y + CM_H, PDR_X + 87, POST_Y - 1, GREEN, 0.5)
    box(pdf, 99, POST_Y, 78, 16,
        ["COMPLETAR CITA",
         "BD: UPDATE cita SET estado='COMPLETADA'",
         "-> Regresa a Panel Médico",
         "Cita desaparece de pendientes"],
        GREEN, WHITE, 6.5)


# ─── PÁGINA 3: Flujos Adicionales + Capa de Datos ────────────────────────────

def pagina_3(pdf):
    pdf._page_subtitle = "PÁGINA 3  ·  Flujos: Admin · Especialista · Paciente  +  Capa de Datos"
    pdf.add_page()

    pdf.set_fill_color(*LGRAY)
    pdf.rect(10, 27, 190, 265, "F")

    y = 30

    # ══════════════════════════════════════════════════════════════════
    # SECCIÓN ADMIN (izquierda) + ESPECIALISTA (derecha)
    # ══════════════════════════════════════════════════════════════════
    section_bar(pdf, 11, y, 92, 7, "PANEL ADMINISTRADOR", ORANG)
    section_bar(pdf, 108, y, 91, 7, "PANEL ESPECIALISTA", PURP)
    y += 10

    # Admin módulos (2 columnas dentro de su sección)
    admin_mods = [
        ([" PACIENTES","CRUD + búsqueda","Historial"], LORANG, ORANG),
        ([" MÉDICOS","CRUD + disponibilidad","Especialidades"], LORANG, ORANG),
        ([" CITAS","Agendar · Cancelar","Procesar solicitudes"], LORANG, ORANG),
        ([" DIAGNÓSTICO IA","Análisis con Claude API","Ver resultados"], LORANG, ORANG),
        ([" REMISIONES","Gestión y asignación","a especialistas"], LORANG, ORANG),
        ([" LISTA DE ESPERA","Solicitudes de cita","sin médico asignado"], LORANG, ORANG),
    ]
    AW = 43
    ax = 12
    ay = y
    for k, (lns, bg_, fg_) in enumerate(admin_mods):
        col = k % 2
        row = k // 2
        bx = ax + col * (AW + 2)
        by = ay + row * 18
        box(pdf, bx, by, AW, 14, lns, bg_, fg_, 6.2)
        if k < len(admin_mods) - 2:
            if col == 0:
                pass  # solo mostrar flecha vertical si es módulo solo
        if k % 2 == 1 and k < len(admin_mods) - 1:
            arr(pdf, ax + AW/2, by+14, ax+AW/2, by+17, ORANG, 0.35)
            arr(pdf, ax+AW+2+AW/2, by+14, ax+AW+2+AW/2, by+17, ORANG, 0.35)

    # Nota admin
    note_y_a = ay + 3 * 18 + 2
    pdf.set_fill_color(*LORANG)
    pdf.set_draw_color(*ORANG)
    pdf.rect(12, note_y_a, 90, 8, "FD")
    pdf.set_font("Arial", "B", 6.5)
    pdf.set_text_color(*ORANG)
    pdf.set_xy(14, note_y_a + 2)
    pdf.cell(0, 4, "Notificaciones Admin  ·  Cerrar Sesión -> Login")
    pdf.set_text_color(0, 0, 0)

    # Especialista flow
    esp_mods = [
        ([" REMISIONES ASIGNADAS","Filtra por medicoEspecialistaId"], LPURP, PURP),
        ([" CONSULTA ESPECIALIZADA","Datos paciente · Historia clínica","Órdenes especializadas"], LPURP, PURP),
        ([" ÓRDENES ESPECIALIZADAS","Medicamentos · Procedimientos","Exámenes especializados"], LPURP, PURP),
        ([" ALTA MÉDICA","Diagnóstico final · Medicamentos","Recomendaciones · Próx. cita"], LPURP, PURP),
    ]
    EX = 109
    EW = 89
    ey = y
    for k, (lns, bg_, fg_) in enumerate(esp_mods):
        box(pdf, EX, ey, EW, 14, lns, bg_, fg_, 6.5)
        if k < len(esp_mods) - 1:
            arr(pdf, EX + EW/2, ey + 14, EX + EW/2, ey + 17, PURP, 0.4)
        ey += 17

    # ══════════════════════════════════════════════════════════════════
    # SECCIÓN PACIENTE (ancho completo)
    # ══════════════════════════════════════════════════════════════════
    PAC_Y = y + 3 * 18 + 16
    section_bar(pdf, 11, PAC_Y, 188, 7, "PORTAL PACIENTE  (portal_paciente.fxml · PortalPacienteController)", GREEN)
    PAC_Y += 10

    pac_mods = [
        ([" AUTODIAGNÓSTICO IA","1. Ingresa síntomas","2. Claude API analiza","3. Diagnóstico + Gravedad"], LGREEN, GREEN, 35),
        ([" MIS CITAS","Ver citas pendientes","Solicitar nueva cita","Ver estado de solicitud"], LGREEN, GREEN, 35),
        ([" MIS ÓRDENES","Ver órdenes activas","Medicamentos · Exámenes","Procedimientos"], LGREEN, GREEN, 35),
        ([" NOTIFICACIONES","Alertas del sistema","Citas asignadas","Resultados disponibles"], LGREEN, GREEN, 32),
        ([" MI HISTORIAL","Historias clínicas","Autodiagnósticos previos","Diagnósticos médicos"], LGREEN, GREEN, 35),
    ]
    px = 12
    for (lns, bg_, fg_, pw) in pac_mods:
        box(pdf, px, PAC_Y, pw, 26, lns, bg_, fg_, 6.2)
        px += pw + 3

    # ══════════════════════════════════════════════════════════════════
    # CAPA DE DATOS
    # ══════════════════════════════════════════════════════════════════
    DB_Y = PAC_Y + 32
    section_bar(pdf, 11, DB_Y, 188, 7, "CAPA DE PERSISTENCIA  ·  Arquitectura en Capas", DARK)
    DB_Y += 9

    # Capas
    layers = [
        ("Controlador (JavaFX)", "(PanelMedicoController, ConsultaMedicaController, etc.)", LBLUE, BLUE),
        ("Service (Negocio)",    "(CitaService, HistoriaClinicaService, OrdenMedicaService, etc.)", LBLUE, (20, 60, 120)),
        ("DAO (Acceso a Datos)", "(CitaDAOImpl, OrdenMedicaDAOImpl, RemisionDAOImpl, etc.)", LGRAY, GRAY),
        ("ConexionDB · Singleton JDBC", "(Una sola conexión MySQL compartida por todos los DAOs)", DARK, WHITE),
    ]
    ly = DB_Y
    for (t1, t2, bg_, fg_) in layers:
        box(pdf, 11, ly, 188, 10, [t1, t2], bg_, fg_, 6.8)
        if t1 != layers[-1][0]:
            arr(pdf, CX := 105, ly + 10, 105, ly + 13, DARK, 0.5)
        ly += 13

    # Tablas MySQL
    TBL_Y = ly + 2
    pdf.set_fill_color(240, 245, 250)
    pdf.set_draw_color(*DARK)
    pdf.set_line_width(0.5)
    pdf.rect(11, TBL_Y, 188, 20, "FD")
    pdf.set_font("Arial", "B", 7.5)
    pdf.set_text_color(*DARK)
    pdf.set_xy(11, TBL_Y + 2)
    pdf.cell(188, 5, "BASE DE DATOS MySQL  ·  clinex", align="C")
    pdf.set_font("Arial", "", 6.8)
    pdf.set_text_color(*GRAY)
    tablas = "persona  ·  paciente  ·  medico  ·  usuario  ·  cita  ·  historia_clinica"
    tablas2 = "autodiagnostico  ·  diagnostico  ·  orden_medica  ·  remision"
    pdf.set_xy(11, TBL_Y + 8)
    pdf.cell(188, 4, tablas, align="C")
    pdf.set_xy(11, TBL_Y + 13)
    pdf.cell(188, 4, tablas2, align="C")
    pdf.set_text_color(0, 0, 0)
    pdf.set_line_width(0.2)

    # ── Estados de una cita ──────────────────────────────────────────
    EST_Y = TBL_Y + 24
    section_bar(pdf, 11, EST_Y, 188, 6, "ESTADOS DEL CICLO DE VIDA DE UNA CITA", DARK)
    EST_Y += 9

    estados = [
        ("SOLICITUD",   "Paciente solicita\nsin médico asignado", LYELL, (130, 90, 0)),
        ("PENDIENTE",   "Admin asigna médico\nfecha y hora", LBLUE, BLUE),
        ("COMPLETADA",  "Médico realiza la\nconsulta médica", LGREEN, GREEN),
        ("CANCELADA",   "Cita cancelada por\npaciente o admin", LRED, RED),
    ]
    ew = 44
    ex = 12
    for k, (estado, desc, bg_, fg_) in enumerate(estados):
        box(pdf, ex, EST_Y, ew, 16, [estado, desc], bg_, fg_, 7)
        if k < len(estados) - 1 and k < 2:
            arr(pdf, ex + ew, EST_Y + 8, ex + ew + 3, EST_Y + 8, DARK, 0.5)
        ex += ew + 2

    # Flecha SOLICITUD -> PENDIENTE
    lbl(pdf, 58, EST_Y + 18, "Admin procesa", 5.5, False, GRAY)
    # Flecha PENDIENTE -> COMPLETADA
    lbl(pdf, 103, EST_Y + 18, "Médico atiende", 5.5, False, GRAY)
    # Flecha a CANCELADA
    arr(pdf, 105 - 22 + ew/2, EST_Y + 16, 105 - 22 + ew/2 + ew + 2 + ew/2 + 2, EST_Y + 16, RED, 0.4)
    lbl(pdf, 110, EST_Y + 18, "Cualquier estado", 5.5, False, RED)


# ─── MAIN ─────────────────────────────────────────────────────────────────────

def main():
    pdf = PDF(orientation="P", unit="mm", format="A4")
    pdf.set_auto_page_break(False)
    pdf.set_margins(0, 0, 0)
    pdf.alias_nb_pages()
    pdf._add_fonts()

    pagina_1(pdf)
    pagina_2(pdf)
    pagina_3(pdf)

    out = os.path.join(os.path.dirname(__file__), "CLINEX_Diagrama_Flujo.pdf")
    pdf.output(out)
    print(f"[OK]  PDF generado en: {out}")


if __name__ == "__main__":
    main()
