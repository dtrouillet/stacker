package fr.damientrouillet.stacker.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Marimba-like blips synthesised at runtime, so the app ships without any audio
 * asset. Each perfect placement in a row plays the next note of a pentatonic
 * scale, exactly like the tone ladder of the original game.
 */
class SoundEngine(context: Context) {

    private companion object {
        const val SAMPLE_RATE = 22050
        const val TONE_COUNT = 16
        const val BASE_FREQUENCY = 261.63

        /** Major pentatonic degrees, in semitones, extended over three octaves. */
        val SCALE = intArrayOf(0, 2, 4, 7, 9, 12, 14, 16, 19, 21, 24, 26, 28, 31, 33, 36)
    }

    private val pool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val toneIds = IntArray(TONE_COUNT)

    @Volatile
    private var failId = 0

    @Volatile
    private var released = false

    /** When false every playback call is a no-op, so the game runs silently. */
    @Volatile
    var enabled: Boolean = true

    private val appContext = context.applicationContext

    init {
        Thread({ prepare() }, "stacker-sfx").apply { isDaemon = true }.start()
    }

    private fun prepare() {
        try {
            val dir = File(appContext.cacheDir, "sfx").apply { mkdirs() }
            for (i in 0 until TONE_COUNT) {
                val frequency = BASE_FREQUENCY * 2.0.pow(SCALE[i] / 12.0)
                val file = File(dir, "tone_$i.wav")
                if (!file.exists()) {
                    writeWav(file, note(frequency, 0.32, 13.0))
                }
                if (released) return
                toneIds[i] = pool.load(file.absolutePath, 1)
            }
            val failFile = File(dir, "fail.wav")
            if (!failFile.exists()) writeWav(failFile, failSweep())
            if (released) return
            failId = pool.load(failFile.absolutePath, 1)
        } catch (error: Exception) {
            // Sound is a nicety; never let it take the game down.
        }
    }

    /** Plays the blip for a placement. [step] is the current perfect streak. */
    fun playPlace(step: Int) {
        if (!enabled) return
        val id = toneIds[step.coerceIn(0, TONE_COUNT - 1)]
        if (id != 0) pool.play(id, 0.85f, 0.85f, 1, 0, 1f)
    }

    fun playGameOver() {
        if (!enabled) return
        if (failId != 0) pool.play(failId, 0.9f, 0.9f, 1, 0, 1f)
    }

    fun release() {
        released = true
        pool.release()
    }

    // ------------------------------------------------------------ synthesis

    private fun note(frequency: Double, duration: Double, decay: Double): ShortArray {
        val count = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * decay) * min(1.0, t / 0.003)
            val value = sin(2 * PI * frequency * t) * 0.62 +
                sin(4 * PI * frequency * t) * 0.24 * exp(-t * decay * 2) +
                sin(6 * PI * frequency * t) * 0.09 * exp(-t * decay * 3)
            samples[i] = toPcm(value * envelope * 0.6)
        }
        return samples
    }

    private fun failSweep(): ShortArray {
        val duration = 0.55
        val count = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val frequency = 220.0 * (1.0 - 0.55 * (t / duration))
            phase += 2 * PI * frequency / SAMPLE_RATE
            val envelope = exp(-t * 4.0) * min(1.0, t / 0.006)
            val value = sin(phase) * 0.7 + sin(phase * 2) * 0.15
            samples[i] = toPcm(value * envelope * 0.6)
        }
        return samples
    }

    private fun toPcm(value: Double): Short =
        (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()

    private fun writeWav(file: File, samples: ShortArray) {
        val dataSize = samples.size * 2
        FileOutputStream(file).use { out ->
            out.write("RIFF".toByteArray())
            out.write(intLe(36 + dataSize))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intLe(16))
            out.write(shortLe(1))            // PCM
            out.write(shortLe(1))            // mono
            out.write(intLe(SAMPLE_RATE))
            out.write(intLe(SAMPLE_RATE * 2))
            out.write(shortLe(2))            // block align
            out.write(shortLe(16))           // bits per sample
            out.write("data".toByteArray())
            out.write(intLe(dataSize))
            val bytes = ByteArray(dataSize)
            for (i in samples.indices) {
                val v = samples[i].toInt()
                bytes[i * 2] = (v and 0xFF).toByte()
                bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
            }
            out.write(bytes)
        }
    }

    private fun intLe(value: Int) = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte()
    )

    private fun shortLe(value: Int) = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte()
    )
}
