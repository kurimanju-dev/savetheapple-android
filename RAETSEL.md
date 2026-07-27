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
| 8 | Geheimschriften (Archiv) | — | — |
| 9 | Der Apfelpfarrer (Archiv) | — | — |
| 10 | Das KAG in Zahlen (Archiv) | — | — |
| 11 | Finale | — | — |

Nur **zwei Stationen** brauchen überhaupt noch Vorbereitung: Stage 1/2 (Dinge, die in der Schule zählbar sein müssen) und Stage 7 (eine ausgedruckte Liste). Alles andere läuft komplett im Tablet.

Die Antworteingabe ist tolerant: Groß-/Kleinschreibung, Leerzeichen und Satzzeichen werden ignoriert (`normalizeAnswer()`).

---

## Die Rätsel im Einzelnen

### Stage 0 — ALARM! *(Intro)*
`intro` · Phase Intro · keine Eingabe

Story-Einstieg: Das Schulnetzwerk wurde gehackt, der Computerraum ist verriegelt.

---

### Stage 1 — Tutorial *(Training)*
`tutorial` · Phase Tutorial · **`419`** · zeigt die Buchstaben-Tabelle

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

- Fragment 1: **FREIHEIT**
- Fragment 2: **1994**
- Fragment 3: **60**

Fragment 4 fehlt und wird im Worträtsel (Stage 4) geknackt. So bleibt der Master-Code identisch zur Vollversion, ohne dass jemand Zettel auslegen muss.

---

### Stage 4 — Worträtsel *(Wordle)*
`wordle` · **`APPLE`** (Konstante `WORDLE_TARGET`) · 6 Versuche

Wordle-Minispiel im Terminal. Grün = richtiger Buchstabe an richtiger Stelle, Gelb = richtiger Buchstabe an falscher Stelle, Grau = kommt nicht vor. Doppelbuchstaben (das **PP**) werden korrekt ausgewertet.

> Der Text sagt ausdrücklich, dass das Wort **englisch** ist — sonst tippen Gruppen naheliegend „APFEL".

Nach 6 Fehlversuchen: ein Leben weg, Board wird geleert.

---

### Stage 5 — Zusammensetzen *(Master-Code)*
`compile` · **`618598592019946011616125`** · zeigt die Buchstaben-Tabelle

Die vier Fragmente in Reihenfolge ihrer Nummern aneinanderhängen, Buchstaben dabei in ihre Alphabet-Position umwandeln, ohne führende Nullen:

```
FREIHEIT → 6-18-5-9-8-5-9-20 → 6185985920
1994     →                     1994
60       →                     60
APPLE    → 1-16-16-12-5      → 11616125
                    ────────────────────────────
             618598592019946011616125   (24 Ziffern)
```

Die Fragmente werden im Rätseltext noch einmal genannt — niemand muss sie sich merken. Die eigentliche Aufgabe (Umwandeln und Aneinanderhängen) bleibt erhalten.

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

Liste aus Kursen und ihren vollständigen Raumnummern. Raumnummern der Größe nach sortieren, dann alle Ziffern aneinanderhängen — **führende Nullen bleiben erhalten**.

Beispielhafte Liste, die 00245 ergibt: Räume `002`, `4`, `5` → sortiert 002 / 4 / 5 → `00245`.

*Die einzige Station der Lite-Variante, für die noch etwas ausgedruckt werden muss.*

---

### Stage 8 — Geheimschriften *(Archiv)*
`cipher_training` · keine Eingabe · zeigt die Buchstaben-Tabelle

Nachschlagewerk. Stellt klar: **Für die Rätsel gilt ausschließlich A=1 … Z=26.** Caesar, ASCII, Morse und Telefon-Tastatur werden erklärt, aber ausdrücklich als *im Spiel nicht verwendet* gekennzeichnet.

---

### Stage 9 — Der Apfelpfarrer *(Archiv)*
`aigner_archive` · keine Eingabe

Die Geschichte des Namensgebers Korbinian Aigner (1885 Hohenpolding – 1966 Freising): Verzicht auf das Hoferbe, Pomologe, rund 900 Sortenbilder (1912–1960), Widerstand gegen die Nationalsozialisten, KZ Dachau, die vier Apfelsorten KZ-1 bis KZ-4, der Korbiniansapfel (1985), Flucht 1945, der Häftlingsmantel auf dem Sarg, die Sammlung im Historischen Archiv der TU München und die documenta 13 (2012).

---

### Stage 10 — Das KAG in Zahlen *(Archiv)*
`school_clues` · keine Eingabe

Recherchierte Fakten zu Schule und Stadt: Eröffnung 2004 als „Gymnasium Erding II", Kreistagsbeschluss zur Umbenennung am 28.06.2010, Namensgebungsfeier Februar 2011, Sigwolfstraße 50 / 85435 Erding, Vorwahl 08122, 1211 Schülerinnen und Schüler und 92 Lehrkräfte (2024/25), drei Ausbildungsrichtungen, 3,3 ha Gelände, Auszeichnungen — dazu Erding: Ersterwähnung 788, Stadtrechte 1228, Schöner Turm von 1408, Flughafen München im Erdinger Moos.

---

### Stage 11 — Finale
`finale` · keine Eingabe

Abschlusstext: Master-Code eingesetzt, Computerraum erreicht, Hackerverbindung getrennt.

> Das Finale ist bewusst noch nicht als eigenes Rätsel ausgebaut.

---

## Spielmechanik

**Hinweis-System.** Jedes lösbare Rätsel hat bis zu drei gestaffelte Hinweise; der letzte nennt die Lösung („Lösung zeigen (spicken)"). Hinweise kosten **kein** Leben.

**Leben.** 3 Leben (`MAX_LIVES`). Bei drei Fehlversuchen erscheint der Fehlschlag-Bildschirm; der **Fortschritt bleibt erhalten** und die Gruppe startet mit vollen Leben beim aktuellen Rätsel neu.

**Phasen.**

| Phase | Label | Stages |
| --- | --- | --- |
| Intro | INTRO-SEQUENZ | 0 |
| Tutorial | TRAININGSMODUS | 1 |
| Mission | MISSION | 2–7 |
| Archiv | ARCHIV | 8–10 |
| Finale | FINALE | 11 |

**Entwickler-Werkzeuge.** Nur im Debug-Build: im Hauptmenü 5× auf den Totenkopf tippen. Danach: Rätsel überspringen, Leben auffüllen, Unverwundbarkeit.

---

## Unterschied zur Vollversion (`main`)

| Station | main | lite |
| --- | --- | --- |
| Bücher-Check (`FREIHEIT`) | Rätsel mit Buch + Zettel | entfernt, Fragment wird vorgegeben |
| Schatten-Rätsel (`1994`) | Rätsel mit Zettel am Fenster | entfernt, Fragment wird vorgegeben |
| Spiegel-Code (`60`) | Rätsel mit Spiegel + Tasche | entfernt, Fragment wird vorgegeben |
| Psychologie-Zettel (`MESPACE`) | Rätsel mit Zettel | ersatzlos entfernt |
| Master-Code | Fragmente selbst erspielt | drei Fragmente vorgegeben, Lösung identisch |
| Stationen gesamt | 16 | 12 |

*Quelle: `MainActivity.kt` (Liste `gameNodes`).*
