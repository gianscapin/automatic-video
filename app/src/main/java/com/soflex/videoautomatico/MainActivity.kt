package com.soflex.videoautomatico

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File


class MainActivity : AppCompatActivity(), ServiceConnection {

    lateinit var mButton: Button
    lateinit var bVideo: Button

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var cameraDevice: CameraDevice
    private lateinit var previewTextureView: TextureView
    private var mRecordVideoService: RecordVideoService? = null
    private var mBound: Boolean = false

    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null

    private var isRecording = false
    lateinit var nameVideo: String
    lateinit var fileVideo: File

    lateinit var ivRecording: ImageView


    private lateinit var videoView: VideoView

    private var handlerRecording: Handler = Handler(Looper.getMainLooper())
    private var toggleVisibilityTask: Runnable? = null

    val SOS_BR = "com.kodiak.intent.action.KEYCODE_SOS"
    val SOS_UP_BR = "android.intent.action.sos.up"
    val SOS_RUGGEAR = "com.ruggear.intent.action.SOS"

    var lastProcessedTime = 0L
    val MIN_PROCESSING_INTERVAL = 5000L // 1 segundo


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        registerReceiver(sosKeyUpBroadcast, IntentFilter(SOS_UP_BR))
        val intentFilter = IntentFilter(SOS_RUGGEAR)
        intentFilter.addAction(SOS_BR)
        registerReceiver(sosKeyRuggearBroadcast, intentFilter)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        previewTextureView = findViewById(R.id.textureView)
        previewTextureView.surfaceTextureListener = previewSurfaceTextureListener

        mButton = findViewById(R.id.btn_recordVideo)
        bVideo = findViewById(R.id.btn_watchVideo)

        videoView = findViewById(R.id.videoView)

        ivRecording = findViewById(R.id.ivRecording)


        bVideo.setOnClickListener {
            playLastVideo()
        }

        val serviceIntent = Intent(this, RecordVideoService::class.java)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
////                startForegroundService(serviceIntent)
//            startService(serviceIntent)
//        } else {
//            startService(serviceIntent)
//        }
//        bindService(intent, this, Context.BIND_AUTO_CREATE)


        // Configurar un controlador de medios para el VideoView
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        mButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                if (arePermissionsGranted()) {
//                    openCamera()
                    previewTextureView.visibility = View.VISIBLE
                }
            }
        }

        if (!arePermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1
            )
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, RecordVideoService::class.java).also { intent ->
            bindService(intent, this, Context.BIND_AUTO_CREATE)

        }
    }


    private fun startRecordingAnimation() {
        isRecording = true
        toggleVisibilityTask = object : Runnable {
            override fun run() {
                // Alternar la visibilidad del icono de grabación
                ivRecording.visibility = if (ivRecording.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
                // Programar la siguiente ejecución después de un intervalo de tiempo
                handlerRecording.postDelayed(this, 500) // 500 milisegundos
            }
        }
        // Iniciar la tarea de alternar visibilidad
        handlerRecording.post(toggleVisibilityTask!!)
    }

    private fun stopRecordingAnimation() {
        isRecording = false
        // Detener la tarea de alternar visibilidad
        handlerRecording.removeCallbacks(toggleVisibilityTask!!)
        // Ocultar el icono de grabación
        ivRecording.visibility = View.INVISIBLE
    }


    private fun startRecording() {
        try {
            isRecording = true
            mButton.text = "Stop Recording"
            mRecordVideoService!!.startRecording()

        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            Toast.makeText(this, "Error starting recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        //mediaRecorder.stop()


//        obtainFiles()
        previewTextureView.visibility = View.GONE
        isRecording = false
        mButton.text = "Start Recording"

        stopRecordingAnimation()
        bVideo.isEnabled = true
        mRecordVideoService!!.stopRecording()

    }

    private fun playLastVideo() {
        val cw = ContextWrapper(applicationContext)
        val directory = cw.getDir("videoDir", MODE_PRIVATE)
        //val uriFile = FileProvider.getUriForFile(this, directory.listFiles().last())

        val file = Uri.parse("${directory.absolutePath}/VIDEO_SOS.mp4")
        reproducirVideo(file)
    }

    fun reproducirVideo(uri: Uri) {
        videoView.visibility = View.VISIBLE
        videoView.setVideoURI(uri)
        videoView.start()
    }

    private fun arePermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
    }

    private val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startRecording()
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice.close()
            this@MainActivity.finish()
        }
    }
    private val previewSurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            // No implementation needed
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
            // No implementation needed
        }
    }

    private fun openCamera() {
        try {
            val cameraIdList = cameraManager.cameraIdList
            if (cameraIdList.isEmpty()) {
                Toast.makeText(this, "No camera available", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            cameraId = cameraIdList[0] // Use the first available camera
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            cameraManager.openCamera(cameraId, cameraDeviceStateCallback, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Cannot access the camera", e)
            finish()
        }
    }


    private fun createCameraPreviewSession() {
        try {
//            surfaceTexture = mediaRecorder.surface
//            surfaceTexture!!.setDefaultBufferSize(previewTextureView.width, previewTextureView.height)
            surface = mRecordVideoService!!.mediaRecorder.surface

            val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface!!)
            }

            cameraDevice.createCaptureSession(listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            session.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "Failed to start camera preview", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed to configure camera preview")
                    }
                }, null)
            startRecordingAnimation()
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create camera preview session", e)
        }
    }


    private val sosKeyUpBroadcast: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action ==SOS_UP_BR) {
                if (isRecording) {
                    stopRecording()
                } else {
                    if (arePermissionsGranted()) {
                        openCamera()
//                        previewTextureView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private val sosKeyRuggearBroadcast: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action ==SOS_RUGGEAR || intent.action == SOS_BR) {
                val currentTime = System.currentTimeMillis()
                if(currentTime - lastProcessedTime > MIN_PROCESSING_INTERVAL){
                    if (isRecording) {
                        stopRecording()
                    } else {
                        if (arePermissionsGranted()) {
                            openCamera()
//                        previewTextureView.visibility = View.VISIBLE
                        }
                    }
                    lastProcessedTime = currentTime
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        //cameraDevice.close()
    }

    companion object {
        private const val TAG = "CameraPreviewActivity"
    }

    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
        val binder = p1 as RecordVideoService.LocalBinder
        mRecordVideoService = binder.getService()
        mBound = true
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        mBound = false
    }
}