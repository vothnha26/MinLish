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

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentOutputFile?.absolutePath)
                prepare()
                start()
                isRecording = true
                Log.d("AudioRecorder", "Recording started: ${currentOutputFile?.absolutePath}")
                return true
            } catch (e: IOException) {
                Log.e("AudioRecorder", "prepare() failed", e)
                return false
            } catch (e: Exception) {
                Log.e("AudioRecorder", "start() failed", e)
                return false
            }
        }
        return false
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
