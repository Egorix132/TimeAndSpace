package com.example.timespace

import MSurfaceTextureListener
import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

var typeCapturing = 1

class MainActivity : AppCompatActivity() {

    lateinit var camManager: CameraManager
    lateinit var mImageView: TextureView
    lateinit var buttonRecord: Button
    lateinit var switchButton: Button
    private lateinit var surfaceTextureListener: SurfaceTextureListener
    private lateinit var mMediaRecorder: MediaRecorder

    private var myCameras: MutableList<CameraService> = mutableListOf()

    private val cam1 = 0
    private val cam2 = 1
    var curCam = 0

    var recording = false

    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startBackgroundThread()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        Log.e("create", "dffdf")


        getPermissions()
        initialize()

        /*try {
            for (camId in camManager.cameraIdList) {
                // Получение списка камер с устройства
                val id = Integer.parseInt(camId)

                // создаем обработчик для камеры
                myCameras.add(id, CameraService(camManager, camId, this, mImageView, mBackgroundHandler))
                var char = camManager.getCameraCharacteristics(camId)
                Log.e("addcam", myCameras[cam1].toString())
            }
        } catch (e: CameraAccessException){
            e.printStackTrace()
        }*/
        myCameras.add(0, CameraService(camManager, "0", this, mImageView, mBackgroundHandler))


        Thread{
            do {
                if(mImageView.isAvailable){
                    myCameras[cam1].openCamera()
                    Log.e("opeencum", myCameras[cam1].toString())
                }
            }
            while(!mImageView.isAvailable)
        }.start()
    }

    public override fun onPause() {
        Log.e("pause", "dffdf")
        myCameras[curCam].closeCamera()
        if(recording){
            myCameras[curCam].stopRecordingVideo()
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
        Log.e("resume", "dffdf")

        super.onResume()
        startBackgroundThread()

        Thread{
            do {
                if(mImageView.isAvailable){
                    myCameras[cam1].openCamera()
                    Log.e("opencum", myCameras[cam1].toString())
                }

            }
            while(!mImageView.isAvailable)
        }.start()
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

    private fun initialize(){
        camManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        surfaceTextureListener = MSurfaceTextureListener()
        mImageView = findViewById(R.id.textureView)
        mImageView.surfaceTextureListener = surfaceTextureListener
        buttonRecord = findViewById(R.id.button6)
        switchButton = findViewById(R.id.switchButton)

        buttonRecord.setOnClickListener {
            recording = if(!recording) {
                myCameras[curCam].startRecordingVideo()
                true
            } else{
                myCameras[curCam].stopRecordingVideo()
                false
            }
        }
        switchButton.setOnClickListener {
            typeCapturing = if(typeCapturing==1){
                switchButton.text = "2"
                2
            }
            else{
                switchButton.text = "1"
                1
            }
        }
    }

}

