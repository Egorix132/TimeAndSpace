package com.example.timespace

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


var typeCapturing = 1
const val numTreads = 8
val threads = Array<RenderThread?>(numTreads) { null }
var isRotated = false
var newActivity = false
var maxFPS = Range(0,0)
var highSpeed = false
var numberOfRotatedFrames = 0
var capturedFrames = 0

var inputBufferBitmaps = Array<Bitmap?>(105){null}
var outputBufferBitmaps = Array<Bitmap?>(60){null}
var canvas = Array<Canvas?>(60){null}

class MainActivity : AppCompatActivity() {

    lateinit var camManager: CameraManager
    lateinit var mImageView: TextureView
    lateinit var previewRollinShutter: TextureView
    lateinit var buttonRecord: Button
    lateinit var switchButton: Button
    private lateinit var timerText: TextView
    lateinit var fpsText: TextView

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

    lateinit var actvityManager:ActivityManager
    lateinit var mInfo: ActivityManager.MemoryInfo


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startBackgroundThread()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        Log.e("create", "dffdf")


        actvityManager = this.getSystemService (ACTIVITY_SERVICE) as ActivityManager
        mInfo = ActivityManager.MemoryInfo()

        getPermissions()
        initialize()


        for (thread in threads.indices) {
            threads[thread] = RenderThread("Thread #$thread")
            threads[thread]!!.start()
            threads[thread]!!.prepareHandler()
        }

        try {
            for (camId in camManager.cameraIdList) {
                // Получение списка камер с устройства
                val id = Integer.parseInt(camId)

                // создаем обработчик для камеры
                //myCameras.add(id, CameraService(camManager, camId, this, mImageView, mBackgroundHandler))
                var char = camManager.getCameraCharacteristics(camId)
                val config = char.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                config!!.highSpeedVideoFpsRanges.forEach {
                    if(it.upper> maxFPS.upper) maxFPS = it
                }
                if(maxFPS.upper>60){
                    highSpeed = true
                }
                //Log.e("addcam", myCameras[cam1].toString())
            }
        } catch (e: CameraAccessException){
            e.printStackTrace()
        }
        myCameras.add(0, CameraService(camManager, "0", this, mImageView, mBackgroundHandler))




        Thread{
            do {
            }
            while(!mImageView.isAvailable)
            if(mImageView.isAvailable){
                myCameras[cam1].openCamera()
            }
        }.start()
    }


    public override fun onPause() {
        Log.e("pause", "dffdf")
        myCameras[curCam].closeCamera()
        if(recording){
            //myCameras[curCam].stopRecordingVideo()
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


        mImageView = findViewById(R.id.textureView)
        previewRollinShutter = findViewById(R.id.preview_RollingShutter)
        timerText = findViewById(R.id.timer)
        fpsText = findViewById(R.id.fpsText)


        buttonRecord = findViewById(R.id.button6)
        switchButton = findViewById(R.id.switchButton)


        buttonRecord.setOnClickListener {
            if(!recording) {
                recording = true
                startTimer()
                mImageView.surfaceTextureListener = object : SurfaceTextureListener{
                    var isAvailable = false
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
                        inputBitmaps.add(mImageView.getBitmap(1080,1920))
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
                    }

                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                        isAvailable = true
                    }
                }
                if(typeCapturing==2){
                    Thread {
                        var i =0
                        while(recording || numberOfRotatedFrames<inputBitmaps.size-1){
                            if(inputBitmaps.size > i){
                                threads[i%4]!!.postTask(RotateFrames(i))
                                i++
                            }
                        }
                        isRotated = true
                    }.start()
                }
            }
            else{
                stopVideo()
            }
        }
        switchButton.setOnClickListener {
            typeCapturing = when (typeCapturing) {
                1 -> {
                    switchButton.text = "2"
                    2
                }
                2 -> {
                    switchButton.text = "3"
                    previewRollinShutter.z = 100f
                    mImageView.z = -100f
                    previewRollinShutter.visibility = View.VISIBLE
                    //mImageView.visibility = View.INVISIBLE
                    //mImageView.isOpaque = false
                    startPreviewRollingShutter()
                    3
                }
                3 -> {
                    switchButton.text = "1"
                    mImageView.visibility = View.VISIBLE
                    previewRollinShutter.visibility = View.GONE
                    1
                }
                else -> {
                    0
                }
            }
        }
    }

    private fun startPreviewRollingShutter(){
        Thread {
            var i = 0
            var delete = 0
            while(typeCapturing==3){
                //Log.e("input","${inputBufferBitmaps.size} $i")
                if(capturedFrames > stride+i){
                    outputBufferBitmaps[i%60] = null
                    threads[i%8]!!.postTask(setFrameRS(i%60+45))
                    i++
                }
                if(outputBufferBitmaps[delete%60]!=null) {
                    Log.e("aaa","$delete")
                    val canvas = previewRollinShutter.lockCanvas()
                    canvas.drawBitmap(outputBufferBitmaps[delete%60]!!,0f,800f,null)
                    previewRollinShutter.unlockCanvasAndPost(canvas)
                    Thread.sleep(50)
                    delete++
                }
            }
            isRotated = true
        }.start()
        mImageView.surfaceTextureListener = object : SurfaceTextureListener{
            var isAvailable = false
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
                //Log.e("aaaaa", "${capturedFrames%105}")
                inputBufferBitmaps[capturedFrames%105] = mImageView.getBitmap(1080,1920)
                capturedFrames++
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            }

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                isAvailable = true
            }
        }
    }

    private var handler: Handler? = null
    private var handlerTask: Runnable? = null

    private fun startTimer() {
        handler = Handler()
        val activityManager =  getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        var  memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        var lastFrame = compressedBitmaps.size
        handlerTask = Runnable {
            // do something

            timerText.text = "${(memoryInfo.availMem-200000000-memoryInfo.threshold)/(1920*1080*2*(1+inputBitmaps.size-lastFrame))}"
            fpsText.text = "FPS: ${inputBitmaps.size-lastFrame}"
            lastFrame = inputBitmaps.size
            activityManager.getMemoryInfo(memoryInfo)
            Log.e("memory","avM: ${memoryInfo.availMem} treshM: ${memoryInfo.threshold} stop?: ${memoryInfo.availMem-200000000<memoryInfo.threshold} total: ${memoryInfo.totalMem}")
            if(memoryInfo.availMem-200000000<memoryInfo.threshold && recording){
                stopVideo()
                handlerTask = null
            }
            else
                handler!!.postDelayed(handlerTask!!, 1000)
        }
        handlerTask!!.run()
    }

    private fun stopVideo(){
        recording = false
        mImageView.surfaceTextureListener = null
        /*if(typeCapturing==2) {
            while (!newActivity) {
                if (isRotated) {
                    val watchVideo = Intent(this, DistortedVideoActivity::class.java)
                    ContextCompat.startActivity(this, watchVideo, null)
                    newActivity = true
                    //myCameras[curCam].stopRecordingVideo()
                }
            }
        }
        else{*/
            val watchVideo = Intent(this, DistortedVideoActivity::class.java)
            ContextCompat.startActivity(this, watchVideo, null)
        //}
    }
}

/*class RotateFrames(private var frame:Int):Runnable {
    override fun run() {
        frames[frame] = frames[frame]!!.copy(Bitmap.Config.RGB_565, false).rotate(270f)
        numberOfRotatedFrames++
    }
}*/

class RotateFrames(private var frame:Int):Runnable {
    override fun run() {
        inputBitmaps[frame]!!.rotate(270f).compress(Bitmap.CompressFormat.JPEG,90,compressedBitmaps[frame])
        inputBitmaps[frame] = null
        numberOfRotatedFrames++
        Log.e("compres","$numberOfRotatedFrames ${inputBitmaps.size}")
    }
}

class setFrameRS(private var frame:Int):Runnable {
    override fun run() {
        //Log.e("sartas", "$frame")
        val bitmap = Bitmap.createBitmap(1080,1920, Bitmap.Config.RGB_565)
        var index = 0
        while (index < width) {
            val pixels = IntArray(height) { 0 }
            //Log.e("aaaaaaaaa", "$index ${frame-index/(height/stride)} ${index+width} ${bufferBitmaps[frame-index/(height/stride)]!!.width}")
            inputBufferBitmaps[frame-index/(width/stride)]!!.getPixels(pixels, 0, 1, index, 0, 1, width)
            bitmap.setPixels(pixels, 0, 1, index, 0, 1, width)
            index++
        }
        outputBufferBitmaps[(frame-45)%60] = bitmap
    }
}


