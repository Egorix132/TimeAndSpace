package com.example.timespace

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.homesoft.encoder.FrameEncoder
import com.homesoft.encoder.HevcEncoderConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import kotlin.properties.Delegates

var outputBitmaps = arrayListOf<Bitmap?>()
var videoDuration by Delegates.notNull<Int>()
var kStrech by Delegates.notNull<Int>()
var outputFrames2 = Array<Bitmap?>(1080) {null}
var isRendered = false
var iterator = 0
const val stride = 45
var width = 1080
var height = 1920
var FPS = 15f
private lateinit var videoView: VideoView
lateinit var progressBar: ProgressBar
var numberOfOutputFrames = 1080


class DistortedVideoActivity : AppCompatActivity() {

    private lateinit var mCurrentFile: File

    private fun startProgressBar() {
        val handler = Handler()
        val handlerTask = object : Runnable {
            override fun run(){
                progressBar.progress = (iterator + numberOfCompressedFrames) * 100 / (numberOfOutputFrames * typeCapturing)
                handler.postDelayed(this, 500)
            }
        }
        handlerTask.run()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.distorted_video)
        initialize()

        mCurrentFile = File(
            getExternalFilesDir(Environment.DIRECTORY_DCIM),
            "test${Date().time}.mp4")

        Log.e("frames", "$capturedFrames")
        videoDuration = capturedFrames

        if(typeCapturing==2){
            width = 1920
            height = 1080
            numberOfOutputFrames = videoDuration - stride
        }
        else{
            kStrech = 1080/videoDuration
        }

        startProgressBar()

        EncodeFrames(mCurrentFile.absolutePath, "encoder", this).start()

        RenderFrames("encoder").start()
    }


    private fun initialize(){
        videoView = findViewById(R.id.videoView)
        videoView.visibility = View.GONE
        progressBar = findViewById(R.id.progressBar)
    }
}

private fun watch(path:String){
    videoView.setVideoURI(path.toUri())
    videoView.start()
}

class RollingShutter(private var numberFrame:Int):Runnable {
    override fun run() {
        val bitmap = Bitmap.createBitmap(1920,1080, Bitmap.Config.RGB_565)
        var index = 0
        while (index < height) {
            val pixels = IntArray(width) { 0 }
            outputBitmaps[numberFrame+index/(height/stride)]!!.getPixels(pixels, 0, width, 0, index, width-1, 1)
            bitmap.setPixels(pixels, 0, width, 0, index, width-1, 1)
            index++
        }
        outputBitmaps[numberFrame] = null
        outputFrames2[numberFrame] = bitmap
    }
}

class ChangeTimeSpace(private var indexWidth:Int):Runnable {
    override fun run() {
        val bitmap = Bitmap.createBitmap(1080,1920, Bitmap.Config.RGB_565)
        var index = 0
        while (index < videoDuration) {
            val pixels = IntArray(1920) { 0 }
            inputFrames[index]!!.getPixels(pixels, 0, 1, indexWidth, 0, 1, height)
            for(i in 0 until kStrech)
                bitmap.setPixels(pixels, 0, 1, index*kStrech+i, 0, 1, height)
            index++
        }
        outputFrames2[indexWidth] = bitmap
    }
}

class RenderThread(name: String) : HandlerThread(name) {

    private var mWorkerHandler: Handler? = null

    fun postTask(task: Runnable) {
        mWorkerHandler!!.post(task)
    }

    fun prepareHandler() {
        mWorkerHandler = Handler(looper)
    }
}

class EncodeFrames (private val path:String, name:String, private val activity: AppCompatActivity) : HandlerThread(name) {
    var mEncoderConfig = HevcEncoderConfig(path, width, height, FPS,2000000)
    private val frameEncoder = FrameEncoder(mEncoderConfig)
    private val numberOfOutputFrames = when(typeCapturing){
        1 -> 1080
        2 -> videoDuration - stride
        else -> 0
    }
    override fun run() {
        frameEncoder.start()
        while (iterator < numberOfOutputFrames) {
            if (outputFrames2[iterator] != null) {
                frameEncoder.createFrame(outputFrames2[iterator])
                Log.e("already  Frames", iterator.toString())
                outputFrames2[iterator] = null
                iterator++
            }
        }
        frameEncoder.release()
        isRendered = true
        activity.runOnUiThread{
            inputFrames = arrayListOf()
            compressedBitmaps = Array(numOfStreams){ ByteArrayOutputStream() }
            Log.e("watch", "$iterator")
            progressBar.visibility = View.GONE
            videoView.visibility = View.VISIBLE
            watch(path)
        }
    }
}

class RenderFrames (name:String) : HandlerThread(name) {
    private val numberOfOutputFrames = when(typeCapturing){
        1 -> 1080
        2 -> videoDuration- stride
        else -> 0
    }
    var i =0
    override fun run() {
        while(!AllFramesIsCompressed) {}
        if(typeCapturing==1) {
            while (i < numberOfOutputFrames) {
                if (iterator > i - numTreads) {
                    threads[i % numTreads]!!.postTask(ChangeTimeSpace(i))
                    i++
                }
            }
        }
        else{
            for(i in 0 until stride){
                val byteArray: ByteArray = compressedBitmaps[i]!!.toByteArray()
                outputBitmaps.add(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size).rotate(270f))
                compressedBitmaps[i]!!.reset()
            }
            while (i < numberOfOutputFrames) {
                if (iterator > i - numTreads) {
                    val byteArray: ByteArray = compressedBitmaps[i+stride]!!.toByteArray()
                    outputBitmaps.add(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size).rotate(270f))
                    compressedBitmaps[i]!!.reset()
                    threads[i % numTreads]!!.postTask(RollingShutter(i))
                    i++
                }
            }
        }
    }
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

