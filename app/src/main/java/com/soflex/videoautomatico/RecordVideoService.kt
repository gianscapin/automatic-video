package com.soflex.videoautomatico

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordVideoService: Service() {

    lateinit var mediaRecorder: MediaRecorder
    lateinit var nameVideo: String
    lateinit var fileVideo: File
//    private lateinit var wakeLock: PowerManager.WakeLock

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): RecordVideoService = this@RecordVideoService
    }

    private val NOTIFICATION_ID = 1234
    private val CHANNEL_ID = "MotionDetectionChannel"

    override fun onCreate() {
        super.onCreate()

//        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
//        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VideoRecordingService::lock")
//        wakeLock.acquire(10*60*1000L /*10 minutes*/)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Motion Detection",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }
    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    fun startRecording() {
        mediaRecorder = MediaRecorder()

        try {
//            val file = createFile()

            val cw = ContextWrapper(applicationContext)
            val directory = cw.getDir("videoDir", MODE_PRIVATE)

            mediaRecorder.apply {
                //setInputSurface(surface)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(640, 480)
                setVideoFrameRate(30)
                setOutputFile("${directory.absolutePath}/VIDEO_SOS.mp4")
                prepare()
                start()
            }

//            isRecording = true
//            mButton.text = "Stop Recording"

        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            Toast.makeText(this, "Error starting recording", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        //mediaRecorder.stop()

        //obtainFiles()
        mediaRecorder.release()
        //mediaRecorder.reset()

    }

    private fun obtainFiles(){
        val cw = ContextWrapper(applicationContext)
        val directory = cw.getDir("videoDir", MODE_PRIVATE)

        for (file in directory.listFiles()!!){


            val bytes = file.length()
            Log.d(TAG, (bytes.toDouble() / (1024 * 1024)).toString())
        }
    }

    private fun createFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cw = ContextWrapper(applicationContext)
        val directory = cw.getDir("videoDir", MODE_PRIVATE)
        nameVideo = "VIDEO_${timestamp}_"
        fileVideo = File.createTempFile(nameVideo, ".mp4", directory)
        return fileVideo
        //return File.createTempFile(nameVideo, ".mp4", directory)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Motion Detection Service")
            .setContentText("Service is running in the background")
            .build()
    }



    companion object {
        private const val TAG = "RecordVideoService"
    }


}