import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.baselineprofile)
}

val releaseKeystorePropertiesFile = rootProject.file("keystore.properties")
val releaseKeystoreProperties = Properties().apply {
    if (releaseKeystorePropertiesFile.exists()) {
        releaseKeystorePropertiesFile.inputStream().use(::load)
    }
}
val hasReleaseKeystore = releaseKeystorePropertiesFile.exists()

android {
    namespace = "de.kagerding.savetheapple.android"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "de.kagerding.savetheapple.android"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(releaseKeystoreProperties["storeFile"] as String)
                storePassword = releaseKeystoreProperties["storePassword"] as String
                keyAlias = releaseKeystoreProperties["keyAlias"] as String
                keyPassword = releaseKeystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// BEKANNTE EINSCHRAENKUNG (Stand AGP 9.2.1 / baselineprofile 1.5.0-alpha07):
//
// Die Sammel-Variante "nonMinifiedRelease" erbt den optimization-Block von
// "release" und laeuft daher durch R8. Das erzeugte Profil enthaelt dadurch die
// obfuskierten Namen genau dieses R8-Laufs. Im echten Release-Build vergibt R8
// andere Namen, weshalb beim Bauen "Startup class not found: a01" erscheint und
// nur ein kleiner Teil der Regeln greift.
//
// Erfolglos versucht: optimization{enable=false} per
// buildTypes.matching{...}.configureEach fuer nonMinifiedRelease (AGP erfasst
// die Einstellung schon bei der Variantenerzeugung) sowie die unten gesetzte
// Option baselineProfileRulesRewrite, die die Regeln eigentlich ueber die
// Mapping-Datei zurueckschreiben soll.
//
// Das Profil schadet nicht (rund 6 KB im APK), bringt aktuell aber kaum etwas.
// Die Option bleibt gesetzt, damit es mit einer stabilen Plugin-Version ohne
// weitere Aenderung funktioniert.
baselineProfile {
    baselineProfileRulesRewrite = true
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Spielt das Baseline Profile beim ersten Start auf dem Geraet ein.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
