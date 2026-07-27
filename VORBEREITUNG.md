# Vorbereitung — Escape the KAG (Lite-Variante)

> **Lite.** Auf diesem Branch sind alle Rätsel entfernt, für die Material in der Schule ausgelegt werden muss. Übrig bleiben **zwei** Vorbereitungspunkte statt sieben. Die Vollversion mit allen Rätseln liegt auf `main`.

**Zeitbedarf:** ca. 30 Minuten. Wenn es sehr eilig ist, reichen **Teil 1** und der **Selbsttest in Teil 4**.

---

## ⚠️ Das Wichtigste zuerst

Nur noch zwei Dinge können das Spiel blockieren:

1. **Die Zählwerte in der Schule müssen zu den Lösungen passen** (Teil 2.1) — Bäume und die Bedingung K · A = 20.
2. **Eine ausgedruckte Raumnummern-Liste** (Teil 2.2).

Alles andere läuft komplett im Tablet.

---

## Teil 1 — App auf die Geräte bringen

### 1.1 Welche Version für wen?

| Version | Für wen | Warum |
| --- | --- | --- |
| **Release-APK** | Spielende Gruppen | Kein Entwicklermenü, kein versehentliches Überspringen |
| **Debug-APK** | Betreuende / Aufsicht | Enthält Entwickler-Werkzeuge (Rätsel überspringen, Leben auffüllen) |

### 1.2 Release-APK bauen (signiert)

Ohne `keystore.properties` entsteht nur eine unsignierte APK, die sich **nicht installieren lässt**. Einmalig einen Keystore anlegen:

```bash
keytool -genkeypair -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias savetheapple
```

Dann im Projektwurzelordner eine `keystore.properties` anlegen:

```properties
storeFile=release-keystore.jks
storePassword=DEIN_STORE_PASSWORT
keyAlias=savetheapple
keyPassword=DEIN_KEY_PASSWORT
```

Bauen — **auf dem `lite`-Branch**:

```bash
git checkout lite
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleRelease
```

Ergebnis: `app/build/outputs/apk/release/app-release.apk`

> Kontrolle: Heißt die Datei `app-release-**unsigned**.apk`, wurde die `keystore.properties` nicht gefunden. Diese Datei lässt sich nicht installieren.

### 1.3 Debug-APK für die Aufsicht

```bash
./gradlew assembleDebug
```

Ergebnis: `app/build/outputs/apk/debug/app-debug.apk`

### 1.4 Installieren

Per USB:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Oder: APK auf das Tablet kopieren, antippen, „Installation aus unbekannten Quellen" erlauben.

### 1.5 Checkliste pro Gerät

- [ ] App installiert und startet
- [ ] **Spielstand geleert** — Einstellungen → „Gespeicherte Daten zurücksetzen". *Unbedingt machen:* Die Rätselreihenfolge unterscheidet sich von der Vollversion; alte Spielstände zeigen sonst das falsche Rätsel.
- [ ] Akku voll bzw. Ladekabel dabei
- [ ] Display-Zeitsperre auf mindestens 5 Minuten
- [ ] Lautstärke/Vibration nach Wunsch (in den Einstellungen der App umschaltbar)
- [ ] Flugmodus oder Kiosk-Modus erwägen

---

## Teil 2 — Die zwei verbleibenden Vorbereitungspunkte

### 2.1 Zählwerte in der Schule prüfen

| Rätsel | Was gezählt wird | Muss ergeben | Geprüft |
| --- | --- | --- | --- |
| Tutorial | Bäume vor den Musikräumen | **4** | ☐ |
| KAG-Gleichung | Apfel-Sortenbilder vor dem Büro der Schulleitung (= K) | siehe unten | ☐ |
| KAG-Gleichung | Unterschiedliche farbige Brücken / Fachgänge (= A) | siehe unten | ☐ |

**Die entscheidende Bedingung für die KAG-Gleichung:**

Die Lösung 8202 setzt sich so zusammen:
- G = letzte Ziffer des Gründungsjahres **2004** → **G = 4** ✅ (recherchiert und bestätigt)
- X = Vorwahl Erding **08122** ohne führende Null → **X = 8122** ✅ (recherchiert und bestätigt)
- Daraus folgt: `K · A · G = 8202 − 8122 = 80` und wegen G = 4: **K · A muss genau 20 ergeben**

Mögliche Kombinationen: **5 Bilder × 4 Brücken**, **4 × 5**, **10 × 2** oder **20 × 1**.

- [ ] Vor Ort zählen, welche Kombination tatsächlich zutrifft
- [ ] Falls K · A **nicht** 20 ergibt: Entweder die Anzahl der aufgehängten Bilder anpassen (am schnellsten), oder in `MainActivity.kt` beim Knoten `kag_formula` den Wert in `acceptedAnswers` auf `(K·A·4)+8122` ändern und neu bauen.

**Ebenso bei den Bäumen:** Stehen dort nicht 4 Bäume, passt entweder die Lösung im Code an (Knoten `tutorial`) oder wählt einen anderen Zählgegenstand.

> **Notfall:** Wenn ihr das gar nicht prüfen könnt, ist es kein Beinbruch — beide Rätsel haben ein Hinweis-System, dessen letzte Stufe die Lösung nennt. Die Gruppen kommen also in jedem Fall weiter.

### 2.2 Raumnummern-Liste ausdrucken

Die einzige Station, für die noch etwas vorbereitet werden muss.

Die Lösung ist `00245`. Die Liste muss so gebaut sein, dass die **der Größe nach sortierten Raumnummern hintereinandergeschrieben genau `00245`** ergeben. Zum Beispiel drei Kurse:

```
Kunst      Raum 4
Biologie   Raum 002
Musik      Raum 5
```
→ sortiert: 002, 4, 5 → aneinandergehängt: **00245** ✅

- [ ] Liste mit dieser Logik erstellen und ausdrucken
- [ ] Selbst einmal nachrechnen, dass wirklich 00245 herauskommt
- [ ] Liste auslegen oder aufhängen

### 2.3 Was in der Lite-Variante **entfällt**

Für diese Rätsel muss **nichts** mehr vorbereitet werden — sie sind entfernt:

- ~~Bücher-Check~~ (Buch + Zettel in der Bibliothek)
- ~~Schatten-Rätsel~~ (Zettel am Fenster)
- ~~Spiegel-Code~~ (Tasche, Handspiegel, Spiegelschrift, Vorhängeschloss, 30 Stühle)
- ~~Psychologie-Zettel~~ (Zettel bei den Psychologieräumen)

Ihre Codefragmente `FREIHEIT`, `1994` und `60` gibt die App jetzt selbst in der Lockdown-Story aus. Der Master-Code bleibt dadurch unverändert und die Rechenaufgabe erhalten.

---

## Teil 3 — Ablauf am Abend

### Empfohlene Rahmenbedingungen

- **Gruppengröße:** 3–5 Kinder pro Tablet
- **Spieldauer:** 20–30 Minuten (kürzer als die Vollversion, da weniger Laufwege)
- **Altersempfehlung:** ab 10 Jahren
- **Aufsicht:** Eine Person genügt; sie sollte auf den Hinweis-Knopf hinweisen können

### Reihenfolge im Spiel (9 Stationen)

| # | Station | Ort | Eingabe |
| --- | --- | --- | --- |
| 1 | ALARM! (Story) | — | — |
| 2 | Tutorial | Musikräume | `419` |
| 3 | Der erste Code | Schulleitung / Fachgänge | `8202` |
| 4 | Der digitale Lockdown (Story) | — | — (nennt 3 Fragmente) |
| 5 | Worträtsel | überall | `APPLE` |
| 6 | Zusammensetzen | überall | `618598592019946011616125` |
| 7 | ERDING-Matrix | überall | `1`, nach 3 Mini-Aufgaben im Raster |
| 8 | Raumnummern | bei der Liste | `00245` |
| 9 | Shutdown (Finale) | überall | 5 Riegel, siehe unten |

> Nur die Stationen 2, 3 und 8 sind an einen Ort gebunden. Alles andere können die Gruppen überall lösen — praktisch, wenn es an einer Stelle staut.

### Das Finale

Das Finale fragt fünf Codes ab, die die Gruppe schon geknackt hat — sie brauchen also ihre Notizen:

| Riegel | Antwort |
| --- | --- |
| 1 | `419` |
| 2 | `8202` |
| 3 | `APPLE` |
| 4 | `00245` |
| 5 | `6125` (die letzten vier Ziffern des Master-Codes) |

Danach muss ein roter Schalter **1,6 Sekunden gedrückt gehalten** werden. Nach zwei Fehlversuchen an einem Riegel erscheint automatisch ein Tipp.

### Das Handbuch

Die Archiv-Inhalte (Geheimschriften mit Buchstaben-Tabelle, Der Apfelpfarrer, Das KAG in Zahlen) sind **jederzeit** erreichbar: im Hauptmenü über „Handbuch", im Spiel über den `?`-Knopf oben links. Der Spielstand geht dabei nicht verloren. Weist die Gruppen darauf hin — besonders die Buchstaben-Tabelle hilft beim Master-Code.

### Für die Aufsicht: Notfall-Werkzeuge

**Hinweise (in jeder Version verfügbar):**
Im Eingabemodus unten auf „Hinweis 1 von 3" tippen. Dreimal tippen zeigt die Lösung.

**Entwicklermodus (nur in der Debug-APK):**
Im Hauptmenü **5× auf den Totenkopf tippen**. Danach erscheint unten im Rätsel ein Panel mit *Rätsel überspringen*, *Leben auffüllen* und *Unverwundbar*.

**Fortschritt zurücksetzen:**
Einstellungen → „Gespeicherte Daten zurücksetzen". Vor jeder neuen Gruppe machen.

---

## Teil 4 — Selbsttest vor dem Fest

Einmal komplett durchspielen. Rechnet 10 Minuten ein.

- [ ] App starten, Spielstand zurückgesetzt
- [ ] Tutorial: `419` wird angenommen
- [ ] KAG-Gleichung: `8202` wird angenommen
- [ ] Lockdown-Story nennt die drei Fragmente FREIHEIT, 1994, 60
- [ ] Worträtsel: `APPLE` wird angenommen, Farben stimmen
- [ ] Master-Code: `618598592019946011616125` wird angenommen
- [ ] ERDING-Matrix: alle drei Mini-Aufgaben gelöst, die **1** ist danach gut erkennbar (auch bei Sonnenlicht draußen!)
- [ ] Raumnummern: Liste liegt aus, `00245` wird angenommen
- [ ] Handbuch über den `?`-Knopf mitten im Rätsel geöffnet — Spielstand bleibt erhalten
- [ ] Hinweis-Knopf bei mindestens einem Rätsel ausprobiert
- [ ] Drei falsche Eingaben absichtlich: Fortschritt bleibt erhalten
- [ ] Finale: alle fünf Riegel geöffnet, Schalter gehalten, Abspann erscheint

**Der Master-Code zum Nachrechnen:**

```
FREIHEIT → 6-18-5-9-8-5-9-20 → 6185985920
1994     →                     1994
60       →                     60
APPLE    → 1-16-16-12-5      → 11616125
                    ────────────────────────────
zusammen → 618598592019946011616125   (24 Ziffern)
```

---

## Teil 5 — Bekannte Einschränkungen

- **Der Absturz-Knopf auf dem Fehlschlag-Bildschirm** beendet die App absichtlich („der Hacker schlägt zurück"). Der Spielstand bleibt erhalten — nach dem Neustart geht es beim gleichen Rätsel weiter. Wer das nicht will, weist die Aufsicht an, den Knopf nicht anzutippen.
- **Die Zählwerte K und A sind nicht verifiziert.** Gründungsjahr (2004) und Vorwahl (08122) sind recherchiert und sicher; Bilder und Brücken muss jemand vor Ort nachzählen.

---

## Schnell-Checkliste für den Abend

- [ ] Tablets geladen, App vom **`lite`-Branch** installiert, Spielstände geleert
- [ ] K · A = 20 vor Ort bestätigt
- [ ] 4 Bäume vor den Musikräumen bestätigt
- [ ] Raumnummern-Liste ausgedruckt und ausgelegt
- [ ] Ein kompletter Selbsttest gemacht
- [ ] Aufsicht kennt Hinweis-Knopf und Reset

Viel Erfolg beim Sommerfest!
