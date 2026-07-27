# Rätsel-Übersicht — Escape the KAG (Lite)

> **Lite-Variante.** Dieser Branch enthält nur Rätsel, für die **kein Material in der Schule ausgelegt** werden muss. Entfernt wurden Bücher-Check, Schatten-Rätsel, Spiegel-Code und Psychologie-Zettel. Der volle Umfang liegt auf `main`.

Alle Stationen in Spielreihenfolge. Die Reihenfolge entspricht der Liste `gameNodes` in `app/src/main/java/de/kagerding/savetheapple/android/MainActivity.kt` — der Spielfortschritt `currentStage` ist direkt der Index in dieser Liste (erstes Element = Stage 0).

Vorbereitung für den Einsatz: siehe **[VORBEREITUNG.md](VORBEREITUNG.md)**.

---

## Lösungen im Überblick

| Stage | Station | Lösung | Vorbereitung nötig |
| --- | --- | --- | --- |
| 0 | ALARM! (Story) | — | — |
| 1 | Tutorial | `419` | 4 Bäume vor den Musikräumen |
| 2 | Der erste Code (KAG-Gleichung) | `8202` | Apfelbilder + Brücken zählbar |
| 3 | Der digitale Lockdown (Story) | — | — |
| 4 | Worträtsel (Wordle) | `APPLE` | — |
| 5 | Zusammensetzen (Master-Code) | `618598592019946011616125` | — |
| 6 | ERDING-Matrix | `1` | — |
| 7 | Raumnummern | `00245` | Kurs-/Raumliste |
| 8 | Shutdown (Finale) | 5 Riegel, siehe unten | — |

Das **Handbuch** (Geheimschriften, Der Apfelpfarrer, Das KAG in Zahlen) ist kein Teil des Ablaufs mehr, sondern **jederzeit erreichbar** — im Hauptmenü über „Handbuch", im Spiel über den `?`-Knopf oben links.

Nur **drei Stationen** brauchen überhaupt noch Vorbereitung: Stage 1 und 2 (Dinge, die in der Schule zählbar sein müssen) und Stage 7 (eine ausgedruckte Liste). Alles andere läuft komplett im Tablet.

Die Antworteingabe ist tolerant: Groß-/Kleinschreibung, Leerzeichen und Satzzeichen werden ignoriert (`normalizeAnswer()`).

---

## Die Rätsel im Einzelnen

### Stage 0 — ALARM! *(Intro)*
`intro` · keine Eingabe

Story-Einstieg: Das Schulnetzwerk wurde gehackt, der Computerraum ist verriegelt.

---

### Stage 1 — Tutorial *(Training)*
`tutorial` · **`419`** · zeigt die Buchstaben-Tabelle

Führt die zentrale Spielregel ein: **Jeder Buchstabe zählt so viel wie seine Position im Alphabet** (A=1 … Z=26).

- Zahl 1: Anzahl der Bäume vor den Musikräumen → **4**
- Zahl 2: Buchstabenwert von K+A+G = 11+1+7 → **19**
- Beide Zahlen *hintereinanderschreiben* (nicht addieren) → **419**

---

### Stage 2 — Der erste Code *(KAG-Gleichung)*
`kag_formula` · **`8202`** · Hilfsfelder K, A, G, X

Formel: **(K · A · G) + X**

| Variable | Bedeutung | Wert |
| --- | --- | --- |
| K | Apfel-Sortenbilder vor dem Büro der Schulleitung | vor Ort zählen |
| A | unterschiedliche farbige Brücken/Fachgänge | vor Ort zählen |
| G | letzte Ziffer des Gründungsjahres **2004** | **4** |
| X | Vorwahl Erding **08122** ohne führende Null | **8122** |

Daraus folgt zwingend: **K · A muss 20 ergeben** (8202 − 8122 = 80, und 80 ÷ 4 = 20).

> ⚠️ Der Text weist ausdrücklich darauf hin, dass K, A, G hier **Anzahlen** sind und *nicht* die Buchstabenwerte aus dem Tutorial.

---

### Stage 3 — Der digitale Lockdown *(Story)*
`lockdown_intro` · keine Eingabe

**In der Lite-Variante die entscheidende Station.** Die Story erklärt, dass der Master-Key aus vier Fragmenten besteht und das System drei davon aus dem Speicher retten konnte:

- Fragment 1: **FREIHEIT** · Fragment 2: **1994** · Fragment 3: **60**

Fragment 4 fehlt und wird im Worträtsel (Stage 4) geknackt. So bleibt der Master-Code identisch zur Vollversion, ohne dass jemand Zettel auslegen muss.

---

### Stage 4 — Worträtsel *(Wordle)*
`wordle` · **`APPLE`** (Konstante `WORDLE_TARGET`) · 6 Versuche

Wordle-Minispiel im Terminal. Grün = richtiger Buchstabe an richtiger Stelle, Gelb = richtiger Buchstabe an falscher Stelle, Grau = kommt nicht vor. Doppelbuchstaben (das **PP**) werden korrekt ausgewertet.

> Der Text sagt ausdrücklich, dass das Wort **englisch** ist — sonst tippen Gruppen naheliegend „APFEL".

---

### Stage 5 — Zusammensetzen *(Master-Code)*
`compile` · **`618598592019946011616125`** · zeigt die Buchstaben-Tabelle

```
FREIHEIT → 6-18-5-9-8-5-9-20 → 6185985920
1994     →                     1994
60       →                     60
APPLE    → 1-16-16-12-5      → 11616125
                    ────────────────────────────
             618598592019946011616125   (24 Ziffern)
```

Die Fragmente werden im Rätseltext noch einmal genannt — niemand muss sie sich merken.

---

### Stage 6 — ERDING-Matrix
`erding_matrix` · **`1`** · zeigt das Raster in der App

Raster aus 7 Zeilen × 6 Spalten, Spaltenköpfe **E-R-D-I-N-G**. Die grün leuchtenden Felder zeichnen aus der Entfernung betrachtet die Ziffer **1**.

Muster in `ERDING_PATTERN` (MainActivity.kt):

```
...X..
..XX..
...X..
...X..
...X..
...X..
..XXX.
```

> Wer das Muster ändert, muss auch `acceptedAnswers` dieses Knotens anpassen.

---

### Stage 7 — Raumnummern
`room_sorting` · **`00245`**

Raumnummern der Größe nach sortieren, dann alle Ziffern aneinanderhängen — **führende Nullen bleiben erhalten**. Beispiel: Räume `002`, `4`, `5` → sortiert → `00245`.

*Die einzige Station, für die noch etwas ausgedruckt werden muss.*

---

### Stage 8 — Shutdown *(Finale)*
`finale` · eigene Mechanik (`FinaleGate`), läuft **nicht** über `acceptedAnswers`

Das Finale baut auf allen vorherigen Rätseln auf und läuft in drei Phasen.

**Phase 1 — Fünf Riegel.** Jeder Riegel fragt einen Code ab, den die Gruppe unterwegs schon geknackt hat. Eine Leiste oben zeigt `ZU` / `OFFEN` pro Riegel.

| Riegel | Verlangt | Antwort | Herkunft |
| --- | --- | --- | --- |
| 1 | Trainings-Code | `419` | Stage 1 |
| 2 | Ergebnis der Formel | `8202` | Stage 2 |
| 3 | Englisches Wort | `APPLE` | Stage 4 |
| 4 | Sortierte Raumnummern | `00245` | Stage 7 |
| 5 | Letzte vier Ziffern des Master-Codes | `6125` | Stage 5 |

Nach **zwei** Fehlversuchen an einem Riegel blendet sich automatisch ein Tipp ein. Ein Fehlversuch kostet ein Leben.

**Phase 2 — Notabschaltung.** Ein roter Schalter muss **1,6 Sekunden gedrückt gehalten** werden (`HOLD_TO_SHUTDOWN_SECONDS`); ein Balken füllt sich von links. Loslassen lässt ihn in 0,45 s zurückfallen. Der Fortschritt wird aus der real vergangenen Zeit gerechnet, nicht aus Frames — sonst dauert das Halten je nach Gerät unterschiedlich lang.

**Phase 3 — Sieg.** Sechs Terminal-Zeilen erscheinen nacheinander, dann „KAG GERETTET" und der Knopf *Abspann ansehen*, der in die Credits führt.

Die Riegel-Antworten stehen in `FINALE_LOCKS`, die Siegzeilen in `FINALE_VICTORY_LINES`.

---

## Das Handbuch

Jederzeit erreichbar, auch mitten in einem Rätsel — der Spielstand bleibt erhalten. Definiert in `manualNodes`.

| Eintrag | Inhalt |
| --- | --- |
| **Geheimschriften** | Stellt klar: Für die Rätsel gilt ausschließlich A=1 … Z=26. Caesar, ASCII, Morse und Telefon-Tastatur werden erklärt, aber ausdrücklich als *im Spiel nicht verwendet* gekennzeichnet. Zeigt die Buchstaben-Tabelle. |
| **Der Apfelpfarrer** | Korbinian Aigner (1885 Hohenpolding – 1966 Freising): Verzicht auf das Hoferbe, Pomologe, rund 900 Sortenbilder, Widerstand gegen die Nationalsozialisten, KZ Dachau, die Sorten KZ-1 bis KZ-4, der Korbiniansapfel (1985), Flucht 1945, der Häftlingsmantel auf dem Sarg, Sammlung im Historischen Archiv der TU München, documenta 13. |
| **Das KAG in Zahlen** | Eröffnung 2004 als „Gymnasium Erding II", Kreistagsbeschluss 28.06.2010, Namensgebungsfeier Februar 2011, Sigwolfstraße 50 / 85435 Erding, Vorwahl 08122, 1211 Schülerinnen und Schüler und 92 Lehrkräfte (2024/25), drei Ausbildungsrichtungen, 3,3 ha Gelände — dazu Erding: Ersterwähnung 788, Stadtrechte 1228, Schöner Turm 1408, Flughafen München im Erdinger Moos. |

---

## Spielmechanik

**Hinweis-System.** Jedes lösbare Rätsel hat bis zu drei gestaffelte Hinweise; der letzte nennt die Lösung („Lösung zeigen (spicken)"). Hinweise kosten **kein** Leben. Der Knopf sitzt direkt unter „Code prüfen" und ist ohne Scrollen erreichbar.

**Leben.** 3 Leben (`MAX_LIVES`). Bei drei Fehlversuchen erscheint der Fehlschlag-Bildschirm; der **Fortschritt bleibt erhalten** und die Gruppe startet mit vollen Leben beim aktuellen Rätsel neu.

**Phasen.**

| Phase | Label | Stages |
| --- | --- | --- |
| Intro | INTRO-SEQUENZ | 0 |
| Tutorial | TRAININGSMODUS | 1 |
| Mission | MISSION | 2–7 |
| Finale | FINALE | 8 |
| Archiv | ARCHIV | nur im Handbuch |

**Entwickler-Werkzeuge.** Nur im Debug-Build: im Hauptmenü 5× auf den Totenkopf tippen. Danach: Rätsel überspringen, Leben auffüllen, Unverwundbarkeit.

**App-Icon & Kategorie.** Das Launcher-Icon ist das KAG-Logo (`kag_logo.png`) als adaptives Icon; die App meldet sich mit `android:appCategory="game"` als Spiel an.

**Baseline Profiles.** Das Modul `:baselineprofile` erzeugt ein Startprofil, damit die App flüssiger startet. Neu erzeugen mit `./gradlew :app:generateBaselineProfile` bei angeschlossenem Gerät; das Ergebnis landet in `app/src/release/generated/baselineProfiles/`.

---

## Unterschied zur Vollversion (`main`)

| Station | main | lite |
| --- | --- | --- |
| Bücher-Check (`FREIHEIT`) | Rätsel mit Buch + Zettel | entfernt, Fragment wird vorgegeben |
| Schatten-Rätsel (`1994`) | Rätsel mit Zettel am Fenster | entfernt, Fragment wird vorgegeben |
| Spiegel-Code (`60`) | Rätsel mit Spiegel + Tasche | entfernt, Fragment wird vorgegeben |
| Psychologie-Zettel (`MESPACE`) | Rätsel mit Zettel | ersatzlos entfernt |
| Archiv | drei Stationen im Ablauf | Handbuch, jederzeit erreichbar |
| Finale | nur Abschlusstext | fünf Riegel + Notabschaltung + Siegsequenz |
| Stationen im Ablauf | 16 | 9 |

*Quelle: `MainActivity.kt` (Listen `gameNodes`, `manualNodes`, `FINALE_LOCKS`).*
