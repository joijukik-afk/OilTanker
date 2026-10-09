package com.oiltanker

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class MusicTrack {
    NONE, MENU, LEVEL, BOSS, VICTORY, GAMEOVER, STORY
}

class MusicEngine {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private var audio: AudioTrack? = null
    private var current: MusicTrack = MusicTrack.NONE

    var enabled: Boolean = true
    var volume: Float = 0.5f

    private val sampleRate = 22050

    private val melodies: Map<MusicTrack, List<Pair<Float, Int>>> = mapOf(
        MusicTrack.MENU to listOf(
            392f to 350, 494f to 350, 587f to 350, 494f to 350,
            392f to 350, 349f to 350, 330f to 350, 349f to 350,
            392f to 350, 494f to 350, 587f to 500, 0f to 150,
            659f to 350, 587f to 350, 494f to 700
        ),
        MusicTrack.LEVEL to listOf(
            523f to 180, 587f to 180, 659f to 180, 523f to 180,
            659f to 320, 523f to 180, 440f to 320,
            523f to 180, 587f to 180, 659f to 180, 698f to 180,
            784f to 320, 659f to 180, 523f to 320,
            440f to 180, 523f to 180, 587f to 180, 659f to 180,
            587f to 320, 523f to 180, 440f to 320,
            392f to 180, 440f to 180, 523f to 180, 587f to 180,
            659f to 500
        ),
        MusicTrack.BOSS to listOf(
            110f to 200, 110f to 100, 165f to 200, 110f to 100,
            220f to 200, 165f to 100, 110f to 200, 82f to 200,
            110f to 200, 165f to 200, 220f to 200, 247f to 300,
            220f to 200, 165f to 200, 110f to 400,
            82f to 200, 110f to 200, 165f to 200, 220f to 200,
            165f to 200, 110f to 200, 82f to 400
        ),
        MusicTrack.VICTORY to listOf(
            523f to 180, 659f to 180, 784f to 180, 1047f to 360,
            784f to 180, 1047f to 720,
            880f to 180, 1047f to 180, 1319f to 600
        ),
        MusicTrack.GAMEOVER to listOf(
            440f to 400, 415f to 400, 392f to 400, 349f to 800,
            330f to 400, 294f to 400, 262f to 1200
        ),
        MusicTrack.STORY to listOf(
            349f to 500, 392f to 500, 440f to 500, 392f to 500,
            349f to 500, 330f to 500, 294f to 1000
        )
    )

    fun play(t: MusicTrack) {
        if (current == t && job?.isActive == true) return
        stop()
        current = t
        if (t == MusicTrack.NONE) return
        val notes = melodies[t] ?: return
        job = scope.launch {
            runCatching { playMelody(notes) }
        }
    }

    private suspend fun playMelody(notes: List<Pair<Float, Int>>) {
        val totalSamples = notes.sumOf { sampleRate * it.second / 1000 }
        val buffer = ShortArray(totalSamples)
        var pos = 0
        for ((freq, durMs) in notes) {
            val count = sampleRate * durMs / 1000
            if (freq > 0f && count > 0) {
                val twoPiF = 2.0 * PI * freq / sampleRate
                val attack = max(1, (count * 0.05f).toInt())
                val release = max(1, (count * 0.20f).toInt())
                for (i in 0 until count) {
                    val env = when {
                        i < attack -> i.toFloat() / attack
                        i > count - release -> (count - i).toFloat() / release
                        else -> 1f
                    }
                    val v = (sin(twoPiF * i) * Short.MAX_VALUE * volume * 0.35f * env).toInt()
                    buffer[pos + i] = v.coerceIn(-32768, 32767).toShort()
                }
            }
            pos += count
        }

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = max(minBuf, sampleRate) * 2

        val at = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audio = at
        at.play()

        while (scope.isActive && job?.isActive == true) {
            var offset = 0
            while (offset < buffer.size && job?.isActive == true) {
                if (!enabled) {
                    delay(100)
                    continue
                }
                val chunk = min(1024, buffer.size - offset)
                val written = at.write(buffer, offset, chunk)
                if (written > 0) offset += written else delay(20)
            }
            delay(250)
        }

        if (audio === at) audio = null
        runCatching { at.stop() }
        runCatching { at.release() }
    }

    fun stop() {
        job?.cancel()
        job = null
        current = MusicTrack.NONE
        val a = audio
        audio = null
        if (a != null) {
            runCatching { a.pause() }
            runCatching { a.flush() }
        }
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
