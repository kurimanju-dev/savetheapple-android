#!/usr/bin/env python3
"""Erzeugt RAETSEL.pdf - eine schlichte Auflistung aller Raetsel."""
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import (BaseDocTemplate, Frame, PageTemplate, Paragraph,
                                Spacer, Table, TableStyle)

OUT = "/Users/luisweitl/Programming/Projects/savetheapple-android/RAETSEL.pdf"

INK = colors.HexColor("#000000")
MUTED = colors.HexColor("#666666")
RULE = colors.HexColor("#cccccc")

S = {
    "title": ParagraphStyle("t", fontName="Helvetica-Bold", fontSize=15, leading=18,
                            textColor=INK, alignment=TA_LEFT, spaceAfter=10),
    "h": ParagraphStyle("h", fontName="Helvetica-Bold", fontSize=10.5, leading=13,
                        textColor=INK, spaceBefore=12, spaceAfter=4),
    "cell": ParagraphStyle("c", fontName="Helvetica", fontSize=9, leading=12,
                           textColor=INK),
    "cellb": ParagraphStyle("cb", fontName="Helvetica-Bold", fontSize=9, leading=12,
                            textColor=INK),
    "sol": ParagraphStyle("s", fontName="Courier-Bold", fontSize=9, leading=12,
                          textColor=INK),
    "note": ParagraphStyle("n", fontName="Helvetica", fontSize=8.5, leading=11.5,
                           textColor=MUTED, spaceAfter=3),
}


def cell(t, style="cell"):
    return Paragraph(t, S[style])


def grid_table(rows, widths, header=True):
    data = []
    for i, row in enumerate(rows):
        out = []
        for c in row:
            if isinstance(c, Paragraph):
                out.append(c)
            else:
                out.append(cell(str(c), "cellb" if (header and i == 0) else "cell"))
        data.append(out)
    t = Table(data, colWidths=widths, repeatRows=1 if header else 0)
    cmds = [
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LINEBELOW", (0, 0), (-1, -1), 0.4, RULE),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
        ("LEFTPADDING", (0, 0), (-1, -1), 4),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
    ]
    if header:
        cmds.append(("LINEBELOW", (0, 0), (-1, 0), 0.9, INK))
    t.setStyle(TableStyle(cmds))
    return t


story = [Paragraph("Escape the KAG — alle Rätsel", S["title"])]

story.append(grid_table([
    ["Nr", "Station", "Lösung", "Was zu tun ist"],
    ["1", "ALARM!", "—",
     "Story. Das Schulnetzwerk wurde gehackt, der Computerraum ist verriegelt."],
    ["2", "Tutorial", cell("419", "sol"),
     "Bäume vor den Musikräumen zählen (4) und den Buchstabenwert von K+A+G "
     "ausrechnen (11+1+7=19). Beide Zahlen hintereinanderschreiben."],
    ["3", "Der erste Code", cell("8202", "sol"),
     "Formel (K · A · G) + X. K = Apfel-Sortenbilder vor dem Büro der Schulleitung, "
     "A = farbige Brücken, G = 4 (letzte Ziffer von 2004), X = 8122 (Vorwahl Erding "
     "ohne führende Null). K · A muss 20 ergeben."],
    ["4", "Der digitale Lockdown", "—",
     "Story. Nennt drei Codefragmente: FREIHEIT, 1994 und 60."],
    ["5", "Worträtsel", cell("APPLE", "sol"),
     "Wordle mit fünf Buchstaben und sechs Versuchen. Das Wort ist englisch."],
    ["6", "Zusammensetzen", cell("618598592019946011616125", "sol"),
     "Die vier Fragmente aneinanderhängen, Buchstaben als Alphabetposition: "
     "FREIHEIT = 6185985920, dann 1994, dann 60, dann APPLE = 11616125. "
     "Ergibt 24 Ziffern."],
    ["7", "ERDING-Matrix", cell("1", "sol"),
     "Raster 7 × 6, Spalten E R D I N G. Drei Aufgaben nennen die Felder, "
     "die angetippt werden müssen. Zusammen ergeben sie die Ziffer 1."],
    ["8", "Shutdown (Finale)", "4 Riegel",
     "Vier Codes erneut eingeben (siehe unten), danach den roten Schalter "
     "1,6 Sekunden gedrückt halten."],
], [9 * mm, 34 * mm, 40 * mm, 87 * mm]))

story.append(Paragraph("ERDING-Matrix — die drei Aufgaben", S["h"]))
story.append(grid_table([
    ["Aufgabe", "Fragestellung", "Felder"],
    ["1 Der Stamm", "Welcher Buchstabe hat den Wert 9? Zeilen 1–6 in seiner Spalte.",
     cell("I1 – I6", "sol")],
    ["2 Die Fahne", "2 × 2 ergibt den Spaltenbuchstaben, die Hälfte von 4 die Zeile.",
     cell("D2", "sol")],
    ["3 Der Fuß", "Zeile 7, Spalten mit den Werten 4, 9 und 14.",
     cell("D7, I7, N7", "sol")],
], [24 * mm, 108 * mm, 38 * mm]))

grid_rows = ["...X..", "..XX..", "...X..", "...X..", "...X..", "...X..", "..XXX."]
gdata = [[""] + list("ERDING")]
for i, r in enumerate(grid_rows, start=1):
    gdata.append([str(i)] + ["■" if ch == "X" else "·" for ch in r])
gt = Table(gdata, colWidths=[7 * mm] + [7 * mm] * 6, rowHeights=[5.5 * mm] * 8)
gt.setStyle(TableStyle([
    ("ALIGN", (0, 0), (-1, -1), "CENTER"),
    ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
    ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
    ("FONTNAME", (0, 1), (0, -1), "Helvetica-Bold"),
    ("FONTSIZE", (0, 0), (-1, -1), 8.5),
    ("GRID", (1, 1), (-1, -1), 0.3, RULE),
]))
story.append(Spacer(1, 4))
story.append(gt)

story.append(Paragraph("Finale — die vier Riegel", S["h"]))
story.append(grid_table([
    ["Riegel", "Antwort", "Kommt aus"],
    ["1", cell("419", "sol"), "Tutorial"],
    ["2", cell("8202", "sol"), "Der erste Code"],
    ["3", cell("APPLE", "sol"), "Worträtsel"],
    ["4", cell("6125", "sol"), "letzte vier Ziffern des Master-Codes"],
], [18 * mm, 42 * mm, 110 * mm]))

story.append(Spacer(1, 6))
story.append(Paragraph(
    "Buchstabenwerte: A=1, B=2, C=3 … Z=26 &nbsp;·&nbsp; "
    "Spalten der Matrix: E=5, R=18, D=4, I=9, N=14, G=7", S["note"]))

doc = BaseDocTemplate(OUT, pagesize=A4,
                      leftMargin=18 * mm, rightMargin=18 * mm,
                      topMargin=16 * mm, bottomMargin=16 * mm,
                      title="Escape the KAG — alle Rätsel")
doc.addPageTemplates([PageTemplate(
    id="main",
    frames=[Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="f")])])
doc.build(story)
print("PDF geschrieben:", OUT)
