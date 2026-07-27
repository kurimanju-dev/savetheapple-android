package de.kagerding.savetheapple.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Erzeugt das Baseline Profile fuer "Escape the KAG".
 *
 * Der Ablauf faehrt die Wege ab, die im Spiel wirklich vorkommen: Start bis ins
 * Menue, das Handbuch samt Detailansicht und der Einstieg ins Spiel mit der
 * Textsequenz. Genau diese Klassen und Methoden werden dadurch vorkompiliert.
 *
 * Erzeugen mit:
 *   ./gradlew :baselineprofile:generateBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()

        // Der Splash laeuft von selbst ins Hauptmenue.
        device.wait(Until.hasObject(By.text("Handbuch")), STARTUP_TIMEOUT)
        device.waitForIdle()

        // Handbuch oeffnen, einen Eintrag lesen, scrollen, zurueck.
        device.findObject(By.text("Handbuch"))?.click()
        device.wait(Until.hasObject(By.text("Geheimschriften")), UI_TIMEOUT)
        device.waitForIdle()

        device.findObject(By.text("Geheimschriften"))?.click()
        device.waitForIdle()
        device.wait(Until.hasObject(By.scrollable(true)), UI_TIMEOUT)
        device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
        device.waitForIdle()

        device.pressBack()
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()

        // Spiel betreten und durch die Textsequenz tippen.
        val startButton = device.findObject(By.text("Spiel starten"))
            ?: device.findObject(By.text("Fortsetzen"))
        startButton?.click()
        device.waitForIdle()

        // Die Intro-Animation vor dem ersten Knoten abwarten.
        device.wait(Until.hasObject(By.textContains("STREAM")), UI_TIMEOUT)
        device.waitForIdle()

        repeat(TEXT_TAPS) {
            device.click(device.displayWidth / 2, device.displayHeight / 2)
            device.waitForIdle()
        }
    }

    private companion object {
        const val PACKAGE_NAME = "de.kagerding.savetheapple.android"
        const val STARTUP_TIMEOUT = 15_000L
        const val UI_TIMEOUT = 5_000L
        const val TEXT_TAPS = 10
    }
}
