package com.example.timespace

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.widget.ImageView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.homesoft.encoder.EncoderConfig
import com.homesoft.encoder.FrameEncoder
import com.homesoft.encoder.HevcEncoderConfig
import org.jcodec.api.android.AndroidSequenceEncoder
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Rational
import java.io.File
import java.util.*
import kotlin.properties.Delegates









var videoDuration by Delegates.notNull<Int>()
var kStrech by Delegates.notNull<Int>()
var outputFrames2 = Array<Bitmap?>(1080) {null}
var isRendered = false
var iterator = 0
lateinit var encoder:AndroidSequenceEncoder
val booleans = Array<Boolean>(1080){false}
lateinit var encode:Thread
val stride = 45
var width = 1080
var height = 1920


class DistortedVideoActivity : AppCompatActivity() {

    lateinit var videoView: VideoView
    lateinit var imageView: ImageView
    lateinit var uri: String
    private lateinit var mCurrentFile: File
    private lateinit var mEncoderConfig: EncoderConfig


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.distorted_video)
        initialize()


        uri = intent.getStringExtra("uri")!!

        mCurrentFile = File(
            getExternalFilesDir(Environment.DIRECTORY_DCIM),
            "test${Date().time}.mp4")
        Log.e("durationOriginal",originalVideoDuration.toString())
        var FPS = 15f
        if(typeCapturing==2){
            FPS = (frames.size/ (originalVideoDuration/1000)).toFloat()
            width = 1920
            height = 1080
        }
        Log.e("FPS",FPS.toString())
        mEncoderConfig = HevcEncoderConfig(mCurrentFile.absolutePath,width,height, FPS,2000000)


        var out: SeekableByteChannel? = null
        out = NIOUtils.writableFileChannel(mCurrentFile.absolutePath)
        encoder = AndroidSequenceEncoder(out, Rational.R(25, 1))

        /*val bitmapToVideoEncoder = BitmapToVideoEncoder(object :
            BitmapToVideoEncoder.IBitmapToVideoEncoderCallback {
            override fun onEncodingComplete(outputFile: File?) {
                isRendered = true
            }
        })*/
        //val ffmpeg = FFmpegFrameRecorder(mCurrentFile, 1080,1920)

        Log.e("frames", frames.size.toString())
        videoDuration = frames.size
        if(videoDuration%2 != 0) videoDuration--

        Log.e("duration", videoDuration.toString())
        var indexWidth = 0
        kStrech = 1080/videoDuration
        //val bitmap = Bitmap.createBitmap(videoDuration,1920, Bitmap.Config.RGB_565)


        //bitmapToVideoEncoder.startEncoding(1080, 1920, mCurrentFile.absoluteFile)


        val frameEncoder = FrameEncoder(mEncoderConfig)
        frameEncoder.start()


        encode = Thread {
            Log.e("threead", "thread started ${Thread.currentThread().name}")
            if(typeCapturing==1) {
                while (iterator < 1080) {
                    if (outputFrames2[iterator] != null) {
                        //encoder.encodeImage(outputFrames2[iterator])
                        frameEncoder.createFrame(outputFrames2[iterator])
                        //bitmapToVideoEncoder.queueFrame(outputFrames2[iterator]!!)
                        //ffmpeg.record(outputFrames2[iterator])
                        Log.e("already  Frames", iterator.toString())
                        outputFrames2[iterator] = null
                        iterator++
                    }
                }
            }
            else{
                while (iterator < videoDuration-stride) {
                    if (outputFrames2[iterator] != null) {
                        //encoder.encodeImage(outputFrames2[iterator])
                        frameEncoder.createFrame(outputFrames2[iterator])
                        //bitmapToVideoEncoder.queueFrame(outputFrames2[iterator]!!)
                        //ffmpeg.record(outputFrames2[iterator])
                        Log.e("already  Frames", iterator.toString())
                        outputFrames2[iterator] = null
                        iterator++
                    }
                }
            }

            isRendered = true

            Log.e("threead", "thread diee")
        }
        encode.priority = Thread.MAX_PRIORITY
        encode.start()

        var idThread = 0
        val threads = Array<MyWorkerThread?>(8) { null }
        if(typeCapturing==1) {
            while (indexWidth < 1080) {
                if (iterator > indexWidth - 8) {
                    idThread = indexWidth % 8
                    if (threads[idThread] == null) {
                        threads[idThread] = MyWorkerThread("Thread #$idThread")
                        threads[idThread]!!.start()
                        threads[idThread]!!.prepareHandler()
                    }
                    threads[idThread]!!.postTask(makeFrame(indexWidth))

                    indexWidth++
                    //encoder.encodeImage(bitmap)
                    Log.e("Start Thread", indexWidth.toString())
                }
            }
        }
        else{
            var i =0
            while(i< videoDuration-stride){
                if (iterator > indexWidth - 8) {
                    idThread = indexWidth % 8
                    if (threads[idThread] == null) {
                        threads[idThread] = MyWorkerThread("Thread #$idThread")
                        threads[idThread]!!.start()
                        threads[idThread]!!.prepareHandler()
                    }
                    threads[idThread]!!.postTask(render2(i))

                    i++
                    //encoder.encodeImage(bitmap)
                    Log.e("Start Thread", indexWidth.toString())
                }
            }
        }

        var complite = false
        while(!complite) {
            if(isRendered) {
                //bitmapToVideoEncoder.stopEncoding()
                frameEncoder.release()
                encoder.finish()
                NIOUtils.closeQuietly(out)
                Log.e("path", mEncoderConfig.path.toUri().toString())
                watch()
                complite= true
            }
        }
    }

    private fun watch(){
        /*Log.e("frrrrame", outputFrames[12].toString())
        imageView.setImageBitmap(outputFrames[12])*/
        videoView.setVideoURI(mEncoderConfig.path.toUri()/*mCurrentFile.toUri()*/)
        videoView.start()
    }
    private fun initialize(){
        videoView = findViewById(R.id.videoView)
        //imageView = findViewById(R.id.imageView)
    }
}

class render2(private var numberFrame:Int):Runnable {
    override fun run() {
        val bitmap = Bitmap.createBitmap(1920,1080, Bitmap.Config.RGB_565)
        var index = 0
        while (index < height) {
            val pixels = IntArray(width) { 0 }
            frames[numberFrame+index/(height/stride)].getPixels(pixels, 0, width, 0, index, width-1, 1)
            bitmap.setPixels(pixels, 0, width, 0, index, width-1, 1)
            index++
        }
        outputFrames2[numberFrame] = bitmap
        Log.e("Finish Thread", numberFrame.toString())
    }
}

class makeFrame(private var indexWidth:Int):Runnable {
    override fun run() {
        val bitmap = Bitmap.createBitmap(1080,1920, Bitmap.Config.RGB_565)
        var index = 0
        while (index < videoDuration) {
            //bitmap[index, indexHeight] = frames[index][indexWidth,indexHeight]
            val pixels = IntArray(1920) { 0 }
            frames[index].getPixels(pixels, 0, 1, indexWidth, 0, 1, 1919)
            for(i in 0 until kStrech)
                bitmap.setPixels(pixels, 0, 1, index*kStrech+i, 0, 1, 1920)
            index++
        }
        outputFrames2[indexWidth] = bitmap
        Log.e("Finish Thread", indexWidth.toString())
    }
}

class MyWorkerThread(name: String) : HandlerThread(name) {

    private var mWorkerHandler: Handler? = null

    fun postTask(task: Runnable) {
        mWorkerHandler!!.post(task)
    }

    fun prepareHandler() {
        mWorkerHandler = Handler(looper)
    }
}
