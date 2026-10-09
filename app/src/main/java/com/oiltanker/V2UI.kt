package com.oiltanker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

// ============================================================================
//  MAIN MENU v2
// ============================================================================

@Composable
fun V2MainMenu(
    save: SaveData,
    onNewGame: () -> Unit,
    onContinue: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onStory: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A1A3A), Color(0xFF0D47A1), Color(0xFF1A237E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: title
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "نفتکش",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "عملیات نجات",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F)
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "بهترین امتیاز: ${save.bestScore}",
                    fontSize = 18.sp,
                    color = Color(0xFFFFEB3B)
                )
                Text(
                    "فصل باز شده: ${save.highestChapter}",
                    fontSize = 16.sp,
                    color = Color(0xFFB0BEC5)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "مجموع کشته‌ها: ${save.totalKills}",
                    fontSize = 14.sp,
                    color = Color(0xFF90A4AE)
                )
            }
            // Right: buttons
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                V2MenuButton("شروع داستان", Color(0xFF6A1B9A), onStory)
                Spacer(Modifier.height(12.dp))
                V2MenuButton("بازی جدید", Color(0xFF2E7D32), onNewGame)
                if (save.highestChapter > 1) {
                    Spacer(Modifier.height(12.dp))
                    V2MenuButton("ادامه از فصل ${save.highestChapter}", Color(0xFF1565C0), onContinue)
                }
                Spacer(Modifier.height(12.dp))
                V2MenuButton("تنظیمات", Color(0xFF37474F), onSettings)
                Spacer(Modifier.height(12.dp))
                V2MenuButton("درباره", Color(0xFF4E342E), onAbout)
            }
        }
    }
}

@Composable
fun V2MenuButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(width = 260.dp, height = 56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ============================================================================
//  STORY INTRO (cinematic)
// ============================================================================

@Composable
fun V2StoryIntroScreen(
    music: MusicEngine,
    onComplete: () -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(true) }
    var typedText by remember { mutableStateOf("") }

    val lines = Story.intro

    LaunchedEffect(Unit) {
        music.play(MusicTrack.STORY)
    }

    LaunchedEffect(index) {
        if (index >= lines.size) {
            onComplete()
            return@LaunchedEffect
        }
        visible = true
        val fullText = lines[index].text
        typedText = ""
        val chars = fullText.length
        val perChar = (lines[index].duration * 1000f / chars.coerceAtLeast(1)).toLong().coerceIn(20L, 90L)
        for (i in 1..chars) {
            typedText = fullText.substring(0, i)
            delay(perChar)
        }
        delay((lines[index].duration * 400).toLong())
        visible = false
    }

    BackHandler { onComplete() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .clickable {
                if (index < lines.size - 1) index++ else onComplete()
            }
    ) {
        // scanline / rain effect
        Canvas(Modifier.fillMaxSize()) {
            val t = System.currentTimeMillis() / 1000f
            for (i in 0 until 60) {
                val y = (i * size.height / 60f + (t * 40f) % (size.height / 60f)) % size.height
                drawRect(
                    Color(0x1100E5FF),
                    Offset(0f, y),
                    Size(size.width, 1.5f)
                )
            }
        }

        if (index < lines.size) {
            val line = lines[index]
            val speakerColor = Color(Story.color(line.speaker))

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xDD000000))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(Modifier.size(14.dp)) {
                                drawCircle(speakerColor)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                Story.name(line.speaker),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = speakerColor
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            typedText,
                            fontSize = 20.sp,
                            color = if (line.red) Color(0xFFFF5252) else Color.White,
                            lineHeight = 32.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "برای ادامه بزن ▶",
                        fontSize = 14.sp,
                        color = Color(0xFF90A4AE)
                    )
                }
            }
        }
    }
}

// ============================================================================
//  IN-GAME HUD
// ============================================================================

@Composable
fun V2Hud(world: V2GameWorld, onPause: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // hearts + barrels
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(V2_MAX_LIVES) { i ->
                Text(
                    if (i < world.lives) "❤" else "♡",
                    fontSize = 20.sp,
                    color = if (i < world.lives) Color(0xFFFF5252) else Color(0x55FFFFFF)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "🛢 ${world.barrels}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F)
            )
        }

        Spacer(Modifier.weight(1f))

        // mission + distance
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                world.missionText,
                fontSize = 13.sp,
                color = Color(0xFFFFEB3B),
                fontWeight = FontWeight.Bold
            )
            Text(
                world.objectiveText,
                fontSize = 11.sp,
                color = Color(0xFFB0BEC5)
            )
            Text(
                "${world.distance.toInt()} متر",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (world.ship.shield > 0f) V2PowerChip(Color(0xFF29B6F6), world.ship.shield / 5f)
            if (world.ship.magnet > 0f) V2PowerChip(Color(0xFFAB47BC), world.ship.magnet / 10f)
            if (world.ship.turbo > 0f) V2PowerChip(Color(0xFFFF7043), world.ship.turbo / 3f)
            if (world.fighterMode) {
                Text(
                    "✈ ${world.kills}/${CH3_TARGET_ENEMIES}",
                    fontSize = 13.sp,
                    color = Color(0xFF4FC3F7),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                "${world.score}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "II",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onPause() }
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun V2PowerChip(color: Color, progress: Float) {
    Box(
        Modifier
            .padding(horizontal = 3.dp)
            .size(width = 34.dp, height = 9.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0x66000000))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(5.dp))
                .background(color)
        )
    }
}

// ============================================================================
//  DIALOGUE OVERLAY
// ============================================================================

@Composable
fun V2DialogueOverlay(world: V2GameWorld) {
    val line = world.currentLine ?: return
    if (!world.dialogueVisible) return
    val speakerColor = Color(Story.color(line.speaker))

    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp, start = 40.dp, end = 40.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xDD000000))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(12.dp)) {
                    drawCircle(speakerColor)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    Story.name(line.speaker),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = speakerColor
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    line.text,
                    fontSize = 16.sp,
                    color = if (line.red) Color(0xFFFF5252) else Color.White
                )
            }
        }
    }
}

// ============================================================================
//  FIGHTER CONTROLS (fire + missile buttons)
// ============================================================================

@Composable
fun V2FighterControls(world: V2GameWorld) {
    Row(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Button(
                onClick = { world.fireMissile() },
                modifier = Modifier.size(width = 100.dp, height = 50.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
            ) {
                Text("موشک", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { world.firePlayer() },
                modifier = Modifier.size(width = 130.dp, height = 62.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315))
            ) {
                Text("شلیک", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ============================================================================
//  PAUSE OVERLAY
// ============================================================================

@Composable
fun V2PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onSettings: () -> Unit
) {
    BackHandler { onResume() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xDD000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "توقف بازی",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(22.dp))
            V2MenuButton("ادامه", Color(0xFF2E7D32), onResume)
            Spacer(Modifier.height(10.dp))
            V2MenuButton("شروع مجدد", Color(0xFFEF6C00), onRestart)
            Spacer(Modifier.height(10.dp))
            V2MenuButton("تنظیمات", Color(0xFF1565C0), onSettings)
            Spacer(Modifier.height(10.dp))
            V2MenuButton("منوی اصلی", Color(0xFF37474F), onMenu)
        }
    }
}

// ============================================================================
//  GAME OVER / VICTORY
// ============================================================================

@Composable
fun V2GameOverScreen(
    world: V2GameWorld,
    save: SaveData,
    onRetry: () -> Unit,
    onMenu: () -> Unit
) {
    BackHandler { onMenu() }
    val isVictory = world.chapter == Chapter.VICTORY

    Box(
        Modifier
            .fillMaxSize()
            .background(
                if (isVictory)
                    Brush.verticalGradient(listOf(Color(0xFF1B5E20), Color(0xFF0D47A1)))
                else
                    Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF000000)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isVictory) "پیروزی!" else "پایان بازی",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isVictory) Color(0xFFFFEB3B) else Color(0xFFFF5252)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "امتیاز: ${world.score}",
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "مسافت: ${world.distance.toInt()} متر",
                    fontSize = 16.sp,
                    color = Color(0xFFB0BEC5)
                )
                Text(
                    "بشکه‌ها: ${world.barrels}",
                    fontSize = 16.sp,
                    color = Color(0xFFFFD54F)
                )
                if (world.fighterMode) {
                    Text(
                        "اهداف نابود شده: ${world.kills}",
                        fontSize = 16.sp,
                        color = Color(0xFF4FC3F7)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "بهترین: ${save.bestScore}",
                    fontSize = 18.sp,
                    color = Color(0xFF81C784),
                    fontWeight = FontWeight.Bold
                )
                if (world.score >= save.bestScore && world.score > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "🏆 رکورد جدید!",
                        fontSize = 22.sp,
                        color = Color(0xFFFFEB3B),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                V2MenuButton("تلاش مجدد", Color(0xFF2E7D32), onRetry)
                Spacer(Modifier.height(12.dp))
                V2MenuButton("منوی اصلی", Color(0xFF37474F), onMenu)
            }
        }
    }
}

// ============================================================================
//  SETTINGS
// ============================================================================

@Composable
fun V2SettingsScreen(
    save: SaveData,
    music: MusicEngine,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF102027)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "تنظیمات",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))

            // Music toggle
            SettingRow("موسیقی", save.musicOn) { v ->
                save.musicOn = v
                music.enabled = v
                save.persistAll()
            }
            Spacer(Modifier.height(4.dp))
            SliderRow("میزان موسیقی", save.musicVol) { v ->
                save.musicVol = v
                music.volume = v
                save.persistAll()
            }

            Spacer(Modifier.height(8.dp))
            SettingRow("افکت صوتی", save.sfxOn) { v ->
                save.sfxOn = v
                save.persistAll()
            }
            Spacer(Modifier.height(8.dp))
            SettingRow("لرزش", save.vibrationOn) { v ->
                save.vibrationOn = v
                save.persistAll()
            }

            Spacer(Modifier.height(4.dp))
            SliderRow("حساسیت کنترل", save.sensitivity) { v ->
                save.sensitivity = v
                save.persistAll()
            }

            Spacer(Modifier.height(20.dp))
            V2MenuButton("بازگشت", Color(0xFF37474F), onBack)
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .width(360.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 18.sp, color = Color.White)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(
        Modifier
            .width(360.dp)
            .padding(vertical = 4.dp)
    ) {
        Text(label, fontSize = 16.sp, color = Color.White)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f
        )
    }
}

// ============================================================================
//  ABOUT
// ============================================================================

@Composable
fun V2AboutScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF102027)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "درباره بازی",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "نفتکش: عملیات نجات",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "سال ۲۰۴۵ — دریای خزر\n" +
                        "یک نفتکش در طوفان گرفتار شده.\n\n" +
                        "تو AmirAlavi85 هستی، خلبان نخبه.\n" +
                        "به دستور فرمانده Nargil، مأموریت داری\n" +
                        "نفتکش را نجات دهی و از Leviathan و Kraken عبور کنی.\n\n" +
                        "نسخه ۲.۰",
                fontSize = 15.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
            Spacer(Modifier.height(24.dp))
            V2MenuButton("بازگشت", Color(0xFF37474F), onBack)
        }
    }
}

// ============================================================================
//  SPLASH
// ============================================================================

@Composable
fun V2SplashScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFF1A237E)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(200.dp, 100.dp)) {
                val w = size.width
                val h = size.height
                val hull = Path()
                hull.moveTo(w * 0.02f, h * 0.42f)
                hull.lineTo(w * 0.78f, h * 0.42f)
                hull.lineTo(w * 0.96f, h * 0.62f)
                hull.lineTo(w * 0.86f, h * 0.92f)
                hull.lineTo(w * 0.10f, h * 0.92f)
                hull.lineTo(0f, h * 0.66f)
                hull.close()
                drawPath(hull, Color(0xFFB71C1C))
                for (i in 0..2) {
                    drawRoundRect(
                        Color(0xFF9E9E9E),
                        Offset(w * (0.12f + i * 0.22f), h * 0.10f),
                        Size(w * 0.16f, h * 0.28f),
                        CornerRadius(h * 0.08f)
                    )
                }
                drawRect(Color(0xFF424242), Offset(w * 0.05f, 0f), Size(w * 0.08f, h * 0.44f))
                drawLine(
                    Color.White,
                    Offset(0f, h * 0.72f), Offset(w, h * 0.72f),
                    strokeWidth = h * 0.05f
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "نفتکش",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "عملیات نجات",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F)
            )
        }
    }
}
