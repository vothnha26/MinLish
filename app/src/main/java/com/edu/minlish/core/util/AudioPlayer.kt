package com.edu.minlish.core.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

object AudioPlayer : TextToSpeech.OnInitListener {
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    fun init(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("AudioPlayer", "TTS Language not supported")
            } else {
                isTtsInitialized = true
            }
        } else {
            Log.e("AudioPlayer", "TTS Initialization failed")
        }
    }

    fun play(url: String, fallbackWord: String = "") {
        val playUrl = if (url.isBlank() && fallbackWord.isNotBlank()) {
            "https://dict.youdao.com/dictvoice?audio=${fallbackWord.trim().lowercase()}&type=2"
        } else {
            url
        }

        if (playUrl.isNotBlank()) {
            // Play from URL
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(playUrl)
                    prepareAsync()
                    setOnPreparedListener { start() }
                    setOnErrorListener { _, what, extra ->
                        Log.e("AudioPlayer", "Error playing audio: $what, $extra")
                        // Fallback to TTS if URL fails
                        playTts(fallbackWord)
                        true
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Failed to setup MediaPlayer", e)
                playTts(fallbackWord)
            }
        } else {
            // Fallback to TTS if no URL
            playTts(fallbackWord)
        }
    }

    private fun playTts(word: String) {
        if (word.isNotBlank() && isTtsInitialized) {
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsInitialized = false
    }
}
