package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Environment
import android.util.Log
import java.io.File
import kotlin.math.sin

object SoundManager {
    private const val TAG = "SoundManager"

    private var mediaPlayer: MediaPlayer? = null
    private var isPlayingBgm = false

    // Synthesize a short navigation click sound in memory
    private val clickPcmData: ByteArray by lazy {
        val sampleRate = 22050
        val durationMs = 15
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ByteArray(numSamples * 2)
        val frequency = 800.0 // 800 Hz beep click

        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i * frequency / sampleRate
            // Apply exponential decay envelope for a clean click
            val envelope = Math.exp(-4.0 * i / numSamples)
            val sampleVal = (sin(angle) * 16383 * envelope).toInt().coerceIn(-32768, 32767)

            buffer[i * 2] = (sampleVal and 0x00FF).toByte()
            buffer[i * 2 + 1] = ((sampleVal shr 8) and 0x00FF).toByte()
        }
        buffer
    }

    fun playNavSound(enabled: Boolean, context: Context? = null, selectedSfxFileName: String = "") {
        if (!enabled) return

        if (context != null && selectedSfxFileName.isNotEmpty() && selectedSfxFileName != "Default") {
            val sfxFolder = getSfxDirectory(context)
            val customFile = File(sfxFolder, selectedSfxFileName)
            if (customFile.exists() && customFile.isFile) {
                try {
                    val sfxPlayer = MediaPlayer().apply {
                        setDataSource(customFile.absolutePath)
                        setVolume(0.8f, 0.8f)
                        prepare()
                        start()
                        setOnCompletionListener {
                            try {
                                it.release()
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
                    return
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing custom SFX file: $selectedSfxFileName", e)
                }
            }
        }

        try {
            val sampleRate = 22050
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(clickPcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(clickPcmData, 0, clickPcmData.size)
            audioTrack.play()
            // Clean up AudioTrack after playing duration
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
            }, 50)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing navigation sound", e)
        }
    }

    fun getSfxDirectory(context: Context): File {
        return com.example.data.config.ConfigStorageManager(context).getSfxDir()
    }

    fun getSfxFileList(context: Context): List<String> {
        val sfxFolder = getSfxDirectory(context)
        val files = sfxFolder.listFiles { file ->
            val name = file.name.lowercase()
            file.isFile && (name.endsWith(".wav") || name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".m4a") || name.endsWith(".flac"))
        } ?: emptyArray()
        return files.map { it.name }.sorted()
    }

    fun updateBgmState(context: Context, enabled: Boolean) {
        if (!enabled) {
            stopBgm()
            return
        }

        if (isPlayingBgm) return

        try {
            val bgmFolder = getBgmDirectory(context)
            if (!bgmFolder.exists()) {
                bgmFolder.mkdirs()
            }

            val audioFiles = bgmFolder.listFiles { file ->
                val name = file.name.lowercase()
                file.isFile && (name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav") || name.endsWith(".m4a") || name.endsWith(".flac"))
            } ?: emptyArray()

            if (audioFiles.isEmpty()) {
                Log.d(TAG, "No BGM files found in ${bgmFolder.absolutePath}")
                stopBgm()
                return
            }

            val trackToPlay = audioFiles.random()
            stopBgm()

            val player = MediaPlayer().apply {
                setDataSource(trackToPlay.absolutePath)
                isLooping = true
                setVolume(0.35f, 0.35f)
                prepare()
                start()
            }
            mediaPlayer = player
            isPlayingBgm = true
            Log.d(TAG, "Started playing BGM: ${trackToPlay.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting BGM player", e)
            stopBgm()
        }
    }

    fun pauseBgm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing BGM", e)
        }
    }

    fun resumeBgm(context: Context) {
        try {
            mediaPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                    return
                }
            }
            updateBgmState(context, true)
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming BGM", e)
            updateBgmState(context, true)
        }
    }

    fun stopBgm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BGM", e)
        } finally {
            mediaPlayer = null
            isPlayingBgm = false
        }
    }

    private fun getBgmDirectory(context: Context): File {
        return com.example.data.config.ConfigStorageManager(context).getBgmDir()
    }
}
