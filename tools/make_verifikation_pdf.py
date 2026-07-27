#!/usr/bin/env python3
"""Erzeugt den Verifikationsbogen als PDF."""
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (BaseDocTemplate, Frame, KeepTogether, PageTemplate,
                                Paragraph, Spacer, Table, TableStyle)

OUT = "/Users/luisweitl/Programming/Projects/savetheapple-android/VERIFIKATION.pdf"

INK = colors.HexColor("#12171c")
MUTED = colors.HexColor("#5b6672")
ACCENT = colors.HexColor("#0b6b4f")
ALERT = colors.HexColor("#9c2b2b")
RULE = colors.HexColor("#c9d1d9")
BAND = colors.HexColor("#eef2f5")

ss = getSampleStyleSheet()
S = {
    "title": ParagraphStyle("t", parent=ss["Title"], fontName="Helvetica-Bold",
                            fontSize=19, leading=23, textColor=INK, alignment=TA_LEFT,
                            spaceAfter=2),
    "sub": ParagraphStyle("s", fontName="Helvetica", fontSize=9.5, leading=13,
                          textColor=MUTED, spaceAfter=12),
    "h1": ParagraphStyle("h1", fontName="Helvetica-Bold", fontSize=13, leading=16,
                         textColor=INK, spaceBefore=14, spaceAfter=6),
    "h2": ParagraphStyle("h2", fontName="Helvetica-Bold", fontSize=10.5, leading=13,
                         textColor=INK, spaceBefore=9, spaceAfter=3),
    "body": ParagraphStyle("b", fontName="Helvetica", fontSize=9.3, leading=13.2,
                           textColor=INK, spaceAfter=5),
    "small": ParagraphStyle("sm", fontName="Helvetica", fontSize=8.2, leading=11,
                            textColor=MUTED, spaceAfter=4),
    "cell": ParagraphStyle("c", fontName="Helvetica", fontSize=8.6, leading=11.5,
                           textColor=INK),
    "cellb": ParagraphStyle("cb", fontName="Helvetica-Bold", fontSize=8.6, leading=11.5,
                            textColor=INK),
    "mono": ParagraphStyle("m", fontName="Courier-Bold", fontSize=9, leading=12,
                           textColor=ACCENT),
    "alert": ParagraphStyle("a", fontName="Helvetica", fontSize=9.3, leading=13.2,
                            textColor=ALERT, spaceAfter=5),
}


def P(t, s="body"):
    return Paragraph(t, S[s])


def rule(space_before=6, space_after=6):
    t = Table([[""]], colWidths=[170 * mm], rowHeights=[0.5])
    t.setStyle(TableStyle([("LINEBELOW", (0, 0), (-1, -1), 0.6, RULE)]))
    return [Spacer(1, space_before), t, Spacer(1, space_after)]


def table(rows, widths, header=True, aligns=None):
    data = []
    for i, row in enumerate(rows):
        style = "cellb" if (header and i == 0) else "cell"
        data.append([Paragraph(str(c), S[style]) if not isinstance(c, Paragraph) else c
                     for c in row])
    t = Table(data, colWidths=widths, repeatRows=1 if header else 0)
    cmds = [
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LINEBELOW", (0, 0), (-1, -1), 0.4, RULE),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
    ]
    if header:
        cmds += [("BACKGROUND", (0, 0), (-1, 0), BAND),
                 ("LINEBELOW", (0, 0), (-1, 0), 0.8, MUTED)]
    if aligns:
        for col, a in aligns.items():
            cmds.append(("ALIGN", (col, 0), (col, -1), a))
    t.setStyle(TableStyle(cmds))
    return t


def box(title, body_lines, tone="normal"):
    col = ALERT if tone == "alert" else ACCENT
    bg = colors.HexColor("#fdf0f0") if tone == "alert" else colors.HexColor("#eef6f2")
    inner = [Paragraph(f"<b>{title}</b>", S["cellb"])]
    for line in body_lines:
        inner.append(Spacer(1, 3))
        inner.append(Paragraph(line, S["cell"]))
    t = Table([[inner]], colWidths=[170 * mm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), bg),
        ("LINEABOVE", (0, 0), (-1, 0), 2, col),
        ("LEFTPADDING", (0, 0), (-1, -1), 8),
        ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, -1), 7),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
    ]))
    return t


# Helvetica hat kein U+2610; ein gesetztes "[  ]" rendert zuverlaessig als Ankreuzfeld.
CB = '<font face="Courier">[  ]</font>'

story = []
story.append(P("Verifikationsbogen — Escape the KAG", "title"))
story.append(P("Lite-Variante · Stand der Prüfung: alle Rätsel gegen den Quelltext nachgerechnet "
               "und einmal komplett auf einem Samsung SM-A146P (Android 15) durchgespielt.", "sub"))

story.append(box("Wozu dieser Bogen", [
    "Jede Seite beschreibt ein Rätsel: was die App verlangt, woher die Lösung kommt und "
    "was in der Schule dafür stimmen muss.",
    "Hakt die Prüffelder ab. Alles, was ihr nicht abhaken könnt, ist ein Problem für heute Abend.",
    "<b>Es muss nichts mehr ausgedruckt oder ausgelegt werden.</b> Der einzige offene Punkt sind "
    "die Zählwerte in der Schule — Abschnitt 5 (Bäume) und Abschnitt 6 (K · A = 20).",
]))

# ---------------------------------------------------------------- Automatik
story.append(P("1 · Was automatisch geprüft wurde", "h1"))
story.append(P("Das Skript <font face='Courier'>tools/verify_raetsel.py</font> liest die Werte direkt aus dem "
               "Quelltext (MainActivity.kt) und rechnet nach. Ergebnis: <b>21 Prüfungen bestanden, 0 Fehler</b>. "
               "Diese Punkte müsst ihr nicht von Hand kontrollieren — nach jeder Änderung am Spiel einfach "
               "erneut laufen lassen.", "body"))
story.append(table([
    ["Prüfung", "Ergebnis"],
    ["Master-Code = FREIHEIT + 1994 + 60 + APPLE", "618598592019946011616125 — stimmt, genau 24 Ziffern"],
    ["Tutorial = 4 Bäume + Buchstabenwert KAG (19)", "419 — stimmt"],
    ["KAG-Gleichung (K·A·G) + 8122 = 8202", "K·A·G = 80 — stimmt"],
    ["Finale-Riegel verweisen auf echte Lösungen", "alle 4 — stimmt"],
    ["Riegel 4 = letzte 4 Ziffern des Master-Codes", "6125 — stimmt"],
    ["Raumnummern-Rätsel entfernt", "kein ausgedrucktes Material mehr nötig"],
    ["Matrix-Aufgaben überschneiden sich nicht", "10 Felder, keine Dopplung"],
    ["Matrix-Felder liegen im Raster", "alle innerhalb 6×7"],
    ["Letzter Hinweis nennt jeweils die Lösung", "bei allen Rätseln vorhanden"],
], [82 * mm, 88 * mm]))

story.append(P("2 · Die Grundregel des Spiels", "h1"))
story.append(P("Alle Rätsel benutzen dieselbe Regel: <b>Ein Buchstabe zählt so viel wie seine Position "
               "im Alphabet.</b> A=1, B=2, C=3 … Z=26. Die App zeigt dazu eine Tabelle im Handbuch und "
               "über den Eingabefeldern. Andere Verfahren (ASCII, Caesar, Morse) werden im Handbuch "
               "ausdrücklich als <i>nicht verwendet</i> gekennzeichnet.", "body"))

story.append(P("3 · Ablauf im Überblick", "h1"))
story.append(table([
    ["#", "Station", "Eingabe", "Vorbereitung nötig"],
    ["1", "ALARM! (Story)", "—", "—"],
    ["2", "Tutorial", "419", "4 Bäume vor den Musikräumen"],
    ["3", "Der erste Code (KAG-Gleichung)", "8202", "Apfelbilder × Brücken = 20"],
    ["4", "Der digitale Lockdown (Story)", "—", "—"],
    ["5", "Worträtsel", "APPLE", "—"],
    ["6", "Zusammensetzen (Master-Code)", "618598592019946011616125", "—"],
    ["7", "ERDING-Matrix", "1 (nach 3 Mini-Aufgaben)", "—"],
    ["8", "Shutdown (Finale)", "4 Riegel + Halteschalter", "—"],
], [8 * mm, 55 * mm, 52 * mm, 55 * mm]))
story.append(P("Das Handbuch (Geheimschriften, Der Apfelpfarrer, Das KAG in Zahlen) ist kein Teil des "
               "Ablaufs, sondern jederzeit über den <b>?</b>-Knopf erreichbar.", "small"))

# ---------------------------------------------------------------- Raetsel
story.append(P("4 · Tutorial → 419", "h1"))
story.append(P("<b>Was die App verlangt:</b> Zwei Zahlen hintereinanderschreiben (nicht addieren).", "body"))
story.append(table([
    ["Teil", "Herkunft", "Wert"],
    ["Erste Zahl", "Anzahl der Bäume vor den Musikräumen", "4"],
    ["Zweite Zahl", "Buchstabenwert K+A+G = 11+1+7", "19"],
    ["Ergebnis", "„4“ und „19“ aneinandergehängt", "419"],
], [30 * mm, 100 * mm, 40 * mm]))
story.append(P(f"{CB} Vor Ort nachgezählt: es stehen wirklich <b>4 Bäume</b> vor den Musikräumen.<br/>"
               f"{CB} Falls nicht: Anzahl notieren ______ → neue Lösung wäre ______ und 19 = __________", "body"))

story.append(P("5 · Der erste Code (KAG-Gleichung) → 8202", "h1"))
story.append(P("<b>Formel:</b> (K · A · G) + X. Der Rätseltext weist ausdrücklich darauf hin, dass K, A, G "
               "hier <i>Anzahlen</i> sind und nicht die Buchstabenwerte aus dem Tutorial — ohne diesen Hinweis "
               "rechneten Gruppen 11·1·7+8122 = 8199 und lagen unerkennbar knapp daneben.", "body"))
story.append(table([
    ["Variable", "Bedeutung", "Wert", "Quelle"],
    ["K", "Apfel-Sortenbilder vor dem Büro der Schulleitung", "vor Ort zählen", "—"],
    ["A", "unterschiedliche farbige Brücken / Fachgänge", "vor Ort zählen", "—"],
    ["G", "letzte Ziffer des Gründungsjahres 2004", "4", "Schulchronik, Wikipedia"],
    ["X", "Vorwahl Erding 08122 ohne führende Null", "8122", "Telefonbuch, Schulwebsite"],
], [18 * mm, 72 * mm, 28 * mm, 52 * mm]))
story.append(box("Die entscheidende Bedingung", [
    "Aus 8202 − 8122 = 80 und G = 4 folgt zwingend: <b>K · A muss genau 20 ergeben.</b>",
    "Mögliche Kombinationen: <b>5 Bilder × 4 Brücken</b>, 4 × 5, 10 × 2 oder 20 × 1.",
    f"{CB} Bilder gezählt: ______   {CB} Brücken gezählt: ______   {CB} Produkt = 20?",
    "Falls das Produkt nicht 20 ergibt: entweder ein Bild dazuhängen/abnehmen, oder die Lösung im Code "
    "auf (K·A·4)+8122 ändern.",
]))

story.append(P("6 · Worträtsel → APPLE", "h1"))
story.append(P("Wordle-Prinzip, sechs Versuche, fünf Buchstaben. Grün = richtiger Buchstabe an richtiger Stelle, "
               "Gelb = richtiger Buchstabe an falscher Stelle, Grau = kommt nicht vor.", "body"))
story.append(P("<b>Hintergrund:</b> Das Wort ist <b>englisch</b> und verweist auf den Apfelpfarrer Korbinian Aigner, "
               "den Namensgeber der Schule. Der Rätseltext sagt ausdrücklich, dass es englisch ist — sonst tippen "
               "Gruppen naheliegend „APFEL“, das ebenfalls fünf Buchstaben hat.", "body"))
story.append(P(f"{CB} Doppelbuchstabe geprüft: Bei zwei „P“ färbt die App korrekt (Zwei-Pass-Verfahren, getestet).", "body"))

story.append(P("7 · Master-Code → 618598592019946011616125", "h1"))
story.append(P("Die vier Codefragmente werden aneinandergehängt, Buchstaben dabei in ihre Alphabet-Position "
               "umgewandelt, ohne führende Nullen. In der Lite-Variante nennt die Lockdown-Story die ersten drei "
               "Fragmente selbst — nur das vierte muss erspielt werden.", "body"))
story.append(table([
    ["Fragment", "Herkunft", "wird zu"],
    ["FREIHEIT", "von der App vorgegeben (Story)", "6-18-5-9-8-5-9-20 → 6185985920"],
    ["1994", "von der App vorgegeben (Story)", "1994"],
    ["60", "von der App vorgegeben (Story)", "60"],
    ["APPLE", "aus dem Worträtsel erspielt", "1-16-16-12-5 → 11616125"],
    ["<b>Gesamt</b>", "", "<b>618598592019946011616125</b> (24 Ziffern)"],
], [26 * mm, 66 * mm, 78 * mm]))
story.append(P(f"{CB} Selbst nachgerechnet. Die Längenangabe „24 Ziffern“ im Rätseltext dient den Kindern als Selbstkontrolle.", "body"))

story.append(P("8 · ERDING-Matrix → 1", "h1"))
story.append(P("Ein Raster aus 7 Zeilen × 6 Spalten. Die Spalten tragen das Wort ERDING, die Zeilen sind 1–7 "
               "nummeriert; ein Feld heißt also z. B. D2. Das Raster startet dunkel. Drei Mini-Aufgaben nennen "
               "die Felder, die angetippt werden müssen.", "body"))
story.append(table([
    ["Aufgabe", "Fragestellung", "Lösung", "Felder"],
    ["1 — Der Stamm", "Welcher Buchstabe hat den Wert 9? Zeilen 1–6 in seiner Spalte.", "I", "I1–I6"],
    ["2 — Die Fahne", "2 × 2 ergibt den Spaltenbuchstaben, die Hälfte von 4 die Zeile.", "D, 2", "D2"],
    ["3 — Der Fuß", "Zeile 7, Spalten mit den Werten 4, 9 und 14.", "D, I, N", "D7, I7, N7"],
], [26 * mm, 82 * mm, 22 * mm, 40 * mm]))
story.append(P("Spaltenwerte zur Kontrolle: <b>E=5, R=18, D=4, I=9, N=14, G=7</b>. "
               "Zusammen ergeben die zehn Felder die Ziffer <b>1</b>:", "body"))
grid_rows = ["...X..", "..XX..", "...X..", "...X..", "...X..", "...X..", "..XXX."]
gdata = [[""] + list("ERDING")]
for i, r in enumerate(grid_rows, start=1):
    gdata.append([str(i)] + ["■" if ch == "X" else "·" for ch in r])
gt = Table(gdata, colWidths=[8 * mm] + [8 * mm] * 6, rowHeights=[6 * mm] * 8)
gt.setStyle(TableStyle([
    ("ALIGN", (0, 0), (-1, -1), "CENTER"),
    ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
    ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
    ("FONTNAME", (0, 1), (0, -1), "Helvetica-Bold"),
    ("FONTSIZE", (0, 0), (-1, -1), 9),
    ("TEXTCOLOR", (1, 1), (-1, -1), ACCENT),
    ("GRID", (1, 1), (-1, -1), 0.3, RULE),
]))
story.append(gt)
story.append(Spacer(1, 4))
story.append(P(f"{CB} Falsch getippte Felder blitzen rot auf, kosten aber <b>kein Leben</b>. Nach zwei Fehltipps "
               "erscheint automatisch ein Tipp mit den Koordinaten. Beides auf dem Gerät getestet.", "body"))

# --------------------------------------------------------- RAUMNUMMERN
story.append(P("9 · Finale → vier Riegel", "h1"))
story.append(P("Das Finale fragt vier Codes ab, die die Gruppe unterwegs schon geknackt hat. Sie brauchen also "
               "ihre Notizen. Danach muss ein roter Schalter 1,6 Sekunden gedrückt gehalten werden.", "body"))
story.append(table([
    ["Riegel", "Verlangt", "Antwort", "Kommt aus"],
    ["1", "Trainings-Code", "419", "Station 2"],
    ["2", "Ergebnis der Formel", "8202", "Station 3"],
    ["3", "Englisches Wort", "APPLE", "Station 5"],
    ["4", "Letzte vier Ziffern des Master-Codes", "6125", "Station 6"],
], [18 * mm, 68 * mm, 32 * mm, 52 * mm]))
story.append(P(f"{CB} Nach zwei Fehlversuchen erscheint automatisch ein Tipp.<br/>"
               f"{CB} Zusätzlich gibt es den Knopf „Lösung zeigen (spicken)“ — er nennt alle vier Riegel.", "body"))

story.append(P("10 · Was beim Testlauf geprüft wurde", "h1"))
story.append(table([
    ["Geprüft", "Ergebnis"],
    ["Kompletter Durchlauf mit echten Antworten", "Alle 8 Stationen bis zum Abspann, Leben blieben bei 3"],
    ["Handbuch mitten im Rätsel öffnen", "Fortschritt bleibt jetzt erhalten (war vorher ein Fehler)"],
    ["Falsches Feld in der Matrix antippen", "Rotes Aufblitzen, kein Leben verloren"],
    ["Automatischer Tipp nach zwei Fehltipps", "Erscheint"],
    ["Halteschalter im Finale", "1,6 s, Balken füllt von links"],
    ["Nach dem Durchspielen", "Menü zeigt wieder „Spiel starten“ — nächste Gruppe startet frisch"],
], [70 * mm, 100 * mm]))

story.append(P("11 · Bekannte Einschränkungen", "h1"))
story.append(P("Ehrlich benannt, damit heute Abend niemand überrascht wird:", "body"))
story.append(table([
    ["Punkt", "Bedeutung für den Abend"],
    ["Absturz-Knopf auf dem Fehlschlag-Bildschirm",
     "Beendet die App absichtlich („der Hacker schlägt zurück“). Der Spielstand bleibt erhalten. "
     "Wer das nicht will: Aufsicht bittet, den Knopf nicht anzutippen."],
    ["Entwickler-Werkzeuge nur im Debug-Build",
     "Für die Aufsicht das Debug-APK verwenden: im Menü 5× auf den Totenkopf tippen schaltet "
     "Überspringen / Leben auffüllen frei."],
    ["K und A nicht verifizierbar",
     "Gründungsjahr (2004) und Vorwahl (08122) sind belegt. Die Anzahl der Bilder und Brücken "
     "muss jemand vor Ort nachzählen — siehe Abschnitt 5."],
], [55 * mm, 115 * mm]))

story.append(Spacer(1, 10))
story.append(P("Geprüft von: ______________________________     Datum: ______________", "body"))


def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 7.5)
    canvas.setFillColor(MUTED)
    canvas.drawString(20 * mm, 12 * mm, "Escape the KAG · Verifikationsbogen · Lite-Variante")
    canvas.drawRightString(190 * mm, 12 * mm, f"Seite {doc.page}")
    canvas.setStrokeColor(RULE)
    canvas.setLineWidth(0.4)
    canvas.line(20 * mm, 15 * mm, 190 * mm, 15 * mm)
    canvas.restoreState()


doc = BaseDocTemplate(OUT, pagesize=A4,
                      leftMargin=20 * mm, rightMargin=20 * mm,
                      topMargin=18 * mm, bottomMargin=20 * mm,
                      title="Verifikationsbogen — Escape the KAG",
                      author="Escape the KAG")
frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="f")
doc.addPageTemplates([PageTemplate(id="main", frames=[frame], onPage=footer)])
doc.build(story)
print("PDF geschrieben:", OUT)
