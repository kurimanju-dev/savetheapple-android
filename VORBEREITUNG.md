# Vorbereitung — Escape the KAG beim Sommerfest

Diese Anleitung bringt das Spiel heute Abend zum Laufen. Arbeitet sie von oben nach unten ab.

**Zeitbedarf:** ca. 90 Minuten für alles. Wenn die Zeit knapp ist, macht mindestens **Teil 1, Teil 2 und den Selbsttest in Teil 5** — ohne die drei funktioniert das Spiel nicht.

---

## ⚠️ Das Wichtigste zuerst

Zwei Dinge entscheiden darüber, ob die Gruppen überhaupt weiterkommen:

1. **Die Zählwerte in der Schule müssen zu den Lösungen passen** (Teil 2). Die App prüft feste Zahlen. Wenn vor den Musikräumen nicht 4 Bäume stehen, ist das Tutorial unlösbar.
2. **Die physischen Materialien müssen ausliegen** (Teil 2). Vier Rätsel brauchen einen Zettel, ein Buch oder eine Liste. Ohne sie hilft nur noch das Hinweis-System.

Alles andere ist Komfort.

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

Bauen:

```bash
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
- [ ] **Spielstand geleert** — Einstellungen → „Gespeicherte Daten zurücksetzen". *Unbedingt machen:* Die Rätselreihenfolge hat sich geändert, alte Spielstände zeigen sonst das falsche Rätsel.
- [ ] Akku voll bzw. Ladekabel dabei
- [ ] Display-Zeitsperre auf mindestens 5 Minuten (sonst geht der Bildschirm beim Suchen aus)
- [ ] Lautstärke/Vibration nach Wunsch (in den Einstellungen der App umschaltbar)
- [ ] Flugmodus oder Kiosk-Modus erwägen, damit niemand nebenbei surft

---

## Teil 2 — Physische Materialien und Zählwerte

Das ist der kritische Teil. **Jede Zeile muss stimmen, sonst hängt die Gruppe fest.**

### 2.1 Zählwerte in der Schule prüfen

| Rätsel | Was gezählt wird | Muss ergeben | Geprüft |
| --- | --- | --- | --- |
| Tutorial | Bäume vor den Musikräumen | **4** | ☐ |
| KAG-Gleichung | Apfel-Sortenbilder vor dem Büro der Schulleitung (= K) | siehe unten | ☐ |
| KAG-Gleichung | Unterschiedliche farbige Brücken / Fachgänge (= A) | siehe unten | ☐ |
| Spiegel-Code | Stühle im Raum | **30** | ☐ |

**Die entscheidende Bedingung für die KAG-Gleichung:**

Die Lösung 8202 setzt sich so zusammen:
- G = letzte Ziffer des Gründungsjahres **2004** → **G = 4** ✅ (recherchiert und bestätigt)
- X = Vorwahl Erding **08122** ohne führende Null → **X = 8122** ✅ (recherchiert und bestätigt)
- Daraus folgt: `K · A · G = 8202 − 8122 = 80` und wegen G = 4: **K · A muss genau 20 ergeben**

Mögliche Kombinationen: **5 Bilder × 4 Brücken**, **4 × 5**, **10 × 2** oder **20 × 1**.

- [ ] Vor Ort zählen, welche Kombination tatsächlich zutrifft
- [ ] Falls K · A **nicht** 20 ergibt: Entweder die Anzahl der aufgehängten Bilder anpassen (einfachste Lösung), oder in `MainActivity.kt` beim Knoten `kag_formula` den Wert in `acceptedAnswers` auf `(K·A·4)+8122` ändern und neu bauen.

**Ebenso bei Bäumen und Stühlen:** Stimmen die realen Zahlen nicht (4 Bäume, 30 Stühle), passt entweder die Umgebung an (Stühle dazustellen/wegnehmen ist am schnellsten) oder ändert die Lösung im Code.

### 2.2 Material, das ausliegen muss

| Rätsel | Was gebraucht wird | Muss ergeben | Fertig |
| --- | --- | --- | --- |
| **Bücher-Check** | Ein Fachbuch im Regal, darin ein Zettel mit „42 / 5 / 3". Auf **Seite 42, Zeile 5** muss das **3. Wort** `FREIHEIT` sein. | FREIHEIT | ☐ |
| **Schatten-Rätsel** | Zettel am Fenster mit schwarzen Balken, der gegen Licht gehalten **1994** zeigt | 1994 | ☐ |
| **Spiegel-Code** | Tasche mit Handspiegel + Blatt in Spiegelschrift: „Zählt alle Stühle im Raum und multipliziert die Anzahl mit 2." Dazu ein Vorhängeschloss mit Code **1994**. | 60 | ☐ |
| **Psychologie-Zettel** | Zettel vor den Psychologieräumen mit dem Wort `MESPACE` | MESPACE | ☐ |
| **Raumnummern** | Liste mit Kursen und ihren vollständigen Raumnummern | siehe unten | ☐ |

**Zum Bücher-Check:** Findet ihr kein Buch, bei dem Seite 42 / Zeile 5 / Wort 3 zufällig „Freiheit" ergibt, legt einfach einen selbst gedruckten Zettel ins Buch, auf dem an dieser Stelle das Wort steht. Das merkt niemand.

**Zu den Raumnummern — hier fehlt noch Material.** Die Lösung ist `00245`. Die Liste muss so gebaut sein, dass die **der Größe nach sortierten Raumnummern hintereinandergeschrieben genau `00245`** ergeben. Zum Beispiel drei Kurse mit den Räumen `002`, `4` und `5`:

```
Kunst      Raum 4
Biologie   Raum 002
Musik      Raum 5
```
→ sortiert: 002, 4, 5 → aneinandergehängt: **00245** ✅

- [ ] Liste mit dieser Logik erstellen und ausdrucken
- [ ] Selbst einmal nachrechnen, dass wirklich 00245 herauskommt

### 2.3 Kein Material nötig

Diese Rätsel laufen komplett in der App — hier gibt es nichts vorzubereiten:

- **Worträtsel (APPLE)** — läuft im Tablet
- **ERDING-Matrix** — das Raster wird jetzt direkt in der App angezeigt
- **Zusammensetzen (Master-Code)** — reine Rechenaufgabe
- Alle Story- und Archiv-Stationen

---

## Teil 3 — Was sich gegenüber der alten Version geändert hat

Damit ihr nicht überrascht werdet:

- **Hinweis-System:** Jedes Rätsel hat jetzt bis zu 3 Hinweise. Der letzte ist die komplette Lösung („Lösung zeigen (spicken)"). **Hinweise kosten kein Leben** — das ist Absicht, damit beim Fest niemand hängen bleibt.
- **ERDING-Matrix:** Komplett neu. Das Raster steht in der App, die leuchtenden Felder zeichnen die Ziffer **1**. Vorher war das Rätsel ohne Zusatzmaterial gar nicht lösbar.
- **Buchstaben-Tabelle:** Tutorial, Master-Code und Archiv zeigen eine A=1…Z=26-Tabelle direkt über dem Eingabefeld.
- **Kein Totalverlust mehr:** Nach drei Fehlversuchen verliert die Gruppe nicht mehr den ganzen Fortschritt, sondern startet mit vollen Leben beim aktuellen Rätsel neu.
- **Wordle-Falle entschärft:** Es steht jetzt ausdrücklich dabei, dass das Wort **englisch** ist. Vorher tippten viele „APFEL".
- **K, A, G:** Im Tutorial sind es Buchstabenwerte, in der KAG-Gleichung Anzahlen. Darauf wird jetzt ausdrücklich hingewiesen — vorher war das eine echte Stolperfalle.
- **Archiv erweitert:** Neue Station „Der Apfelpfarrer" über Korbinian Aigner sowie „Das KAG in Zahlen" mit recherchierten Fakten zu Schule und Stadt.

---

## Teil 4 — Ablauf am Abend

### Empfohlene Rahmenbedingungen

- **Gruppengröße:** 3–5 Kinder pro Tablet. Bei mehr sehen die hinteren nichts.
- **Spieldauer:** 30–45 Minuten für den kompletten Durchlauf inklusive Laufwege.
- **Altersempfehlung:** ab 10 Jahren; jüngere Kinder brauchen eine begleitende Person.
- **Aufsicht:** Mindestens eine Person, die die Laufwege kennt und bei Bedarf auf das Hinweis-System zeigt.

### Reihenfolge im Spiel (16 Stationen)

| # | Station | Ort | Eingabe |
| --- | --- | --- | --- |
| 1 | ALARM! (Story) | — | — |
| 2 | Tutorial | Musikräume | `419` |
| 3 | Der erste Code | Schulleitung / Fachgänge | `8202` |
| 4 | Der digitale Lockdown (Story) | — | — |
| 5 | Der Bücher-Check | Bibliothek | `FREIHEIT` |
| 6 | Das Schatten-Rätsel | Fenster | `1994` |
| 7 | Der Spiegel-Code | Raum mit Stühlen | `60` |
| 8 | Worträtsel | überall | `APPLE` |
| 9 | Zusammensetzen | überall | `618598592019946011616125` |
| 10 | ERDING-Matrix | überall | `1` |
| 11 | Psychologie-Zettel | Psychologieräume | `MESPACE` |
| 12 | Raumnummern | überall | `00245` |
| 13 | Geheimschriften (Archiv) | — | — |
| 14 | Der Apfelpfarrer (Archiv) | — | — |
| 15 | Das KAG in Zahlen (Archiv) | — | — |
| 16 | Finale | — | — |

> Tipp für die Aufsicht: Die Stationen 8 bis 15 brauchen keinen bestimmten Ort. Wenn es an einem Engpass staut, können Gruppen diese Stationen überall lösen.

### Für die Aufsicht: Notfall-Werkzeuge

**Hinweise (in jeder Version verfügbar):**
Im Eingabemodus unten auf „Hinweis 1 von 3" tippen. Dreimal tippen zeigt die Lösung.

**Entwicklermodus (nur in der Debug-APK):**
Im Hauptmenü **5× auf den Totenkopf tippen**. Danach erscheint unten im Rätsel ein Panel mit:
- *Rätsel überspringen* — wenn ein Material verloren ging
- *Leben auffüllen*
- *Unverwundbar* — für einen Vorführ-Durchlauf

**Fortschritt zurücksetzen:**
Einstellungen → „Gespeicherte Daten zurücksetzen". Vor jeder neuen Gruppe machen.

---

## Teil 5 — Selbsttest vor dem Fest

Unbedingt einmal komplett durchspielen. Rechnet 15 Minuten ein.

- [ ] App starten, Spielstand zurückgesetzt
- [ ] Tutorial: `419` wird angenommen
- [ ] KAG-Gleichung: `8202` wird angenommen
- [ ] Bücher-Check: Zettel im Buch gefunden, `FREIHEIT` wird angenommen
- [ ] Schatten-Rätsel: Zettel gegen Licht lesbar, `1994` wird angenommen
- [ ] Spiegel-Code: Stühle zählbar, `60` wird angenommen
- [ ] Worträtsel: `APPLE` wird angenommen, Farben stimmen
- [ ] Master-Code: `618598592019946011616125` wird angenommen
- [ ] ERDING-Matrix: Die **1** ist auf dem Display gut erkennbar (auch bei Sonnenlicht draußen!)
- [ ] Psychologie-Zettel: Zettel hängt, `MESPACE` wird angenommen
- [ ] Raumnummern: Liste liegt aus, `00245` wird angenommen
- [ ] Hinweis-Knopf bei mindestens einem Rätsel ausprobiert
- [ ] Drei falsche Eingaben absichtlich: Fortschritt bleibt erhalten
- [ ] Kompletter Durchlauf bis zum Finale

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

## Teil 6 — Bekannte Einschränkungen

Ehrlich benannt, damit heute Abend niemand überrascht wird:

- **Das Finale ist noch nicht ausgebaut.** Es zeigt nur einen Abschlusstext und einen „Weiter"-Knopf. Eine echte Schlusseingabe ist bewusst noch nicht implementiert.
- **Nach dem Finale landet man im Hauptmenü**, nicht im Abspann. Der Abspann ist über das Menü erreichbar. Wer mag, ruft ihn nach dem letzten Bildschirm von Hand auf.
- **Der Absturz-Knopf auf dem Fehlschlag-Bildschirm** beendet die App absichtlich („der Hacker schlägt zurück"). Der Spielstand bleibt jetzt aber erhalten — nach dem Neustart geht es beim gleichen Rätsel weiter. Wenn ihr das beim Fest nicht wollt, weist die Aufsicht darauf hin, den Knopf nicht anzutippen.
- **Raumnummern-Material fehlt noch** (siehe 2.2) — das ist der einzige offene Punkt, der zwingend heute noch erledigt werden muss.
- **Die Zählwerte K und A sind nicht verifiziert.** Gründungsjahr (2004) und Vorwahl (08122) sind recherchiert und sicher; die Anzahl der Bilder und Brücken muss jemand vor Ort nachzählen.

---

## Schnell-Checkliste für den Abend

- [ ] Tablets geladen, App installiert, Spielstände geleert
- [ ] K · A = 20 vor Ort bestätigt
- [ ] 4 Bäume vor den Musikräumen, 30 Stühle im Spiegel-Raum
- [ ] Zettel im Buch (Bibliothek)
- [ ] Zettel am Fenster (1994)
- [ ] Tasche mit Spiegel + Spiegelschrift + Schloss
- [ ] Zettel MESPACE bei den Psychologieräumen
- [ ] Raumnummern-Liste ausgedruckt
- [ ] Ein kompletter Selbsttest gemacht
- [ ] Aufsicht kennt Hinweis-Knopf und Reset

Viel Erfolg beim Sommerfest!
