package com.example.timespace

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream


var inputBitmaps = Array<Bitmap?>(200){null}
var inputFrames = arrayListOf<Bitmap?>()
const val numOfStreams = 1000
var compressedBitmaps = Array<ByteArrayOutputStream?>(numOfStreams){ ByteArrayOutputStream() }

var typeCapturing = 1
const val numTreads = 8
val threads = Array<RenderThread?>(numTreads) { null }
var AllFramesIsCompressed = false
var maxFPS = Range(0,0)
var highSpeed = false
var numberOfCompressedFrames = 0
var capturedFrames = 0
var recording = false

class MainActivity : AppCompatActivity() {

    private lateinit var camManager: CameraManager
    private lateinit var mTextureView: TextureView
    private lateinit var recordBtn: Button
    private lateinit var switchButton: Button
    private lateinit var timerText: TextView
    private lateinit var fpsText: TextView

    private var myCameras: MutableList<CameraService> = mutableListOf()

    var curCam = 0

    private lateinit var mBackgroundThread: HandlerThread
    private var mBackgroundHandler: Handler? = null

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread.start()
        mBackgroundHandler = Handler(mBackgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        mBackgroundThread.quitSafely()
        try {
            mBackgroundThread.join()
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startBackgroundThread()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        getPermissions()
        initialize()

        for (thread in threads.indices) {
            threads[thread] = RenderThread("Thread #$thread")
            threads[thread]!!.start()
            threads[thread]!!.prepareHandler()
        }

        try {
            for (camId in camManager.cameraIdList) {
                // создаем обработчик для камеры
                val char = camManager.getCameraCharacteristics(camId)
                val config = char.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                config!!.highSpeedVideoFpsRanges.forEach {
                    if(it.upper> maxFPS.upper) maxFPS = it
                }
                if(maxFPS.upper>60){
                    highSpeed = true
                }
            }
        } catch (e: CameraAccessException){
            e.printStackTrace()
        }
        myCameras.add(0, CameraService(camManager, "0", this, mBackgroundHandler))
    }


    public override fun onPause() {
        myCameras[curCam].closeCamera()
        if(recording){
            recording = false
        }
        super.onPause()
        stopBackgroundThread()
    }

    override fun onStop() {
        myCameras[curCam].closeCamera()
        super.onStop()
    }

    override fun onDestroy() {
        myCameras[curCam].closeCamera()
        super.onDestroy()
    }

    public override fun onResume() {
        super.onResume()
        if(mTextureView.isAvailable)
            myCameras[curCam].openCamera(mTextureView)
        startBackgroundThread()
    }

    private fun initialize(){
        camManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        mTextureView = findViewById(R.id.textureView)
        mTextureView.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean { return true }
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                myCameras[curCam].openCamera(mTextureView)
            }
        }

        timerText = findViewById(R.id.timerText)
        fpsText = findViewById(R.id.fpsText)

        recordBtn = findViewById(R.id.recordBtn)
        switchButton = findViewById(R.id.switchBtn)

        val modeSwitcher = ModeSwitcher(mTextureView)
        val videoManger = VideoManager(mTextureView, this)
        val recordTimer = RecordTimer(timerText, fpsText, this)

        recordBtn.setOnClickListener {
            if(!videoManger.recording) {
                recordTimer.startTimer(modeSwitcher, videoManger)
                videoManger.startCapture()
            }
            else{
                videoManger.stopCapture()
            }
        }

        switchButton.setOnClickListener {
            modeSwitcher.nextMode()
            typeCapturing =  modeSwitcher.currentModeId
            switchButton.text = modeSwitcher.currentModeId.toString()
        }
    }

    private fun getPermissions(){
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                ), 1
            )
        }
    }
}
