# Escape the KAG

Android-Escape-Room-App fuer das P-Seminar Informatik App Programmierung am Korbinian-Aigner-Gymnasium Erding, Schuljahr 25/26.

## Release-Stand

- App-Name: `Escape the KAG`
- Package: `de.kagerding.savetheapple.android`
- Mindestversion: `minSdk 24`
- Version: `1.0.0`
- UI: Jetpack Compose
- Release-Build: optimiert; optional signierbar ueber lokale `keystore.properties`

## Loesungen

| Abschnitt | Eingabe |
| --- | --- |
| Tutorial | `419` |
| KAG-Gleichung | `8202` |
| Buecher-Check | `FREIHEIT` |
| Schatten-Raetsel | `1994` |
| Spiegel-Code | `60` |
| Wordle | `APPLE` |
| Compile | `618598592019946011616125` |
| ERDING-Matrix | `1` |
| Psychologie-Zettel | `MESPACE` |
| Raumnummern | `00245` |

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
