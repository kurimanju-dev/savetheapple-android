package de.kagerding.savetheapple.android

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
        enableEdgeToEdge()
        setTerminalBars(window)
        setContent {
            SaveTheAppleTheme {
                EscapeKagApp()
            }
        }
    }
}

private fun setTerminalBars(window: Window) {
    window.statusBarColor = android.graphics.Color.BLACK
    window.navigationBarColor = android.graphics.Color.BLACK
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

private val Screen.sceneRank: Int
    get() = when (this) {
        Screen.Splash -> 0
        Screen.Menu -> 1
        Screen.Settings -> 2
        Screen.Credits -> 2
        Screen.Game -> 3
        Screen.Failure -> 4
    }

private val Screen.sceneAccent: Color
    get() = when (this) {
        Screen.Splash -> NeonCyan
        Screen.Menu -> NeonGreen
        Screen.Game -> DangerRed
        Screen.Credits -> WarmAmber
        Screen.Settings -> NeonCyan
        Screen.Failure -> DangerRed
    }

private val Screen.sceneCommand: String
    get() = when (this) {
        Screen.Splash -> "BOOT"
        Screen.Menu -> "HUB READY"
        Screen.Game -> "BREACH START"
        Screen.Credits -> "TRACE AUTHORS"
        Screen.Settings -> "CONFIG"
        Screen.Failure -> "LOCKOUT"
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EscapeKagApp() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var screen by remember { mutableStateOf(Screen.Splash) }
    var currentStage by remember { mutableStateOf(readStoredStage(prefs)) }
    var isMuted by remember { mutableStateOf(prefs.getBoolean(PREF_MUTED, false)) }
    var hapticsEnabled by remember { mutableStateOf(prefs.getBoolean(PREF_HAPTICS, true)) }
    var developerMode by remember {
        mutableStateOf(DEV_TOOLS_AVAILABLE && prefs.getBoolean(PREF_DEVELOPER_MODE, false))
    }
    var invincible by remember {
        mutableStateOf(DEV_TOOLS_AVAILABLE && prefs.getBoolean(PREF_INVINCIBLE, false))
    }
    var lives by remember { mutableStateOf(readStoredLives(prefs)) }
    var errorFlash by remember { mutableStateOf(false) }
    var resetSignal by remember { mutableStateOf(0) }

    LaunchedEffect(errorFlash) {
        if (errorFlash) {
            delay(760)
            errorFlash = false
        }
    }

    fun saveStage(stage: Int) {
        val next = stage.coerceIn(0, gameNodes.lastIndex)
        currentStage = next
        prefs.edit().putInt(PREF_STAGE, next).apply()
    }

    fun saveLives(value: Int) {
        lives = value.coerceIn(0, MAX_LIVES)
        prefs.edit().putInt(PREF_LIVES, lives).apply()
    }

    fun performHaptic(type: HapticFeedbackType = HapticFeedbackType.LongPress) {
        if (hapticsEnabled) {
            haptics.performHapticFeedback(type)
        }
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
        screen = Screen.Menu
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
            screen = Screen.Menu
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
            screen = Screen.Failure
        } else {
            saveLives(remaining)
        }
    }

    BackHandler(enabled = screen != Screen.Menu && screen != Screen.Splash && screen != Screen.Failure) {
        screen = Screen.Menu
    }

    CompositionLocalProvider(LocalHapticsEnabled provides hapticsEnabled) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                val movingForward = targetState.sceneRank >= initialState.sceneRank
                val enter = when (targetState) {
                    Screen.Game -> slideInVertically(tween(720, easing = LinearEasing)) { it } +
                        fadeIn(tween(480))
                    Screen.Settings -> slideInHorizontally(tween(520, easing = LinearEasing)) { -it / 2 } +
                        fadeIn(tween(260))
                    Screen.Credits -> slideInHorizontally(tween(520, easing = LinearEasing)) { it / 2 } +
                        fadeIn(tween(260))
                    Screen.Menu -> if (initialState == Screen.Splash) {
                        slideInVertically(tween(620, easing = LinearEasing)) { -it / 4 } +
                            fadeIn(tween(520))
                    } else {
                        slideInHorizontally(tween(420, easing = LinearEasing)) {
                            if (movingForward) it / 4 else -it / 4
                        } + fadeIn(tween(260))
                    }
                    Screen.Failure -> fadeIn(tween(160))
                    Screen.Splash -> fadeIn(tween(240))
                }
                val exit = when (initialState) {
                    Screen.Game -> slideOutVertically(tween(420, easing = LinearEasing)) { it / 3 } +
                        fadeOut(tween(260))
                    Screen.Settings -> slideOutHorizontally(tween(360, easing = LinearEasing)) { -it / 3 } +
                        fadeOut(tween(220))
                    Screen.Credits -> slideOutHorizontally(tween(360, easing = LinearEasing)) { it / 3 } +
                        fadeOut(tween(220))
                    Screen.Splash -> slideOutVertically(tween(520, easing = LinearEasing)) { it / 5 } +
                        fadeOut(tween(360))
                    Screen.Menu -> slideOutHorizontally(tween(460, easing = LinearEasing)) {
                        if (targetState == Screen.Settings) it / 3 else -it / 3
                    } + fadeOut(tween(240))
                    Screen.Failure -> fadeOut(tween(120))
                }
                enter togetherWith exit
            },
            label = "scene-router",
        ) { activeScreen ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (activeScreen) {
                    Screen.Splash -> SplashScreen(
                        errorFlash = errorFlash,
                        onFinished = { screen = Screen.Menu },
                    )
                    Screen.Menu -> MenuScreen(
                        progress = currentStage,
                        lives = lives,
                        errorFlash = errorFlash,
                        onStart = { screen = Screen.Game },
                        onSettings = { screen = Screen.Settings },
                        onCredits = { screen = Screen.Credits },
                        onDeveloperUnlock = if (DEV_TOOLS_AVAILABLE) ::unlockDeveloperMode else null,
                    )
                    Screen.Game -> GameDeckScreen(
                        node = gameNodes[currentStage],
                        stage = currentStage,
                        totalStages = gameNodes.size,
                        lives = lives,
                        developerMode = developerMode,
                        invincible = invincible,
                        errorFlash = errorFlash,
                        resetSignal = resetSignal,
                        onBack = { screen = Screen.Menu },
                        onSolved = ::solveCurrentNode,
                        onWrong = ::wrongAnswer,
                        onRestoreLives = { saveLives(MAX_LIVES) },
                        onToggleInvincible = { enabled ->
                            invincible = enabled
                            prefs.edit().putBoolean(PREF_INVINCIBLE, enabled).apply()
                            if (enabled) {
                                saveLives(MAX_LIVES)
                            }
                        },
                    )
                    Screen.Credits -> CreditsScreen(
                        errorFlash = errorFlash,
                        onBack = { screen = Screen.Menu },
                    )
                    Screen.Settings -> SettingsScreen(
                        isMuted = isMuted,
                        hapticsEnabled = hapticsEnabled,
                        errorFlash = errorFlash,
                        onMutedChange = { muted ->
                            isMuted = muted
                            prefs.edit().putBoolean(PREF_MUTED, muted).apply()
                        },
                        onHapticsChange = { enabled ->
                            hapticsEnabled = enabled
                            prefs.edit().putBoolean(PREF_HAPTICS, enabled).apply()
                        },
                        onResetProgress = ::resetSavedData,
                        onBack = { screen = Screen.Menu },
                    )
                    Screen.Failure -> FailureScreen(
                        errorFlash = true,
                        onCrash = {
                            resetRunState(prefs)
                            throw IllegalStateException("KAG security lockout: all lives depleted")
                        },
                    )
                }
                SceneTransitionOverlay(activeScreen)
            }
        }
    }
}

@Composable
private fun SceneTransitionOverlay(screen: Screen) {
    var armed by remember(screen) { mutableStateOf(false) }
    LaunchedEffect(screen) {
        armed = true
    }
    val progress by animateFloatAsState(
        targetValue = if (armed) 1f else 0f,
        animationSpec = tween(durationMillis = if (screen == Screen.Game) 920 else 680, easing = LinearEasing),
        label = "scene-wipe",
    )
    if (progress >= 0.995f) return

    val accent = screen.sceneAccent
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha((1f - progress * 0.72f).coerceIn(0f, 1f)),
    ) {
        val sweepX = size.width * (progress * 1.45f - 0.24f)
        drawRect(Color.Black.copy(alpha = (0.62f - progress * 0.36f).coerceAtLeast(0f)))
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    accent.copy(alpha = 0.72f),
                    DangerRed.copy(alpha = if (screen == Screen.Game) 0.42f else 0.18f),
                    Color.Transparent,
                ),
                startX = sweepX - 160.dp.toPx(),
                endX = sweepX + 160.dp.toPx(),
            ),
        )
        repeat(9) { index ->
            val y = size.height * (index + 1) / 10f
            val laneStart = (sweepX - index * 42.dp.toPx()).coerceIn(-size.width, size.width)
            drawLine(
                color = accent.copy(alpha = 0.42f * (1f - progress)),
                start = Offset(laneStart, y),
                end = Offset((laneStart + size.width * 0.42f).coerceAtMost(size.width), y),
                strokeWidth = (1.4f + index % 3).dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawContext.canvas.nativeCanvas.drawText(
            screen.sceneCommand,
            24.dp.toPx(),
            size.height - 42.dp.toPx(),
            android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (180 * (1f - progress)).toInt().coerceIn(0, 180),
                    105,
                    255,
                    154,
                )
                textSize = 14.sp.toPx()
                typeface = android.graphics.Typeface.MONOSPACE
                isFakeBoldText = true
            },
        )
    }
}

@Composable
private fun SplashScreen(errorFlash: Boolean, onFinished: () -> Unit) {
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

    TerminalScaffold(background = R.drawable.secondback, errorFlash = errorFlash) {
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
}

@Composable
private fun MenuScreen(
    progress: Int,
    lives: Int,
    errorFlash: Boolean,
    onStart: () -> Unit,
    onSettings: () -> Unit,
    onCredits: () -> Unit,
    onDeveloperUnlock: (() -> Unit)?,
) {
    val startLabel = if (progress == 0) "Spiel starten" else "Fortsetzen"
    val nextNode = gameNodes[progress.coerceIn(0, gameNodes.lastIndex)]
    var devTapCount by remember { mutableStateOf(0) }

    TerminalScaffold(background = R.drawable.kagescape, errorFlash = errorFlash) {
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
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun GameDeckScreen(
    node: GameNode,
    stage: Int,
    totalStages: Int,
    lives: Int,
    developerMode: Boolean,
    invincible: Boolean,
    errorFlash: Boolean,
    resetSignal: Int,
    onBack: () -> Unit,
    onSolved: () -> Unit,
    onWrong: () -> Unit,
    onRestoreLives: () -> Unit,
    onToggleInvincible: (Boolean) -> Unit,
) {
    TerminalScaffold(background = node.phase.background, errorFlash = errorFlash) {
        PhaseAmbientOverlay(node.phase)
        AnimatedContent(
            targetState = node,
            transitionSpec = {
                (slideInHorizontally(tween(520, easing = LinearEasing)) { it / 2 } + fadeIn(tween(320))) togetherWith
                    (slideOutHorizontally(tween(320, easing = LinearEasing)) { -it / 3 } + fadeOut(tween(220)))
            },
            label = "game-node",
        ) { activeNode ->
            PuzzleScreen(
                node = activeNode,
                stage = stage,
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
        PhaseChangeOverlay(node.phase)
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
    errorFlash: Boolean,
    onMutedChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit,
) {
    TerminalScaffold(background = R.drawable.secondback, errorFlash = errorFlash) {
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
}

@Composable
private fun CreditsScreen(errorFlash: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    TerminalScaffold(background = R.drawable.secondback, errorFlash = errorFlash) {
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
}

@Composable
private fun FailureScreen(errorFlash: Boolean, onCrash: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1100)
        onCrash()
    }
    TerminalScaffold(background = R.drawable.secondback, errorFlash = errorFlash) {
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
            color = if (message.startsWith("ACCESS")) DangerRed else TextSecondary,
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
                        guesses = nextGuesses
                        currentGuess = ""
                        message = "ACCESS DENIED // ${lives - 1} Leben verbleibend"
                        onWrong()
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
private fun PhaseChangeOverlay(phase: GamePhase) {
    var armed by remember(phase) { mutableStateOf(false) }
    LaunchedEffect(phase) {
        armed = true
    }
    val progress by animateFloatAsState(
        targetValue = if (armed) 1f else 0f,
        animationSpec = tween(durationMillis = 1100, easing = LinearEasing),
        label = "phase-change",
    )
    if (progress >= 0.995f) return

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha((1f - progress).coerceIn(0f, 1f)),
    ) {
        val accent = phase.accent
        val bandTop = size.height * (0.48f - progress * 0.48f)
        val bandHeight = size.height * (0.04f + progress * 0.96f)
        drawRect(Color.Black.copy(alpha = 0.5f))
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.82f),
                    accent.copy(alpha = 0.28f),
                    Color.Black.copy(alpha = 0.82f),
                ),
            ),
            topLeft = Offset(0f, bandTop),
            size = Size(size.width, bandHeight),
        )
        repeat(6) { index ->
            val y = size.height * (0.28f + index * 0.08f)
            val startX = size.width * (progress - 0.28f) - index * 24.dp.toPx()
            drawLine(
                color = accent.copy(alpha = 0.58f),
                start = Offset(startX.coerceAtLeast(0f), y),
                end = Offset((startX + size.width * 0.58f).coerceAtMost(size.width), y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawContext.canvas.nativeCanvas.drawText(
            phase.label,
            24.dp.toPx(),
            size.height * 0.5f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (220 * (1f - progress * 0.55f)).toInt().coerceIn(0, 220),
                    232,
                    255,
                    244,
                )
                textSize = 24.sp.toPx()
                typeface = android.graphics.Typeface.MONOSPACE
                isFakeBoldText = true
            },
        )
        drawContext.canvas.nativeCanvas.drawText(
            phase.systemLine,
            24.dp.toPx(),
            size.height * 0.5f + 28.dp.toPx(),
            android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (160 * (1f - progress * 0.55f)).toInt().coerceIn(0, 160),
                    0,
                    229,
                    255,
                )
                textSize = 12.sp.toPx()
                typeface = android.graphics.Typeface.MONOSPACE
            },
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
private fun TerminalScaffold(
    background: Int,
    errorFlash: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBlack),
    ) {
        Image(
            painter = painterResource(background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.76f,
        )
        MatrixRain()
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.32f),
                        Color.Black.copy(alpha = 0.08f),
                        Color.Black.copy(alpha = 0.56f),
                    ),
                ),
            )
        }
        content()
        CrtOverlay(errorFlash)
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

    Box(
        modifier = modifier
            .then(widthModifier)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, accent.copy(alpha = 0.62f), RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.11f), RoundedCornerShape(8.dp))
            .clickable {
                if (hapticsEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onClick()
            }
            .padding(horizontal = if (compact) 0.dp else 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = accent,
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
