# Escape the KAG

Android-Escape-Room-App fuer das P-Seminar Informatik App Programmierung am Korbinian-Aigner-Gymnasium Erding, Schuljahr 25/26.

## Release-Stand

- App-Name: `Escape the KAG`
- Package: `de.kagerding.savetheapple.android`
- Mindestversion: `minSdk 24`
- Version: `1.0.0`
- UI: Jetpack Compose
- Release-Build: optimiert; optional signierbar ueber lokale `keystore.properties`

## Lite-Variante

Dieser Branch (`lite`) verzichtet auf alle Raetsel, fuer die vorher physische
Materialien in der Schule ausgelegt oder ausgedruckt werden muessen. Entfernt
wurden Buecher-Check, Schatten-Raetsel, Spiegel-Code, Psychologie-Zettel und
Raumnummern.

Die drei Codefragmente aus diesen Raetseln (`FREIHEIT`, `1994`, `60`) liefert
jetzt die App selbst in der Lockdown-Story. Der Master-Code bleibt dadurch
unveraendert, und die Rechenaufgabe bleibt als Raetsel erhalten.

Vorbereitung: Es muss nichts mehr ausgelegt werden. Nur die Zaehlwerte in der
Schule muessen stimmen (Baeume vor den Musikraeumen, K mal A = 20) - siehe
VORBEREITUNG.md.

Der volle Umfang liegt auf `main`.

## Loesungen

| Abschnitt | Eingabe |
| --- | --- |
| Tutorial | `419` |
| KAG-Gleichung | `8202` |
| Wordle | `APPLE` |
| Compile | `618598592019946011616125` |
| ERDING-Matrix | `1` |
| Finale (4 Riegel) | `419`, `8202`, `APPLE`, `6125` |

Von der App vorgegeben (keine Eingabe noetig): `FREIHEIT`, `1994`, `60`.

## Production-APK exportieren

### Variante A: Android Studio

1. `Build > Generate Signed App Bundle / APK...`
2. `APK` auswaehlen.
3. Einen bestehenden Keystore auswaehlen oder einen neuen erstellen.
4. Build Variant `release` waehlen.
5. Fertige APK aus dem von Android Studio angezeigten Ausgabeordner verwenden.

### Variante B: Kommandozeile

1. Einmalig einen Release-Keystore erstellen:

```powershell
keytool -genkeypair -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias savetheapple
```

2. Im Projektwurzelordner eine lokale `keystore.properties` anlegen:

```properties
storeFile=release-keystore.jks
storePassword=DEIN_STORE_PASSWORT
keyAlias=savetheapple
keyPassword=DEIN_KEY_PASSWORT
```

3. Release-APK bauen:

```powershell
.\gradlew.bat assembleRelease
```

4. Die signierte APK liegt danach hier:

```text
app\build\outputs\apk\release\app-release.apk
```

Ohne lokale `keystore.properties` baut Gradle nur eine technische Kontrollversion:

```text
app\build\outputs\apk\release\app-release-unsigned.apk
```

Diese unsigned APK ist nicht fuer die finale Abgabe gedacht.

Vor Abgabe testen:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleRelease
```

`keystore.properties` und Keystore-Dateien sind absichtlich in `.gitignore` eingetragen und duerfen nicht ins Repository committed werden.
