package com.edu.minlish.core.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    
    var isRecording = false
        private set

    fun startRecording(): Boolean {
        if (isRecording) return false

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "REC_${timeStamp}.m4a"
        val storageDir = context.cacheDir

        currentOutputFile = File(storageDir, fileName)

        // Try with optimal high-quality settings first
        val success = tryRecord(currentOutputFile, useOptimalSettings = true)
        if (success) {
            isRecording = true
            return true
        }

        Log.w("AudioRecorder", "Failed to start recording with optimal settings. Retrying with default settings...")
        // Fallback to default settings
        val fallbackSuccess = tryRecord(currentOutputFile, useOptimalSettings = false)
        if (fallbackSuccess) {
            isRecording = true
            return true
        }

        return false
    }

    private fun tryRecord(outputFile: File?, useOptimalSettings: Boolean): Boolean {
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                
                if (useOptimalSettings) {
                    setAudioSamplingRate(44100) // 44.1kHz is extremely safe and supported by all Android devices
                    setAudioEncodingBitRate(64000)
                    setAudioChannels(1)
                }

                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            Log.d("AudioRecorder", "Recording started successfully (optimal=$useOptimalSettings): ${outputFile?.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "tryRecord(optimal=$useOptimalSettings) failed", e)
            try {
                mediaRecorder.release()
            } catch (ex: Exception) {
                // Ignore release errors
            }
            false
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null

        try {
            recorder?.apply {
                stop()
                release()
            }
            Log.d("AudioRecorder", "Recording stopped")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "stop() failed", e)
        } finally {
            recorder = null
            isRecording = false
        }

        return currentOutputFile
    }

    fun release() {
        recorder?.release()
        recorder = null
        isRecording = false
    }
}
