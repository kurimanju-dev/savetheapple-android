# Rätsel-Übersicht — Escape the KAG

Alle Stationen des Spiels in Spielreihenfolge. Die Reihenfolge entspricht der Liste `gameNodes` in `app/src/main/java/de/kagerding/savetheapple/android/MainActivity.kt` — der Spielfortschritt `currentStage` ist direkt der Index in dieser Liste (erstes Element = Stage 0).

Vorbereitung für den Einsatz an der Schule: siehe **[VORBEREITUNG.md](VORBEREITUNG.md)**.

---

## Lösungen im Überblick

| Stage | Station | Lösung | Material nötig |
| --- | --- | --- | --- |
| 0 | ALARM! (Story) | — | — |
| 1 | Tutorial | `419` | 4 Bäume vor den Musikräumen |
| 2 | Der erste Code (KAG-Gleichung) | `8202` | Apfelbilder + Brücken zählbar |
| 3 | Der digitale Lockdown (Story) | — | — |
| 4 | Der Bücher-Check | `FREIHEIT` | Buch + Zettel in der Bibliothek |
| 5 | Das Schatten-Rätsel | `1994` | Zettel am Fenster |
| 6 | Der Spiegel-Code | `60` | Spiegel, Spiegelschrift, 30 Stühle |
| 7 | Worträtsel (Wordle) | `APPLE` | — (läuft in der App) |
| 8 | Zusammensetzen (Master-Code) | `618598592019946011616125` | — |
| 9 | ERDING-Matrix | `1` | — (Raster ist in der App) |
| 10 | Psychologie-Zettel | `MESPACE` | Zettel vor den Psychologieräumen |
| 11 | Raumnummern | `00245` | Kurs-/Raumliste |
| 12 | Geheimschriften (Archiv) | — | — |
| 13 | Der Apfelpfarrer (Archiv) | — | — |
| 14 | Das KAG in Zahlen (Archiv) | — | — |
| 15 | Finale | — | — |

Die Antworteingabe ist tolerant: Groß-/Kleinschreibung, Leerzeichen und Satzzeichen werden ignoriert (`normalizeAnswer()`).

---

## Die Rätsel im Einzelnen

### Stage 0 — ALARM! *(Intro)*
`intro` · Phase Intro · keine Eingabe

Story-Einstieg: Das Schulnetzwerk wurde gehackt, der Computerraum ist verriegelt, drei Schlüssel sind über die Schule verteilt.

---

### Stage 1 — Tutorial *(Training)*
`tutorial` · Phase Tutorial · **`419`** · zeigt die Buchstaben-Tabelle

Führt die zentrale Spielregel ein: **Jeder Buchstabe zählt so viel wie seine Position im Alphabet** (A=1 … Z=26).

- Zahl 1: Anzahl der Bäume vor den Musikräumen → **4**
- Zahl 2: Buchstabenwert von K+A+G = 11+1+7 → **19**
- Beide Zahlen *hintereinanderschreiben* (nicht addieren) → **419**

*Hinweise:* Tabelle nutzen → K=11, A=1, G=7 → Lösung 419

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

Daraus folgt zwingend: **K · A muss 20 ergeben** (denn 8202 − 8122 = 80, und 80 ÷ 4 = 20).

> ⚠️ Der Text weist ausdrücklich darauf hin, dass K, A, G hier **Anzahlen** sind und *nicht* die Buchstabenwerte aus dem Tutorial. Ohne diesen Hinweis rechneten Gruppen 11·1·7+8122 = 8199 — knapp daneben und schwer zu erkennen.

---

### Stage 3 — Der digitale Lockdown *(Story)*
`lockdown_intro` · keine Eingabe

Kündigt den Hauptblock an: **vier Rätsel**, jedes liefert ein Codefragment, danach werden sie zusammengesetzt.

---

### Stage 4 — Der Bücher-Check *(Bibliothek)*
`book_check` · **`FREIHEIT`**

Ein Zettel im ersten Fachbuch nennt drei Zahlen = Seite / Zeile / Wort. Konkret **Seite 42, Zeile 5, Wort 3**. Der Zettel muss physisch vorhanden sein — die App verrät die Koordinaten erst als Hinweis.

---

### Stage 5 — Das Schatten-Rätsel *(Fenster)*
`shadow_riddle` · **`1994`**

Zettel mit schwarzen Balken gegen das Licht halten; die Balken ergeben eine vierstellige Zahl. Sie ist zugleich der Code für das Vorhängeschloss an der Tasche (führt zu Stage 6).

---

### Stage 6 — Der Spiegel-Code *(Spiegel)*
`mirror_code` · **`60`**

In der Tasche: Handspiegel + Blatt in Spiegelschrift. Im Spiegel gelesen steht dort die Aufgabe: **Stühle im Raum zählen × 2**. Bei 30 Stühlen → 60.

---

### Stage 7 — Worträtsel *(Wordle)*
`wordle` · **`APPLE`** (Konstante `WORDLE_TARGET`) · 6 Versuche

Wordle-Minispiel im Terminal. Grün = richtiger Buchstabe an richtiger Stelle, Gelb = richtiger Buchstabe an falscher Stelle, Grau = kommt nicht vor. Doppelbuchstaben (das **PP**) werden korrekt ausgewertet.

> Der Text sagt ausdrücklich, dass das Wort **englisch** ist — sonst tippen Gruppen naheliegend „APFEL".

Nach 6 Fehlversuchen: ein Leben weg, Board wird geleert.

---

### Stage 8 — Zusammensetzen *(Master-Code)*
`compile` · **`618598592019946011616125`** · zeigt die Buchstaben-Tabelle

Die vier Fragmente in Fundreihenfolge aneinanderhängen, Buchstaben dabei in ihre Alphabet-Position umwandeln, ohne führende Nullen:

```
FREIHEIT → 6-18-5-9-8-5-9-20 → 6185985920
1994     →                     1994
60       →                     60
APPLE    → 1-16-16-12-5      → 11616125
                    ────────────────────────────
             618598592019946011616125   (24 Ziffern)
```

Die Längenangabe „24 Ziffern" im Text dient als Selbstkontrolle.

---

### Stage 9 — ERDING-Matrix
`erding_matrix` · **`1`** · zeigt das Raster in der App

Ein Raster aus 7 Zeilen × 6 Spalten, Spaltenköpfe **E-R-D-I-N-G**. Die grün leuchtenden Felder zeichnen aus der Entfernung betrachtet die Ziffer **1**.

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

### Stage 10 — Psychologie-Zettel
`mespace` · **`MESPACE`**

Zettel vor den Psychologieräumen mit einem einzelnen Wort, das wie eine getarnte Systemkennung wirkt.

---

### Stage 11 — Raumnummern
`room_sorting` · **`00245`**

Liste aus Kursen und ihren vollständigen Raumnummern. Raumnummern der Größe nach sortieren, dann alle Ziffern aneinanderhängen — **führende Nullen bleiben erhalten**.

Beispielhafte Liste, die 00245 ergibt: Räume `002`, `4`, `5` → sortiert 002 / 4 / 5 → `00245`.

---

### Stage 12 — Geheimschriften *(Archiv)*
`cipher_training` · keine Eingabe · zeigt die Buchstaben-Tabelle

Nachschlagewerk. Stellt klar: **Für die Rätsel gilt ausschließlich A=1 … Z=26.** Andere Verfahren (Caesar, ASCII, Morse, Telefon-Tastatur) werden erklärt, aber ausdrücklich als *im Spiel nicht verwendet* gekennzeichnet — vorher standen ASCII (A=65) und A=1 gleichrangig nebeneinander.

---

### Stage 13 — Der Apfelpfarrer *(Archiv)*
`aigner_archive` · keine Eingabe

Die Geschichte des Namensgebers Korbinian Aigner (1885 Hohenpolding – 1966 Freising): Verzicht auf das Hoferbe, Pomologe, rund 900 Sortenbilder (1912–1960), Widerstand gegen die Nationalsozialisten, KZ Dachau, die vier Apfelsorten KZ-1 bis KZ-4, der Korbiniansapfel (1985), Flucht 1945, der Häftlingsmantel auf dem Sarg, die Sammlung im Historischen Archiv der TU München und die documenta 13 (2012).

Alle Angaben sind belegt recherchiert.

---

### Stage 14 — Das KAG in Zahlen *(Archiv)*
`school_clues` · keine Eingabe

Recherchierte Fakten zu Schule und Stadt: Eröffnung 2004 als „Gymnasium Erding II", Kreistagsbeschluss zur Umbenennung am 28.06.2010, Namensgebungsfeier Februar 2011, Sigwolfstraße 50 / 85435 Erding, Vorwahl 08122, 1211 Schülerinnen und Schüler und 92 Lehrkräfte (2024/25), drei Ausbildungsrichtungen, 3,3 ha Gelände, Auszeichnungen — dazu Erding: Ersterwähnung 788, Stadtrechte 1228, Schöner Turm von 1408, Flughafen München im Erdinger Moos.

---

### Stage 15 — Finale
`finale` · keine Eingabe

Abschlusstext: Master-Code eingesetzt, Computerraum erreicht, Hackerverbindung getrennt.

> Das Finale ist bewusst noch nicht als eigenes Rätsel ausgebaut — es zeigt nur Text und einen „Weiter"-Knopf.

---

## Spielmechanik

**Hinweis-System.** Jedes lösbare Rätsel hat bis zu drei gestaffelte Hinweise; der letzte nennt die Lösung („Lösung zeigen (spicken)"). Hinweise kosten **kein** Leben — bewusst so, damit bei einer Veranstaltung niemand hängen bleibt.

**Leben.** 3 Leben (`MAX_LIVES`). Bei drei Fehlversuchen erscheint der Fehlschlag-Bildschirm; der **Fortschritt bleibt erhalten** und die Gruppe startet mit vollen Leben beim aktuellen Rätsel neu.

**Phasen.** Steuern Label, Hintergrund und Akzentfarbe:

| Phase | Label | Stages |
| --- | --- | --- |
| Intro | INTRO-SEQUENZ | 0 |
| Tutorial | TRAININGSMODUS | 1 |
| Mission | MISSION | 2–11 |
| Archiv | ARCHIV | 12–14 |
| Finale | FINALE | 15 |

**Entwickler-Werkzeuge.** Nur im Debug-Build: im Hauptmenü 5× auf den Totenkopf tippen. Danach: Rätsel überspringen, Leben auffüllen, Unverwundbarkeit.

*Quelle: `MainActivity.kt` (Liste `gameNodes`) sowie recherchierte Fakten zu Schule, Namensgeber und Stadt.*
