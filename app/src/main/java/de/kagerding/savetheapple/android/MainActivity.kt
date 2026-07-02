package de.kagerding.savetheapple.android

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Window
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kagerding.savetheapple.android.ui.theme.SaveTheAppleTheme
import kotlinx.coroutines.delay

private const val SCHOOL_WEBSITE = "https://www.kag-erding.de/"
private const val GITHUB_URL = "https://github.com/kurimanju-dev/savetheapple-android"
private const val PREFS_NAME = "escape_kag_state"
private const val PREF_STAGE = "progress_stage"
private const val PREF_MUTED = "muted"
private const val PREF_HAPTICS = "haptics"
private const val PREF_LIVES = "lives"
private const val PREF_DEVELOPER_MODE = "developer_mode"
private const val PREF_INVINCIBLE = "invincible"
private const val MAX_LIVES = 3
private const val DEV_UNLOCK_TAPS = 5
private const val WORDLE_NODE_ID = "wordle"
private const val WORDLE_TARGET = "APPLE"
private const val WORDLE_MAX_ATTEMPTS = 6
private val DEV_TOOLS_AVAILABLE = BuildConfig.DEBUG

private val LocalHapticsEnabled = staticCompositionLocalOf { true }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        setTerminalBars(window)
        setContent {
            SaveTheAppleTheme {
                EscapeKagApp()
            }
        }
    }
}

private fun setTerminalBars(window: Window) {
    val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
    insetsController.isAppearanceLightStatusBars = false
    insetsController.isAppearanceLightNavigationBars = false
}

private fun resetRunState(prefs: SharedPreferences) {
    prefs.edit()
        .putInt(PREF_STAGE, 0)
        .putInt(PREF_LIVES, MAX_LIVES)
        .apply()
}

private fun storedLivesAreDepleted(prefs: SharedPreferences): Boolean {
    return when (val rawValue = prefs.all[PREF_LIVES]) {
        is Int -> rawValue <= 0
        is String -> (rawValue.toIntOrNull() ?: MAX_LIVES) <= 0
        is Number -> rawValue.toInt() <= 0
        else -> false
    }
}

private fun readStoredStage(prefs: SharedPreferences): Int {
    if (storedLivesAreDepleted(prefs)) {
        resetRunState(prefs)
        return 0
    }

    val rawValue = prefs.all[PREF_STAGE]
    val migratedStage = when (rawValue) {
        is Int -> rawValue
        is String -> when (rawValue) {
            "fresh", "intro" -> 0
            "tutorial" -> 1
            "chapter_one" -> 2
            "complete" -> gameNodes.lastIndex
            else -> rawValue.toIntOrNull() ?: 0
        }
        is Number -> rawValue.toInt()
        else -> 0
    }.coerceIn(0, gameNodes.lastIndex)

    if (rawValue !is Int || rawValue != migratedStage) {
        prefs.edit().putInt(PREF_STAGE, migratedStage).apply()
    }

    return migratedStage
}

private fun readStoredLives(prefs: SharedPreferences): Int {
    val rawValue = prefs.all[PREF_LIVES]
    val migratedLives = when (rawValue) {
        is Int -> rawValue
        is String -> rawValue.toIntOrNull() ?: MAX_LIVES
        is Number -> rawValue.toInt()
        else -> MAX_LIVES
    }.coerceIn(0, MAX_LIVES)

    if (migratedLives <= 0) {
        resetRunState(prefs)
        return MAX_LIVES
    }

    if (rawValue !is Int || rawValue != migratedLives) {
        prefs.edit().putInt(PREF_LIVES, migratedLives).apply()
    }

    return migratedLives
}

private enum class Screen {
    Splash,
    Menu,
    GameStartIntro,
    Game,
    Credits,
    Settings,
    Failure,
}

private enum class GamePhase {
    Intro,
    Tutorial,
    Mission,
    Archive,
    Finale,
}

private data class GameNode(
    val id: String,
    val title: String,
    val overline: String,
    val sections: List<String>,
    val phase: GamePhase = GamePhase.Mission,
    val acceptedAnswers: Set<String> = emptySet(),
    val supportFields: List<String> = emptyList(),
    val fullText: String = sections.joinToString("\n\n"),
)

private enum class WordleHint {
    Empty,
    Miss,
    Present,
    Correct,
}

private enum class GameStartIntroPhase {
    Blackout,
    Skull,
    Glitch,
}

private val GamePhase.label: String
    get() = when (this) {
        GamePhase.Intro -> "INTRO-SEQUENZ"
        GamePhase.Tutorial -> "TRAININGSMODUS"
        GamePhase.Mission -> "MISSION"
        GamePhase.Archive -> "ARCHIV"
        GamePhase.Finale -> "FINALE"
    }

private val GamePhase.systemLine: String
    get() = when (this) {
        GamePhase.Intro -> "story uplink // keine eingabe erforderlich"
        GamePhase.Tutorial -> "sandbox aktiv // loesung ueben"
        GamePhase.Mission -> "live node // schloss gesichert"
        GamePhase.Archive -> "optional cache // schulspuren"
        GamePhase.Finale -> "shutdown sequence // letzter zugriff"
    }

private val GamePhase.inputLine: String
    get() = when (this) {
        GamePhase.Intro -> "intro abgeschlossen // weiter"
        GamePhase.Tutorial -> "training gate unlocked"
        GamePhase.Mission -> "input gate unlocked"
        GamePhase.Archive -> "archive node ready"
        GamePhase.Finale -> "final shutdown ready"
    }

private val GamePhase.accent: Color
    get() = when (this) {
        GamePhase.Intro -> NeonCyan
        GamePhase.Tutorial -> WarmAmber
        GamePhase.Mission -> NeonGreen
        GamePhase.Archive -> Color(0xFFB48CFF)
        GamePhase.Finale -> DangerRed
    }

private val GamePhase.background: Int
    get() = when (this) {
        GamePhase.Intro -> R.drawable.secondback
        GamePhase.Tutorial -> R.drawable.kagescape
        GamePhase.Mission -> R.drawable.secondback
        GamePhase.Archive -> R.drawable.kagescape
        GamePhase.Finale -> R.drawable.secondback
    }

@Stable
private class EscapeKagAppState(
    private val prefs: SharedPreferences,
    private val haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    var screen by mutableStateOf(Screen.Splash)
        private set
    var currentStage by mutableStateOf(readStoredStage(prefs))
        private set
    var isMuted by mutableStateOf(prefs.getBoolean(PREF_MUTED, false))
        private set
    var hapticsEnabled by mutableStateOf(prefs.getBoolean(PREF_HAPTICS, true))
        private set
    var developerMode by mutableStateOf(DEV_TOOLS_AVAILABLE && prefs.getBoolean(PREF_DEVELOPER_MODE, false))
        private set
    var invincible by mutableStateOf(DEV_TOOLS_AVAILABLE && prefs.getBoolean(PREF_INVINCIBLE, false))
        private set
    var lives by mutableStateOf(readStoredLives(prefs))
        private set
    var errorFlash by mutableStateOf(false)
        private set
    var resetSignal by mutableStateOf(0)
        private set

    val canNavigateBack: Boolean
        get() = screen != Screen.Menu &&
            screen != Screen.Splash &&
            screen != Screen.GameStartIntro &&
            screen != Screen.Failure

    val background: Int
        get() = screen.backgroundForStage(currentStage)

    val showAmbientMotion: Boolean
        get() = screen == Screen.Splash || screen == Screen.Game

    val showErrorOverlay: Boolean
        get() = errorFlash || screen == Screen.Failure

    fun navigateTo(destination: Screen) {
        screen = destination
    }

    fun startGameFromMenu() {
        screen = if (currentStage == 0) Screen.GameStartIntro else Screen.Game
    }

    fun finishGameStartIntro() {
        screen = Screen.Game
    }

    fun clearErrorFlash() {
        errorFlash = false
    }

    fun updateMuted(muted: Boolean) {
        isMuted = muted
        prefs.edit().putBoolean(PREF_MUTED, muted).apply()
    }

    fun updateHapticsEnabled(enabled: Boolean) {
        hapticsEnabled = enabled
        prefs.edit().putBoolean(PREF_HAPTICS, enabled).apply()
    }

    fun resetSavedData() {
        currentStage = 0
        lives = MAX_LIVES
        isMuted = false
        hapticsEnabled = true
        developerMode = false
        invincible = false
        prefs.edit()
            .clear()
            .putInt(PREF_STAGE, 0)
            .putInt(PREF_LIVES, MAX_LIVES)
            .putBoolean(PREF_MUTED, false)
            .putBoolean(PREF_HAPTICS, true)
            .putBoolean(PREF_DEVELOPER_MODE, false)
            .putBoolean(PREF_INVINCIBLE, false)
            .apply()
        resetSignal += 1
        navigateTo(Screen.Menu)
    }

    fun unlockDeveloperMode() {
        if (DEV_TOOLS_AVAILABLE && !developerMode) {
            developerMode = true
            prefs.edit().putBoolean(PREF_DEVELOPER_MODE, true).apply()
            performHaptic()
        }
    }

    fun solveCurrentNode() {
        performHaptic()
        saveLives(MAX_LIVES)
        if (currentStage < gameNodes.lastIndex) {
            saveStage(currentStage + 1)
            resetSignal += 1
        } else {
            navigateTo(Screen.Menu)
        }
    }

    fun wrongAnswer() {
        performHaptic()
        errorFlash = true
        if (invincible) {
            saveLives(MAX_LIVES)
            return
        }

        val remaining = lives - 1
        if (remaining <= 0) {
            currentStage = 0
            lives = MAX_LIVES
            resetRunState(prefs)
            resetSignal += 1
            navigateTo(Screen.Failure)
        } else {
            saveLives(remaining)
        }
    }

    fun restoreLives() {
        saveLives(MAX_LIVES)
    }

    fun updateInvincible(enabled: Boolean) {
        invincible = enabled
        prefs.edit().putBoolean(PREF_INVINCIBLE, enabled).apply()
        if (enabled) {
            saveLives(MAX_LIVES)
        }
    }

    fun crashAfterFailure(): Nothing {
        resetRunState(prefs)
        throw IllegalStateException("KAG security lockout: all lives depleted")
    }

    private fun saveStage(stage: Int) {
        val next = stage.coerceIn(0, gameNodes.lastIndex)
        currentStage = next
        prefs.edit().putInt(PREF_STAGE, next).apply()
    }

    private fun saveLives(value: Int) {
        val next = value.coerceIn(0, MAX_LIVES)
        lives = next
        prefs.edit().putInt(PREF_LIVES, next).apply()
    }

    private fun performHaptic(type: HapticFeedbackType = HapticFeedbackType.LongPress) {
        if (hapticsEnabled) {
            haptics.performHapticFeedback(type)
        }
    }
}

@Composable
private fun rememberEscapeKagAppState(): EscapeKagAppState {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    return remember(prefs, haptics) {
        EscapeKagAppState(prefs = prefs, haptics = haptics)
    }
}

@Composable
private fun EscapeKagApp() {
    val appState = rememberEscapeKagAppState()

    LaunchedEffect(appState.errorFlash) {
        if (appState.errorFlash) {
            delay(760)
            appState.clearErrorFlash()
        }
    }

    BackHandler(enabled = appState.canNavigateBack) {
        appState.navigateTo(Screen.Menu)
    }

    CompositionLocalProvider(LocalHapticsEnabled provides appState.hapticsEnabled) {
        EscapeAppScaffold(
            background = appState.background,
            showAmbientMotion = appState.showAmbientMotion,
            errorFlash = appState.showErrorOverlay,
        ) {
            AppRouteContent(appState)
        }
    }
}

private fun Screen.backgroundForStage(stage: Int): Int {
    return when (this) {
        Screen.Splash,
        Screen.GameStartIntro,
        Screen.Settings,
        Screen.Credits,
        Screen.Failure -> R.drawable.secondback
        Screen.Menu -> R.drawable.kagescape
        Screen.Game -> gameNodes[stage.coerceIn(0, gameNodes.lastIndex)].phase.background
    }
}

@Composable
private fun EscapeAppScaffold(
    background: Int,
    showAmbientMotion: Boolean,
    errorFlash: Boolean,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = TerminalBlack,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TerminalBlack),
        ) {
            AppBackground(background)
            if (showAmbientMotion) {
                MatrixRain()
            }
            AppBackdropScrim()
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0.dp),
            ) { _ ->
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
            CrtOverlay(errorFlash)
        }
    }
}

@Composable
private fun AppRouteContent(appState: EscapeKagAppState) {
    AnimatedContent(
        targetState = appState.screen,
        transitionSpec = {
            if (targetState == Screen.GameStartIntro ||
                (initialState == Screen.GameStartIntro && targetState == Screen.Game)
            ) {
                return@AnimatedContent fadeIn(
                    animationSpec = tween(durationMillis = 130, easing = LinearEasing),
                ) togetherWith fadeOut(
                    animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                )
            }

            val direction = if (targetState.routeOrder >= initialState.routeOrder) 1 else -1
            val enter = slideInHorizontally(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            ) { width -> width / 10 * direction } + fadeIn(
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            )
            val exit = slideOutHorizontally(
                animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing),
            ) { width -> -width / 12 * direction } + fadeOut(
                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
            )
            enter togetherWith exit
        },
        label = "app-route",
    ) { screen ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                Screen.Splash -> SplashScreen(
                    onFinished = { appState.navigateTo(Screen.Menu) },
                )
                Screen.Menu -> MenuScreen(
                    progress = appState.currentStage,
                    lives = appState.lives,
                    onStart = appState::startGameFromMenu,
                    onSettings = { appState.navigateTo(Screen.Settings) },
                    onCredits = { appState.navigateTo(Screen.Credits) },
                    onDeveloperUnlock = if (DEV_TOOLS_AVAILABLE) appState::unlockDeveloperMode else null,
                )
                Screen.GameStartIntro -> GameStartIntroScreen(
                    onFinished = appState::finishGameStartIntro,
                )
                Screen.Game -> GameDeckScreen(
                    node = gameNodes[appState.currentStage],
                    stage = appState.currentStage,
                    totalStages = gameNodes.size,
                    lives = appState.lives,
                    developerMode = appState.developerMode,
                    invincible = appState.invincible,
                    resetSignal = appState.resetSignal,
                    onBack = { appState.navigateTo(Screen.Menu) },
                    onSolved = appState::solveCurrentNode,
                    onWrong = appState::wrongAnswer,
                    onRestoreLives = appState::restoreLives,
                    onToggleInvincible = appState::updateInvincible,
                )
                Screen.Credits -> CreditsScreen(
                    onBack = { appState.navigateTo(Screen.Menu) },
                )
                Screen.Settings -> SettingsScreen(
                    isMuted = appState.isMuted,
                    hapticsEnabled = appState.hapticsEnabled,
                    onMutedChange = appState::updateMuted,
                    onHapticsChange = appState::updateHapticsEnabled,
                    onResetProgress = appState::resetSavedData,
                    onBack = { appState.navigateTo(Screen.Menu) },
                )
                Screen.Failure -> FailureScreen(
                    onCrash = appState::crashAfterFailure,
                )
            }
            RouteGlitchReveal(screen)
        }
    }
}

private val Screen.routeOrder: Int
    get() = when (this) {
        Screen.Splash -> 0
        Screen.Menu -> 1
        Screen.Settings -> 2
        Screen.Credits -> 2
        Screen.GameStartIntro -> 3
        Screen.Game -> 4
        Screen.Failure -> 5
    }

@Composable
private fun AppBackground(background: Int) {
    Crossfade(
        targetState = background,
        animationSpec = tween(durationMillis = 240),
        label = "app-background",
    ) { backgroundRes ->
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.76f,
        )
    }
}

@Composable
private fun AppBackdropScrim() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.Black.copy(alpha = 0.34f),
                    Color.Black.copy(alpha = 0.12f),
                    Color.Black.copy(alpha = 0.58f),
                ),
            ),
        )
    }
}

@Composable
private fun RouteGlitchReveal(screen: Screen) {
    if (screen == Screen.Splash || screen == Screen.GameStartIntro) return

    var visible by remember(screen) { mutableStateOf(true) }
    LaunchedEffect(screen) {
        visible = true
        delay(if (screen == Screen.Game) 420 else 260)
        visible = false
    }

    if (visible) {
        HackerGlitchOverlay(
            modifier = Modifier.fillMaxSize(),
            intensity = if (screen == Screen.Game) 0.9f else 0.52f,
        )
    }
}

@Composable
private fun HackerGlitchOverlay(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    darkPulse: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "hacker-glitch-overlay")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 110, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glitch-phase",
    )
    val drift by transition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 170, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glitch-drift",
    )

    Canvas(modifier = modifier) {
        if (darkPulse) {
            drawRect(Color.Black.copy(alpha = (0.18f + phase * 0.18f) * intensity))
        }

        val lineColor = NeonGreen.copy(alpha = 0.24f * intensity)
        var scanY = 0f
        val scanStep = 7.dp.toPx()
        while (scanY < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, scanY),
                end = Offset(size.width, scanY),
                strokeWidth = 1.dp.toPx(),
            )
            scanY += scanStep
        }

        repeat(15) { index ->
            val seed = index * 0.137f
            val y = size.height * ((seed + phase * (0.18f + index * 0.009f)) % 1f)
            val height = (2 + index % 5).dp.toPx()
            val start = size.width * (((index * 0.211f) + phase * 0.37f) % 1f) - size.width * 0.24f
            val width = size.width * (0.16f + (index % 4) * 0.08f)
            val color = when (index % 3) {
                0 -> NeonCyan
                1 -> DangerRed
                else -> NeonGreen
            }
            drawRect(
                color = color.copy(alpha = (0.16f + (index % 4) * 0.035f) * intensity),
                topLeft = Offset(start + drift * (index % 3 - 1), y),
                size = Size(width, height),
            )
        }

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    NeonCyan.copy(alpha = 0.16f * intensity),
                    DangerRed.copy(alpha = 0.12f * intensity),
                    Color.Transparent,
                ),
                startX = size.width * phase - 80.dp.toPx(),
                endX = size.width * phase + 120.dp.toPx(),
            ),
        )
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2800)
        onFinished()
    }

    val transition = rememberInfiniteTransition(label = "splash")
    val pulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val jitter by transition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 170, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "jitter",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.kag_logo),
            contentDescription = "KAG Logo",
            modifier = Modifier
                .size(230.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                    translationX = jitter
                }
                .clip(RoundedCornerShape(2.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.65f), RoundedCornerShape(2.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(34.dp))
        GlitchText(
            text = "ESCAPE THE KAG",
            style = TerminalTextStyle.copy(
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(Modifier.height(10.dp))
        StreamingStatus("boot sequence // P-Seminar Informatik 25/26")
    }
}

@Composable
private fun GameStartIntroScreen(onFinished: () -> Unit) {
    var phase by remember { mutableStateOf(GameStartIntroPhase.Blackout) }

    LaunchedEffect(Unit) {
        delay(360)
        phase = GameStartIntroPhase.Skull
        delay(920)
        phase = GameStartIntroPhase.Glitch
        delay(760)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBlack),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = phase,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "new-game-intro-phase",
        ) { activePhase ->
            when (activePhase) {
                GameStartIntroPhase.Blackout -> Box(Modifier.fillMaxSize())
                GameStartIntroPhase.Skull,
                GameStartIntroPhase.Glitch -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    SkullMascot(mascotSize = if (activePhase == GameStartIntroPhase.Glitch) 168.dp else 142.dp)
                    Spacer(Modifier.height(22.dp))
                    if (activePhase == GameStartIntroPhase.Glitch) {
                        GlitchText(
                            text = "SIGNALSTÖRUNG",
                            style = TerminalTextStyle.copy(
                                color = NeonGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                            ),
                        )
                        Spacer(Modifier.height(8.dp))
                        StreamingStatus("decrypting first node // unstable link")
                    }
                }
            }
        }

        if (phase == GameStartIntroPhase.Glitch) {
            HackerGlitchOverlay(
                modifier = Modifier.fillMaxSize(),
                intensity = 1f,
                darkPulse = true,
            )
        }
    }
}

@Composable
private fun MenuScreen(
    progress: Int,
    lives: Int,
    onStart: () -> Unit,
    onSettings: () -> Unit,
    onCredits: () -> Unit,
    onDeveloperUnlock: (() -> Unit)?,
) {
    val startLabel = if (progress == 0) "Spiel starten" else "Fortsetzen"
    val nextNode = gameNodes[progress.coerceIn(0, gameNodes.lastIndex)]
    var devTapCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LivesIndicator(lives)
            Box(
                modifier = Modifier.clickable {
                    val unlock = onDeveloperUnlock ?: return@clickable
                    devTapCount += 1
                    if (devTapCount >= DEV_UNLOCK_TAPS) {
                        devTapCount = 0
                        unlock()
                    }
                },
            ) {
                SkullMascot(mascotSize = 70.dp)
            }
        }
        Spacer(Modifier.height(6.dp))
        GlitchText(
            text = "Escape the KAG",
            style = TerminalTextStyle.copy(
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            ),
        )
        Text(
            text = "Korbinian-Aigner-Gymnasium Erding",
            color = WarmAmber,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        StreamingStatus("${nextNode.phase.label} // node ${progress + 1}/${gameNodes.size}")
        Spacer(Modifier.height(18.dp))
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HackerButton(
                text = startLabel,
                accent = NeonGreen,
                large = true,
                onClick = onStart,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HackerButton(
                    text = "Optionen",
                    accent = NeonCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onSettings,
                )
                HackerButton(
                    text = "Credits",
                    accent = WarmAmber,
                    modifier = Modifier.weight(1f),
                    onClick = onCredits,
                )
            }
        }
        Spacer(Modifier.height(34.dp))
    }
}

@Composable
private fun GameDeckScreen(
    node: GameNode,
    stage: Int,
    totalStages: Int,
    lives: Int,
    developerMode: Boolean,
    invincible: Boolean,
    resetSignal: Int,
    onBack: () -> Unit,
    onSolved: () -> Unit,
    onWrong: () -> Unit,
    onRestoreLives: () -> Unit,
    onToggleInvincible: (Boolean) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        PhaseAmbientOverlay(node.phase)
        Crossfade(
            targetState = stage.coerceIn(0, gameNodes.lastIndex),
            animationSpec = tween(durationMillis = 180),
            label = "game-node",
        ) { activeStage ->
            val activeNode = gameNodes[activeStage]
            PuzzleScreen(
                node = activeNode,
                stage = activeStage,
                totalStages = totalStages,
                lives = lives,
                developerMode = developerMode,
                invincible = invincible,
                resetSignal = resetSignal,
                onBack = onBack,
                onSolved = onSolved,
                onWrong = onWrong,
                onRestoreLives = onRestoreLives,
                onToggleInvincible = onToggleInvincible,
            )
        }
    }
}

@Composable
private fun PuzzleScreen(
    node: GameNode,
    stage: Int,
    totalStages: Int,
    lives: Int,
    developerMode: Boolean,
    invincible: Boolean,
    resetSignal: Int,
    onBack: () -> Unit,
    onSolved: () -> Unit,
    onWrong: () -> Unit,
    onRestoreLives: () -> Unit,
    onToggleInvincible: (Boolean) -> Unit,
) {
    var sectionIndex by remember(node.id, resetSignal) { mutableStateOf(0) }
    var visibleCount by remember(node.id, sectionIndex, resetSignal) { mutableStateOf(0) }
    var inputMode by remember(node.id, resetSignal) { mutableStateOf(node.sections.isEmpty()) }
    var answer by remember(node.id, resetSignal) { mutableStateOf("") }
    var helperValues by remember(node.id, resetSignal) {
        mutableStateOf(List(node.supportFields.size) { "" })
    }
    var showFullText by remember(node.id, resetSignal) { mutableStateOf(false) }
    var feedback by remember(node.id, resetSignal) { mutableStateOf<String?>(null) }

    val currentText = node.sections.getOrNull(sectionIndex).orEmpty()
    val phase = node.phase

    LaunchedEffect(node.id, sectionIndex, inputMode, resetSignal) {
        visibleCount = 0
        if (!inputMode) {
            while (visibleCount < currentText.length) {
                delay(if (currentText.length > 180) 10 else 16)
                visibleCount += 1
            }
        }
    }

    fun advanceText() {
        if (inputMode) return
        if (visibleCount < currentText.length) {
            visibleCount = currentText.length
            return
        }
        if (sectionIndex < node.sections.lastIndex) {
            sectionIndex += 1
        } else {
            inputMode = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .padding(18.dp),
    ) {
        HeaderBar(
            overline = "${phase.label} // ${stage + 1}/$totalStages",
            lives = lives,
            onBack = onBack,
        )
        Spacer(Modifier.height(10.dp))
        PhaseBanner(
            phase = phase,
            node = node,
            sectionIndex = sectionIndex,
            totalSections = node.sections.size,
            inputMode = inputMode,
        )
        Spacer(Modifier.height(12.dp))
        GlitchText(
            text = node.title,
            style = TerminalTextStyle.copy(
                fontSize = 27.sp,
                fontWeight = FontWeight.Black,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(enabled = !inputMode) { advanceText() },
            contentAlignment = Alignment.Center,
        ) {
            if (!inputMode) {
                PhaseTextStage(phase = phase) {
                    Column(
                        modifier = Modifier.widthIn(max = 520.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        SkullMascot(mascotSize = if (phase == GamePhase.Intro) 92.dp else 74.dp)
                        Spacer(Modifier.height(20.dp))
                        TypewriterBlock(
                            text = currentText.take(visibleCount),
                            cursorVisible = visibleCount >= currentText.length,
                            accent = phase.accent,
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = if (visibleCount < currentText.length) "tippen: sofort dekodieren" else "tippen: weiter",
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StreamingStatus(phase.inputLine)
                    Spacer(Modifier.height(14.dp))
                    if (node.id == WORDLE_NODE_ID) {
                        WordleGate(
                            lives = lives,
                            onSolved = onSolved,
                            onWrong = onWrong,
                        )
                    } else {
                        if (node.supportFields.isNotEmpty()) {
                            SupportInputs(
                                labels = node.supportFields,
                                values = helperValues,
                                onValuesChange = { helperValues = it },
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        if (node.acceptedAnswers.isNotEmpty()) {
                            CodeInput(
                                value = answer,
                                onValueChange = { answer = it },
                                label = "Code / Antwort",
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        HackerButton(
                            text = if (node.acceptedAnswers.isEmpty()) "Weiter" else "Code prüfen",
                            accent = NeonGreen,
                            onClick = {
                                if (node.acceptedAnswers.isEmpty()) {
                                    onSolved()
                                } else if (node.matches(answer)) {
                                    feedback = null
                                    onSolved()
                                } else {
                                    feedback = "ACCESS DENIED // ${lives - 1} Leben verbleibend"
                                    onWrong()
                                }
                            },
                        )
                        feedback?.let {
                            Spacer(Modifier.height(10.dp))
                            GlitchText(
                                text = it,
                                style = TerminalTextStyle.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    HackerButton(
                        text = if (showFullText) "Text schließen" else "Gesamten Text öffnen",
                        accent = WarmAmber,
                        onClick = { showFullText = !showFullText },
                    )
                    if (showFullText) {
                        Spacer(Modifier.height(14.dp))
                        TerminalPanel {
                            Text(
                                text = node.fullText,
                                style = TerminalTextStyle.copy(fontSize = 14.sp, lineHeight = 21.sp),
                                color = TextPrimary,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        ProgressRail(stage = stage, totalStages = totalStages)
        if (developerMode) {
            Spacer(Modifier.height(8.dp))
            DeveloperPanel(
                invincible = invincible,
                onSkip = onSolved,
                onRestoreLives = onRestoreLives,
                onToggleInvincible = onToggleInvincible,
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    isMuted: Boolean,
    hapticsEnabled: Boolean,
    onMutedChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp),
    ) {
        HeaderBar(overline = "Optionen", lives = MAX_LIVES, onBack = onBack)
        Spacer(Modifier.height(18.dp))
        GlitchText(
            text = "Einstellungen",
            style = TerminalTextStyle.copy(fontSize = 29.sp, fontWeight = FontWeight.Black),
        )
        Spacer(Modifier.height(22.dp))
        SettingsToggle(
            title = "Ton",
            subtitle = if (isMuted) "Ton Aus" else "Ton An",
            checked = !isMuted,
            onCheckedChange = { checked -> onMutedChange(!checked) },
        )
        Spacer(Modifier.height(14.dp))
        SettingsToggle(
            title = "Haptik",
            subtitle = if (hapticsEnabled) "Vibration An" else "Vibration Aus",
            checked = hapticsEnabled,
            onCheckedChange = onHapticsChange,
        )
        Spacer(Modifier.height(26.dp))
        HackerButton(
            text = "Gespeicherte Daten zurücksetzen",
            accent = DangerRed,
            onClick = onResetProgress,
        )
        Spacer(Modifier.weight(1f))
        StreamingStatus("settings saved locally // persistent state armed")
    }
}

@Composable
private fun CreditsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeaderBar(overline = "Credits", lives = MAX_LIVES, onBack = onBack)
        Spacer(Modifier.height(18.dp))
        GlitchText(
            text = "Credits",
            style = TerminalTextStyle.copy(fontSize = 30.sp, fontWeight = FontWeight.Black),
        )
        Spacer(Modifier.height(20.dp))
        TerminalPanel {
            Text(
                text = "Korbinian-Aigner-Gymnasium Erding\n\n" +
                    "Projektteam: P-Seminar Informatik App Programmierung\n" +
                    "Schuljahr: 25/26\n\n" +
                    "P-Seminar Informatik App Programmierung Schuljahr 25/26",
                style = TerminalTextStyle.copy(fontSize = 15.sp, lineHeight = 23.sp),
                color = TextPrimary,
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTerminalButton(
                label = "Schule",
                accent = NeonCyan,
                onClick = { openUrl(context, SCHOOL_WEBSITE) },
            ) {
                GlobeIcon(Modifier.size(30.dp), NeonCyan)
            }
            Spacer(Modifier.width(24.dp))
            IconTerminalButton(
                label = "GitHub",
                accent = WarmAmber,
                onClick = { openUrl(context, GITHUB_URL) },
            ) {
                GithubIcon(Modifier.size(30.dp), WarmAmber)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FailureScreen(onCrash: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1100)
        onCrash()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SkullMascot(mascotSize = 130.dp, danger = true)
        Spacer(Modifier.height(24.dp))
        GlitchText(
            text = "FATAL ERROR",
            style = TerminalTextStyle.copy(
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "3/3 Leben verloren // System wird terminiert",
            color = DangerRed,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

private fun GameNode.matches(answer: String): Boolean {
    val normalized = answer.normalizeAnswer()
    return acceptedAnswers.any { it.normalizeAnswer() == normalized }
}

private fun String.normalizeAnswer(): String {
    return filter { it.isLetterOrDigit() }.uppercase()
}

@Composable
private fun WordleGate(
    lives: Int,
    onSolved: () -> Unit,
    onWrong: () -> Unit,
) {
    var guesses by remember { mutableStateOf(emptyList<String>()) }
    var currentGuess by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("5 Buchstaben. Gleiche Farbe wie Wordle: grün = richtig, gelb = falsche Position.") }
    val messageColor = when {
        message.startsWith("ACCESS GRANTED") -> NeonGreen
        message.startsWith("WORDLE LOCKOUT") -> DangerRed
        else -> TextSecondary
    }

    Column(
        modifier = Modifier.widthIn(max = 430.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WordleBoard(
            guesses = guesses,
            currentGuess = currentGuess,
            target = WORDLE_TARGET,
        )
        WordleLegend()
        Text(
            text = message,
            color = messageColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
        CodeInput(
            value = currentGuess,
            onValueChange = { input ->
                currentGuess = input
                    .filter { it.isLetter() }
                    .uppercase()
                    .take(WORDLE_TARGET.length)
            },
            label = "Guess",
        )
        HackerButton(
            text = "Guess prüfen",
            accent = NeonGreen,
            onClick = {
                val guess = currentGuess.filter { it.isLetter() }.uppercase()
                when {
                    guess.length != WORDLE_TARGET.length -> {
                        message = "INPUT ERROR // genau ${WORDLE_TARGET.length} Buchstaben eingeben"
                    }
                    guess in guesses -> {
                        message = "CACHE HIT // dieses Wort wurde schon geprüft"
                    }
                    guess == WORDLE_TARGET -> {
                        guesses = guesses + guess
                        message = "ACCESS GRANTED // WORDLE FRAGMENT: $WORDLE_TARGET"
                        onSolved()
                    }
                    else -> {
                        val nextGuesses = guesses + guess
                        currentGuess = ""
                        if (nextGuesses.size >= WORDLE_MAX_ATTEMPTS) {
                            guesses = emptyList()
                            message = "WORDLE LOCKOUT // alle Versuche verbraucht // ${lives - 1} Leben verbleibend"
                            onWrong()
                        } else {
                            guesses = nextGuesses
                            val remainingAttempts = WORDLE_MAX_ATTEMPTS - nextGuesses.size
                            message = "WORDLE MISS // $remainingAttempts Versuche verbleibend"
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun WordleBoard(
    guesses: List<String>,
    currentGuess: String,
    target: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(WORDLE_MAX_ATTEMPTS) { rowIndex ->
            val submitted = rowIndex < guesses.size
            val rowText = when {
                submitted -> guesses[rowIndex]
                rowIndex == guesses.size -> currentGuess
                else -> ""
            }
            val hints = if (submitted) {
                evaluateWordleGuess(rowText, target)
            } else {
                List(target.length) { WordleHint.Empty }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                repeat(target.length) { column ->
                    WordleTile(
                        letter = rowText.getOrNull(column)?.toString().orEmpty(),
                        hint = hints[column],
                        active = rowIndex == guesses.size,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WordleTile(
    letter: String,
    hint: WordleHint,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "wordle-tile")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 980, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wordle-pulse",
    )
    val fill = when (hint) {
        WordleHint.Correct -> NeonGreen.copy(alpha = 0.72f)
        WordleHint.Present -> WarmAmber.copy(alpha = 0.78f)
        WordleHint.Miss -> TextSecondary.copy(alpha = 0.22f)
        WordleHint.Empty -> PanelBlack
    }
    val border = when (hint) {
        WordleHint.Correct -> NeonGreen
        WordleHint.Present -> WarmAmber
        WordleHint.Miss -> TextSecondary.copy(alpha = 0.55f)
        WordleHint.Empty -> if (active) NeonCyan.copy(alpha = pulse) else NeonCyan.copy(alpha = 0.28f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(7.dp))
            .background(fill, RoundedCornerShape(7.dp))
            .border(1.dp, border, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 23.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WordleLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendChip("richtig", NeonGreen)
        Spacer(Modifier.width(8.dp))
        LegendChip("falsche Position", WarmAmber)
        Spacer(Modifier.width(8.dp))
        LegendChip("nicht drin", TextSecondary)
    }
}

@Composable
private fun LegendChip(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color.copy(alpha = 0.8f), RoundedCornerShape(2.dp)),
        )
        Text(
            text = label,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
}

private fun evaluateWordleGuess(guess: String, target: String): List<WordleHint> {
    val result = MutableList(target.length) { WordleHint.Miss }
    val remaining = mutableMapOf<Char, Int>()

    target.forEachIndexed { index, targetChar ->
        if (guess.getOrNull(index) == targetChar) {
            result[index] = WordleHint.Correct
        } else {
            remaining[targetChar] = (remaining[targetChar] ?: 0) + 1
        }
    }

    guess.forEachIndexed { index, guessChar ->
        if (result[index] != WordleHint.Correct) {
            val available = remaining[guessChar] ?: 0
            if (available > 0) {
                result[index] = WordleHint.Present
                remaining[guessChar] = available - 1
            }
        }
    }

    return result
}

@Composable
private fun PhaseAmbientOverlay(phase: GamePhase) {
    val transition = rememberInfiniteTransition(label = "phase-ambient")
    val sweep by transition.animateFloat(
        initialValue = -0.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase-sweep",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(phase.accent.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.18f),
                radius = size.maxDimension * 0.62f,
            ),
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(phase.accent.copy(alpha = 0.18f), Color.Transparent),
                startX = 0f,
                endX = size.width * 0.42f,
            ),
        )
        val y = size.height * sweep
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, phase.accent.copy(alpha = 0.18f), Color.Transparent),
                startY = y - 30.dp.toPx(),
                endY = y + 30.dp.toPx(),
            ),
        )
    }
}

@Composable
private fun PhaseBanner(
    phase: GamePhase,
    node: GameNode,
    sectionIndex: Int,
    totalSections: Int,
    inputMode: Boolean,
) {
    val transition = rememberInfiniteTransition(label = "phase-banner")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 980, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "phase-glow",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, phase.accent.copy(alpha = glow), RoundedCornerShape(8.dp))
            .background(phase.accent.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = phase.label,
                color = phase.accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                maxLines = 1,
            )
            Text(
                text = if (inputMode) "INPUT" else "STREAM ${sectionIndex + 1}/${totalSections.coerceAtLeast(1)}",
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                maxLines = 1,
                textAlign = TextAlign.End,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${node.overline} // ${phase.systemLine}",
            color = TextPrimary.copy(alpha = 0.9f),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun PhaseTextStage(
    phase: GamePhase,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "phase-text-stage")
    val scan by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase-text-scan",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.Center),
        ) {
            val accent = phase.accent
            val corner = 36.dp.toPx()
            val stroke = 2.dp.toPx()
            val left = 8.dp.toPx()
            val right = size.width - 8.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = size.height - 8.dp.toPx()

            drawLine(accent.copy(alpha = 0.72f), Offset(left, top), Offset(left + corner, top), stroke)
            drawLine(accent.copy(alpha = 0.72f), Offset(left, top), Offset(left, top + corner), stroke)
            drawLine(accent.copy(alpha = 0.72f), Offset(right, top), Offset(right - corner, top), stroke)
            drawLine(accent.copy(alpha = 0.72f), Offset(right, top), Offset(right, top + corner), stroke)
            drawLine(accent.copy(alpha = 0.72f), Offset(left, bottom), Offset(left + corner, bottom), stroke)
            drawLine(accent.copy(alpha = 0.72f), Offset(left, bottom), Offset(left, bottom - corner), stroke)
            drawLine(accent.copy(alpha = 0.72f), Offset(right, bottom), Offset(right - corner, bottom), stroke)
            drawLine(accent.copy(alpha = 0.72f), Offset(right, bottom), Offset(right, bottom - corner), stroke)

            val scanX = size.width * ((scan + 1f) / 2f)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, accent.copy(alpha = 0.16f), Color.Transparent),
                    startX = scanX - 60.dp.toPx(),
                    endX = scanX + 60.dp.toPx(),
                ),
            )
        }
        content()
    }
}

@Composable
private fun HeaderBar(
    overline: String,
    lives: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HackerButton(
            text = "<",
            accent = NeonCyan,
            compact = true,
            onClick = onBack,
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = overline,
                color = WarmAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
            )
            LivesIndicator(lives)
        }
    }
}

@Composable
private fun LivesIndicator(lives: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(MAX_LIVES) { index ->
            Text(
                text = if (index < lives) "♥" else "x",
                color = if (index < lives) DangerRed else TextSecondary.copy(alpha = 0.55f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun ProgressRail(stage: Int, totalStages: Int) {
    val fraction = (stage + 1).toFloat() / totalStages.toFloat()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        drawLine(
            color = NeonCyan.copy(alpha = 0.25f),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 2.dp.toPx(),
        )
        drawLine(
            color = NeonGreen.copy(alpha = 0.85f),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width * fraction, size.height / 2f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    TerminalPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TerminalTextStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = NeonCyan,
                )
                Text(
                    text = subtitle,
                    style = TerminalTextStyle.copy(fontSize = 14.sp),
                    color = TextSecondary,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonGreen,
                    checkedTrackColor = NeonGreen.copy(alpha = 0.35f),
                    uncheckedThumbColor = DangerRed,
                    uncheckedTrackColor = DangerRed.copy(alpha = 0.25f),
                ),
            )
        }
    }
}

@Composable
private fun DeveloperPanel(
    invincible: Boolean,
    onSkip: () -> Unit,
    onRestoreLives: () -> Unit,
    onToggleInvincible: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DangerRed.copy(alpha = 0.62f), RoundedCornerShape(8.dp))
            .background(PanelBlack, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "DEV CHANNEL // unlocked",
            color = DangerRed,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HackerButton(
                text = "Skip",
                accent = WarmAmber,
                modifier = Modifier.weight(1f),
                onClick = onSkip,
            )
            HackerButton(
                text = "Lives",
                accent = NeonGreen,
                modifier = Modifier.weight(1f),
                onClick = onRestoreLives,
            )
            HackerButton(
                text = if (invincible) "God:ON" else "God:OFF",
                accent = if (invincible) NeonGreen else DangerRed,
                modifier = Modifier.weight(1f),
                onClick = { onToggleInvincible(!invincible) },
            )
        }
    }
}

@Composable
private fun StreamingStatus(text: String) {
    val transition = rememberInfiniteTransition(label = "status")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 820, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "status-alpha",
    )
    Text(
        text = "> $text",
        color = NeonGreen.copy(alpha = alpha),
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun TypewriterBlock(
    text: String,
    cursorVisible: Boolean,
    accent: Color = NeonGreen,
) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursor-alpha",
    )
    val cursor = if (cursorVisible) " ${(if (cursorAlpha > 0.5f) "█" else " ")}" else "█"
    GlitchText(
        text = text + cursor,
        style = TerminalTextStyle.copy(
            color = accent,
            fontSize = 20.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SupportInputs(
    labels: List<String>,
    values: List<String>,
    onValuesChange: (List<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.chunked(2).forEachIndexed { chunkIndex, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chunk.forEachIndexed { itemIndex, label ->
                    val globalIndex = chunkIndex * 2 + itemIndex
                    CodeInput(
                        value = values.getOrElse(globalIndex) { "" },
                        onValueChange = { value ->
                            onValuesChange(values.toMutableList().also { it[globalIndex] = value })
                        },
                        label = label,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (chunk.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MatrixRain() {
    val transition = rememberInfiniteTransition(label = "matrix")
    val offset by transition.animateFloat(
        initialValue = -120f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "matrix-offset",
    )
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.42f),
    ) {
        val columnWidth = 34.dp.toPx()
        val rowHeight = 18.dp.toPx()
        val columns = (size.width / columnWidth).toInt() + 2
        val rows = (size.height / rowHeight).toInt() + 8
        repeat(columns) { column ->
            repeat(rows) { row ->
                val y = (row * rowHeight + offset + column * 13f) % (size.height + 160f) - 80f
                val bit = if ((row + column) % 2 == 0) "1" else "0"
                drawContext.canvas.nativeCanvas.drawText(
                    bit,
                    column * columnWidth,
                    y,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(70, 105, 255, 154)
                        textSize = 12.sp.toPx()
                        typeface = android.graphics.Typeface.MONOSPACE
                    },
                )
            }
        }
    }
}

@Composable
private fun TerminalPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonCyan.copy(alpha = 0.48f), RoundedCornerShape(8.dp))
            .background(PanelBlack, RoundedCornerShape(8.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun HackerButton(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val height = when {
        compact -> 42.dp
        large -> 68.dp
        else -> 54.dp
    }
    val widthModifier = if (compact) Modifier.width(52.dp) else Modifier.fillMaxWidth()

    Button(
        onClick = {
            if (hapticsEnabled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        },
        modifier = modifier
            .then(widthModifier)
            .height(height),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent.copy(alpha = 0.11f),
            contentColor = accent,
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.62f)),
        contentPadding = PaddingValues(horizontal = if (compact) 0.dp else 14.dp),
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (large) 21.sp else 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(32)) },
        modifier = modifier.fillMaxWidth(),
        label = {
            Text(
                text = label,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
            )
        },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            color = NeonGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonGreen,
            unfocusedBorderColor = NeonCyan.copy(alpha = 0.55f),
            cursorColor = NeonGreen,
            focusedLabelColor = NeonGreen,
            unfocusedLabelColor = TextSecondary,
            focusedContainerColor = PanelBlack,
            unfocusedContainerColor = PanelBlack,
        ),
    )
}

@Composable
private fun GlitchText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "glitch")
    val shift by transition.animateFloat(
        initialValue = -2.6f,
        targetValue = 2.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shift",
    )
    val redAlpha by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 710, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "red-alpha",
    )

    Box(modifier = modifier) {
        Text(
            text = text,
            style = style,
            color = DangerRed.copy(alpha = redAlpha),
            modifier = Modifier.graphicsLayer { translationX = -shift },
        )
        Text(
            text = text,
            style = style,
            color = NeonCyan.copy(alpha = 0.48f),
            modifier = Modifier.graphicsLayer { translationX = shift },
        )
        Text(text = text, style = style, color = TextPrimary)
    }
}

@Composable
private fun SkullMascot(
    mascotSize: androidx.compose.ui.unit.Dp,
    danger: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "skull")
    val bob by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skull-bob",
    )
    val eyeAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (danger) 220 else 720, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skull-eyes",
    )
    Canvas(
        modifier = Modifier
            .size(mascotSize)
            .graphicsLayer { translationY = bob },
    ) {
        val accent = if (danger) DangerRed else NeonGreen
        val stroke = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        val skullRect = Rect(
            left = size.width * 0.18f,
            top = size.height * 0.1f,
            right = size.width * 0.82f,
            bottom = size.height * 0.72f,
        )
        drawOval(
            color = accent.copy(alpha = 0.8f),
            topLeft = skullRect.topLeft,
            size = skullRect.size,
            style = stroke,
        )
        drawRoundRect(
            color = accent.copy(alpha = 0.72f),
            topLeft = Offset(size.width * 0.32f, size.height * 0.58f),
            size = Size(size.width * 0.36f, size.height * 0.22f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = stroke,
        )
        drawCircle(
            color = DangerRed.copy(alpha = eyeAlpha),
            radius = size.width * 0.07f,
            center = Offset(size.width * 0.39f, size.height * 0.39f),
        )
        drawCircle(
            color = DangerRed.copy(alpha = eyeAlpha),
            radius = size.width * 0.07f,
            center = Offset(size.width * 0.61f, size.height * 0.39f),
        )
        drawLine(accent, Offset(size.width * 0.5f, size.height * 0.48f), Offset(size.width * 0.45f, size.height * 0.56f), strokeWidth = 2.dp.toPx())
        repeat(3) { index ->
            val x = size.width * (0.42f + index * 0.08f)
            drawLine(accent.copy(alpha = 0.8f), Offset(x, size.height * 0.64f), Offset(x, size.height * 0.76f), strokeWidth = 1.6.dp.toPx())
        }
        drawLine(accent.copy(alpha = 0.7f), Offset(size.width * 0.16f, size.height * 0.88f), Offset(size.width * 0.84f, size.height * 0.78f), strokeWidth = 2.dp.toPx())
        drawLine(accent.copy(alpha = 0.7f), Offset(size.width * 0.16f, size.height * 0.78f), Offset(size.width * 0.84f, size.height * 0.88f), strokeWidth = 2.dp.toPx())
    }
}

@Composable
private fun IconTerminalButton(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    Column(
        modifier = Modifier
            .width(108.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, accent.copy(alpha = 0.62f), RoundedCornerShape(8.dp))
            .background(PanelBlack, RoundedCornerShape(8.dp))
            .clickable {
                if (hapticsEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onClick()
            }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = accent,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun GlobeIcon(modifier: Modifier, color: Color) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(color = color, style = stroke)
        drawLine(color, Offset(size.width * 0.16f, size.height * 0.5f), Offset(size.width * 0.84f, size.height * 0.5f), strokeWidth = 2.dp.toPx())
        drawLine(color, Offset(size.width * 0.5f, size.height * 0.1f), Offset(size.width * 0.5f, size.height * 0.9f), strokeWidth = 2.dp.toPx())
        drawOval(
            color = color,
            topLeft = Offset(size.width * 0.28f, size.height * 0.08f),
            size = Size(size.width * 0.44f, size.height * 0.84f),
            style = stroke,
        )
        drawLine(color, Offset(size.width * 0.23f, size.height * 0.29f), Offset(size.width * 0.77f, size.height * 0.29f), strokeWidth = 1.5.dp.toPx())
        drawLine(color, Offset(size.width * 0.23f, size.height * 0.71f), Offset(size.width * 0.77f, size.height * 0.71f), strokeWidth = 1.5.dp.toPx())
    }
}

@Composable
private fun GithubIcon(modifier: Modifier, color: Color) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val head = Rect(
            left = size.width * 0.2f,
            top = size.height * 0.18f,
            right = size.width * 0.8f,
            bottom = size.height * 0.74f,
        )
        val path = Path().apply {
            moveTo(size.width * 0.34f, size.height * 0.24f)
            lineTo(size.width * 0.27f, size.height * 0.11f)
            lineTo(size.width * 0.42f, size.height * 0.2f)
            moveTo(size.width * 0.66f, size.height * 0.24f)
            lineTo(size.width * 0.73f, size.height * 0.11f)
            lineTo(size.width * 0.58f, size.height * 0.2f)
        }
        drawOval(color = color, topLeft = head.topLeft, size = head.size, style = stroke)
        drawPath(path, color = color, style = stroke)
        drawCircle(color = color, radius = 1.8.dp.toPx(), center = Offset(size.width * 0.4f, size.height * 0.46f))
        drawCircle(color = color, radius = 1.8.dp.toPx(), center = Offset(size.width * 0.6f, size.height * 0.46f))
        drawLine(color, Offset(size.width * 0.5f, size.height * 0.72f), Offset(size.width * 0.5f, size.height * 0.9f), strokeWidth = 2.dp.toPx())
        drawLine(color, Offset(size.width * 0.5f, size.height * 0.83f), Offset(size.width * 0.33f, size.height * 0.9f), strokeWidth = 2.dp.toPx())
        drawLine(color, Offset(size.width * 0.5f, size.height * 0.83f), Offset(size.width * 0.67f, size.height * 0.9f), strokeWidth = 2.dp.toPx())
    }
}

@Composable
private fun CrtOverlay(errorFlash: Boolean) {
    val transition = rememberInfiniteTransition(label = "crt")
    val flicker by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 90, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "flicker",
    )
    val scanY by transition.animateFloat(
        initialValue = -0.1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scan-y",
    )
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.94f),
    ) {
        var y = 0f
        val scanlineStep = 4.dp.toPx()
        while (y < size.height) {
            drawLine(
                color = Color.Black.copy(alpha = 0.34f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
            y += scanlineStep
        }
        val movingY = size.height * scanY
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, NeonCyan.copy(alpha = 0.12f), Color.Transparent),
                startY = movingY - 22.dp.toPx(),
                endY = movingY + 22.dp.toPx(),
            ),
        )
        drawRect(Color.White.copy(alpha = flicker * 0.08f))
        if (errorFlash) {
            drawRect(DangerRed.copy(alpha = 0.24f + flicker * 0.32f))
        }
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension * 0.72f,
            ),
        )
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        if (context is Activity) {
            context.runOnUiThread { }
        }
    }
}

private val TerminalBlack = Color(0xFF020404)
private val PanelBlack = Color(0xD9081111)
private val NeonGreen = Color(0xFF69FF9A)
private val NeonCyan = Color(0xFF00E5FF)
private val WarmAmber = Color(0xFFFFD166)
private val DangerRed = Color(0xFFFF4D6D)
private val TextPrimary = Color(0xFFE8FFF4)
private val TextSecondary = Color(0xFF93C9CA)

private val TerminalTextStyle = TextStyle(
    color = TextPrimary,
    fontFamily = FontFamily.Monospace,
    letterSpacing = 0.sp,
)

private val gameNodes = listOf(
    GameNode(
        id = "intro",
        title = "ALARM!",
        overline = "Intro // Systemmeldung",
        phase = GamePhase.Intro,
        sections = listOf(
            "ALARM! Das Netzwerk des KAG wurde gehackt!",
            "Ein unbekannter Hacker hat sich Zugriff auf die Systeme der Schule verschafft - und die Zeit läuft. Es gibt nur einen Weg, ihn aufzuhalten: den Computerraum erreichen und die Verbindung kappen, bevor es zu spät ist.",
            "Doch der Zugang ist nicht so einfach. Eine massive Tür versperrt den Weg, gesichert durch ein Schloss mit drei Schlüsseln.",
            "Diese Schlüssel wurden über die gesamte Schule verteilt und hinter kniffligen Rätseln versteckt - wer sie finden will, muss Mut, Köpfchen und Teamgeist beweisen.",
            "Die Schule wird zu deinem Spielfeld. Jeder Gang, jedes Klassenzimmer könnte einen Hinweis bergen.",
            "Doch Vorsicht: Der Hacker beobachtet jeden deiner Schritte und wird alles tun, um dich aufzuhalten.",
            "Die Uhr tickt. Das Schicksal des KAG liegt in deinen Händen. Bist du bereit, das Abenteuer zu bestehen?",
        ),
    ),
    GameNode(
        id = "tutorial",
        title = "Tutorial",
        overline = "Schlüssel 00 // Training",
        phase = GamePhase.Tutorial,
        sections = listOf(
            "Tipp: Buchstabenwert von Ja=10+1",
            "Die gesuchte Zahl ergibt sich durch das Hintereinander schreiben der herausgefundenen Zahlenwerte.",
            "Zahlenwert eins ergibt sich aus der Anzahl der Bäume, die sich vor den Musikräumen befinden.",
            "Der zweite Zahlenwert ergibt sich aus dem Buchstabenwert von KAG.",
        ),
        acceptedAnswers = setOf("419"),
    ),
    GameNode(
        id = "kag_formula",
        title = "Der erste Code",
        overline = "Kapitel 1 // KAG-Gleichung",
        sections = listOf(
            "Formel für den Code: (K*A*G)+X",
            "Der erste Code ist gut verschlüsselt!",
            "Hinweis I - Die Früchte des Namensgebers: Unser Namensgeber Korbinian Aigner, auch Apfelpfarrer genannt, war ein begeisterter Pomologe.",
            "Zeit seines Lebens malte er rund 1000 originalgetreu Sortenbilder. Vor dem Büro der Schulleitung sind einige dieser Bilder abgebildet. Die Anzahl dieser Bilder ist K.",
            "Hinweis II - Wege zum Wissen: Hoch über den Fluren verbinden sie zwei Seiten. Jede von ihnen trägt ihre eigene Farbe.",
            "Wie viele unterschiedliche Brücken könnt ihr entdecken? Diese Zahl ist A.",
            "Hinweis III - Der Anfang der Geschichte: Jede Schule hat ein Gründungsjahr. Findet heraus, wann unsere Geschichte begann.",
            "Ihr braucht nicht das ganze Jahr - nur die letzte Ziffer. Diese Zahl ist G.",
            "Hinweis IV - Der Anruf nach Erding: Würdet ihr die Schule anrufen, welche Zahlen würdet ihr vor der eigentlichen Telefonnummer wählen? Diese Zahl ist X.",
            "Setze nun die Zahlenwerte in die folgende Formel ein: (K*A*G)+X.",
        ),
        acceptedAnswers = setOf("8202"),
        supportFields = listOf("K", "A", "G", "X"),
        fullText = """
Formel für den Code: (K*A*G)+X

Der erste Code ist gut verschlüsselt!

K = Anzahl der Sortenbilder vor dem Büro der Schulleitung.
A = Anzahl der unterschiedlichen farblichen Brücken/Fachgänge.
G = letzte Ziffer des Gründungsjahres.
X = Zahlen vor der eigentlichen Telefonnummer.

Setze die vier Werte in die Formel ein und öffne damit das erste Schloss.
""".trimIndent(),
    ),
    GameNode(
        id = "lockdown_intro",
        title = "Der digitale Lockdown",
        overline = "Story // Lockdown",
        sections = listOf(
            "Ein mysteriöser Hacker hat das Schulnetzwerk übernommen.",
            "Alle Noten sollen gelöscht werden, und die Türen des Computerraums sind elektronisch verriegelt.",
            "Ihr habt das einzige Tablet, das noch Zugriff auf das System hat.",
            "Der Master-Key wurde in physische Rätsel zerlegt, damit kein Hacker ihn finden kann.",
            "Es gibt 5 Rätsel. Findet jedes Codefragment und setzt am Ende alles zusammen.",
        ),
    ),
    GameNode(
        id = "book_check",
        title = "Der Bücher-Check",
        overline = "Rätsel 1 // Bibliothek",
        sections = listOf(
            "An der Tafel steht: Die Antwort liegt zwischen den Seiten der Wissensträger.",
            "Ein Zettel im ersten Fachbuch im Regal enthält Koordinaten.",
            "Folgt den Koordinaten: Seite 42, Zeile 5, Wort 3.",
            "Gebt das gefundene Wort als Codefragment ein.",
        ),
        acceptedAnswers = setOf("FREIHEIT"),
    ),
    GameNode(
        id = "shadow_riddle",
        title = "Das Schatten-Rätsel",
        overline = "Rätsel 2 // Fenster",
        sections = listOf(
            "Ein Zettel am Fenster zeigt nur wirre schwarze Balken.",
            "Erst wenn man ihn gegen das Licht hält, ergibt sich eine Zahl.",
            "Diese Zahl ist die Kombination für das Vorhängeschloss an der Tasche.",
            "Lest die Schatten korrekt und gebt die Zahl ein.",
        ),
        acceptedAnswers = setOf("1994"),
    ),
    GameNode(
        id = "mirror_code",
        title = "Der Spiegel-Code",
        overline = "Rätsel 3 // Spiegel",
        sections = listOf(
            "In der Tasche liegt ein kleiner Handspiegel und ein Blatt mit Spiegelschrift.",
            "Der Schlüssel ist die Summe der Stühle im Raum multipliziert mit 2.",
            "Nach dem Zählen und Rechnen gibt die Gruppe die Zahl in das Tablet ein.",
            "Gebt das Ergebnis der Rechnung als Codefragment ein.",
        ),
        acceptedAnswers = setOf("60"),
    ),
    GameNode(
        id = "wordle",
        title = "Wordle",
        overline = "Rätsel 4 // Wordle",
        sections = listOf(
            "Das vierte Fragment kommt aus Wordle.",
            "Das Terminal zeigt ein eigenes fünfstelliges Wortfeld.",
            "Jeder Versuch markiert richtige Buchstaben und falsche Positionen.",
            "Hinweis: Es hat 5 Buchstaben und passt zum Projektthema.",
        ),
        fullText = """
Das vierte Fragment kommt aus Wordle.

Das Terminal zeigt ein eigenes fünfstelliges Wortfeld. Das Feld zeigt:
grün = richtiger Buchstabe an richtiger Stelle
gelb = richtiger Buchstabe an falscher Stelle
grau = Buchstabe kommt nicht vor

Hinweis: Es hat 5 Buchstaben und passt zum Projektthema.
""".trimIndent(),
    ),
    GameNode(
        id = "compile",
        title = "Compile",
        overline = "Rätsel 5 // Finale Kompilierung",
        sections = listOf(
            "Alle Lösungen müssen nacheinander gereiht werden.",
            "Der finale Code ist der Entschlüsselungscode.",
            "Zahlen werden zu Nummern: A -> 1.",
            "Reiht die Fragmente in der gefundenen Reihenfolge und wandelt alle Buchstaben in Zahlen um.",
        ),
        acceptedAnswers = setOf("618598592019946011616125"),
    ),
    GameNode(
        id = "erding_matrix",
        title = "ERDING-Matrix",
        overline = "Raster // Matrix",
        sections = listOf(
            "Das Koordinatensystem trägt das Schlüsselwort ERDING.",
            "Die Buchstaben werden über ein Raster aus Zeilen und Spalten entschlüsselt.",
            "Lest das markierte Feld und gebt das Codefragment ein.",
        ),
        acceptedAnswers = setOf("1"),
    ),
    GameNode(
        id = "mespace",
        title = "Psychologie-Zettel",
        overline = "Raumriddle // Psychologie",
        sections = listOf(
            "Auf einem Zettel vor den Psychologieräumen steht ein Wort.",
            "Der Schriftzug wirkt wie eine getarnte Systemkennung.",
            "Gib das Wort exakt als Codefragment ein.",
        ),
        acceptedAnswers = setOf("MESPACE"),
    ),
    GameNode(
        id = "room_sorting",
        title = "Raumnummern",
        overline = "Kursliste // Sortierung",
        sections = listOf(
            "Rätsel mit vollständigen Raumnummern zu jeweiligen Kursen.",
            "Sortiert die Raumnummern der Größe nach: links klein, rechts groß.",
            "Nutzt die vollständige Raumnummer und hängt die Ziffern als Codefragment aneinander.",
            "Achtet darauf, führende Nullen nicht zu verlieren.",
        ),
        acceptedAnswers = setOf("00245"),
    ),
    GameNode(
        id = "cipher_training",
        title = "Chiffrierung",
        overline = "Methoden // Chiffren",
        phase = GamePhase.Archive,
        sections = listOf(
            "ASCII-Code: Die Zahl wird in einen Buchstaben umgewandelt. Beispiel: 65 -> A, 72 -> H.",
            "Caesar-Verschiebung mit Zahl: Die Zahl gibt an, wie weit ein Buchstabe verschoben wird. Beispiel: A mit Ergebnis 8 wird H.",
            "Koordinatensystem: Die Zahl verweist auf Positionen, etwa den 8. Buchstaben eines Satzes oder ein Feld im Raster.",
            "Morsecode: Zahl in Morse übersetzen. In der App kann das mit blinkenden Lichtern oder Terminal-Pulsen erscheinen.",
            "QR-/Matrix-Logik: Die Zahl bestimmt ein Muster. Beispiel: 8 = 8 leuchtende Felder aktivieren.",
            "Primzahlen/Mathe-Logik: Die Zahl muss weiterverarbeitet werden, etwa nächste Primzahl oder Quersumme.",
            "Telefon-Tastatur-Code: Wie alte Handys. 2 = ABC, 3 = DEF, 8 = TUV. Ergebnis 8 -> U.",
        ),
    ),
    GameNode(
        id = "school_clues",
        title = "Schulort-Archiv",
        overline = "Archiv // Schulorte",
        phase = GamePhase.Archive,
        sections = listOf(
            "Raumzahlen als Code: Anzahl von Bäumen, Fachgänge, Apfelbilder, Trinkbrunnen, Umkleiden, Turnhallen, Räume in Chemie, Musikübungsräume und Computerraum.",
            "Weitere Schulwerte: Gründungsdatum als Code 2004, Länge der Tartanbahn, Namensänderungsjahr 2010.",
            "Weitere Schulfragen: Wie viele Fliesen gibt es auf dem Sportplatz? Anzahl der Stühle in einem Raum? Wie viele Punkte auf einer fehlerhaften Tafelhälfte?",
            "Diese Archiv-Station sammelt optionale Schulhinweise für spätere Erweiterungen.",
        ),
    ),
    GameNode(
        id = "finale",
        title = "Finale",
        overline = "Letztes Schloss // Shutdown",
        phase = GamePhase.Finale,
        sections = listOf(
            "In den Codefeldern werden vorherige Rätsel als Schlüssel genutzt.",
            "Das Wordle-Fragment entriegelt die letzte Sperre.",
            "Der Computerraum ist wieder erreichbar.",
            "Die Verbindung des Hackers wird getrennt.",
        ),
    ),
)
