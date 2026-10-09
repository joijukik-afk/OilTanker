package com.oiltanker

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ============================================================================
//  SCREEN ENUM (v2)
// ============================================================================

enum class V2Screen { SPLASH, MENU, STORY, PLAYING, PAUSED, GAMEOVER, SETTINGS, ABOUT }

// ============================================================================
//  MAIN APP COMPOSABLE
// ============================================================================

@Composable
private fun OilTankerAppV2() {
    val context = LocalContext.current
    val save = remember { SaveData(context) }
    val music = remember { MusicEngine() }
    val world = remember { V2GameWorld(music, save) }

    var screen by remember { mutableStateOf(V2Screen.SPLASH) }
    var settingsFromPause by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { music.release() }
    }

    LaunchedEffect(save.musicOn, save.musicVol) {
        music.enabled = save.musicOn
        music.volume = save.musicVol
    }

    // Splash → Menu
    LaunchedEffect(Unit) {
        delay(2200)
        if (screen == V2Screen.SPLASH) {
            screen = V2Screen.MENU
        }
    }

    // Dynamic music per screen
    LaunchedEffect(screen) {
        when (screen) {
            V2Screen.MENU -> music.play(MusicTrack.MENU)
            V2Screen.STORY -> music.play(MusicTrack.STORY)
            V2Screen.GAMEOVER -> {
                if (world.chapter == Chapter.VICTORY) music.play(MusicTrack.VICTORY)
                else music.play(MusicTrack.GAMEOVER)
            }
            else -> {}
        }
    }

    // Universal handler for end of chapter / game over
    val handleEnd: () -> Unit = {
        save.persistScore(world.score)
        save.addKills(world.kills)
        save.addDistance(world.distance.toInt())
        save.persistChapter(
            when (world.chapter) {
                Chapter.CH3, Chapter.VICTORY -> 3
                else -> 1
            }
        )
        screen = V2Screen.GAMEOVER
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when (screen) {

                V2Screen.SPLASH -> V2SplashScreen()

                V2Screen.MENU -> V2MainMenu(
                    save = save,
                    onNewGame = {
                        world.startNewGame()
                        screen = V2Screen.PLAYING
                    },
                    onContinue = {
                        world.startNewGame()
                        if (save.highestChapter >= 3) {
                            world.changeChapter(Chapter.CH3)
                        }
                        screen = V2Screen.PLAYING
                    },
                    onSettings = { screen = V2Screen.SETTINGS },
                    onAbout = { screen = V2Screen.ABOUT },
                    onStory = { screen = V2Screen.STORY }
                )

                V2Screen.STORY -> V2StoryIntroScreen(
                    music = music,
                    onComplete = {
                        save.persistStorySeen()
                        world.startNewGame()
                        screen = V2Screen.PLAYING
                    }
                )

                V2Screen.PLAYING -> V2GameScreen(
                    world = world,
                    onPause = { screen = V2Screen.PAUSED },
                    onEnd = handleEnd
                )

                V2Screen.PAUSED -> {
                    // Static (frozen) game frame behind pause overlay
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF06132B))
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            renderV2(world)
                        }
                        V2Hud(world = world, onPause = {})
                    }
                    V2PauseOverlay(
                        onResume = { screen = V2Screen.PLAYING },
                        onRestart = {
                            world.startNewGame()
                            screen = V2Screen.PLAYING
                        },
                        onMenu = {
                            music.play(MusicTrack.MENU)
                            screen = V2Screen.MENU
                        },
                        onSettings = {
                            settingsFromPause = true
                            screen = V2Screen.SETTINGS
                        }
                    )
                }

                V2Screen.GAMEOVER -> V2GameOverScreen(
                    world = world,
                    save = save,
                    onRetry = {
                        world.startNewGame()
                        screen = V2Screen.PLAYING
                    },
                    onMenu = {
                        music.play(MusicTrack.MENU)
                        screen = V2Screen.MENU
                    }
                )

                V2Screen.SETTINGS -> V2SettingsScreen(
                    save = save,
                    music = music,
                    onBack = {
                        if (settingsFromPause) {
                            settingsFromPause = false
                            screen = V2Screen.PAUSED
                        } else {
                            screen = V2Screen.MENU
                        }
                    }
                )

                V2Screen.ABOUT -> V2AboutScreen(onBack = { screen = V2Screen.MENU })
            }
        }
    }
}

// ============================================================================
//  IN-GAME COMPOSABLE (game loop + render + input)
// ============================================================================

@Composable
private fun V2GameScreen(
    world: V2GameWorld,
    onPause: () -> Unit,
    onEnd: () -> Unit
) {
    // 60 FPS game loop
    LaunchedEffect(Unit) {
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    world.update(dt)
                }
                last = now
                world.frame++
            }
        }
    }

    // Chapter end detection
    val ch = world.chapter
    LaunchedEffect(ch) {
        if (ch == Chapter.DEFEAT || ch == Chapter.VICTORY) {
            onEnd()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF06132B))
            .onSizeChanged {
                world.resize(it.width.toFloat(), it.height.toFloat())
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    world.onDrag(dragAmount.y)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            renderV2(world)
        }

        V2Hud(world = world, onPause = onPause)
        V2DialogueOverlay(world = world)

        if (world.fighterMode) {
            V2FighterControls(world = world)
        } else if (world.bossActive) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(end = 30.dp, bottom = 18.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Button(
                    onClick = { world.firePlayer() },
                    modifier = Modifier.size(width = 120.dp, height = 58.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315))
                ) {
                    Text(
                        "شلیک",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================================================
//  ACTIVITY
// ============================================================================

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        setContent {
            OilTankerAppV2()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
