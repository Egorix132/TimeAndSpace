package com.example.timespace

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import java.io.File
import java.util.*
import kotlin.properties.Delegates


var frames = arrayListOf<Bitmap>()
private var mCameraManager: CameraManager? = null
private val cam1 = 0
private val cam2 = 1
var numberFrame = 0
val mWidth = 1920
val mHeight = 1080
var isPixeled = false
var originalVideoDuration by Delegates.notNull<Long>()
var startTime by Delegates.notNull<Long>()
var readyFrames = 0
class CameraService(
    cameraManager: CameraManager,
    cameraID:String,
    private val activity: MainActivity,
    private val mImageView: TextureView,
    var mBackgroundHandler: android.os.Handler?
) {

    private val mCameraID:String
    private var mCameraDevice: CameraDevice? = null
    private lateinit var mSession: CameraCaptureSession
    lateinit var mMediaRecorder: MediaRecorder
    lateinit var mImageReader: ImageReader
    lateinit var mCurrentFile: File


    val isOpen:Boolean
        get() {
            return mCameraDevice != null
        }
    init{
        mCameraManager = cameraManager
        mCameraID = cameraID
    }
    fun openCamera() {
        try
        {
            if (checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            {
                //setUpMediaRecorder()
                mCameraManager!!.openCamera(mCameraID, mCameraCallback, mBackgroundHandler)
            }
        }
        catch (e: CameraAccessException) {
            Log.i("cammm", e.message!!)
        }
    }
    fun closeCamera() {
        if (mCameraDevice != null)
        {
            mCameraDevice!!.close()
            mCameraDevice = null
        }
    }


    private val mCameraCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            activity.curCam = mCameraDevice!!.id.toInt()
            Log.e("cameraCallBack", "Open camera  with id:" + mCameraDevice!!.id)

            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            mCameraDevice!!.close()

            Log.i("dissconecd", "disconnect camera  with id:" + mCameraDevice!!.id)
            mCameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.i("errrorr", "error! camera id:" + camera.id + " error:" + error)
        }
    }

    private fun createCameraPreviewSession() {
        val texture : SurfaceTexture = mImageView.surfaceTexture
        texture.setDefaultBufferSize(1920,1080)
        val surface = Surface(texture)

        try {
            val builder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            builder.addTarget(surface)
            mCameraDevice!!.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(session: CameraCaptureSession) {
                        mSession = session
                        try {
                            mSession.setRepeatingRequest(builder.build(), null, mBackgroundHandler)
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun startCameraRecordSession() {
        /*mImageReader = ImageReader.newInstance(mWidth,mHeight, PixelFormat.RGBA_8888,20)
        mImageReader.setOnImageAvailableListener(
            { reader ->
                val image = reader!!.acquireLatestImage()
                val thread = Thread(makeFrames(image, numberFrame))
                thread.start()
                numberFrame++
                if(numberFrame>401) {
                    activity.recording = false
                    stopRecordingVideo()
                }
            },
            mBackgroundHandler)
*/
        val texture : SurfaceTexture = mImageView.surfaceTexture
        texture.setDefaultBufferSize(1920,1080)
        val surface = Surface(texture)

        texture.setOnFrameAvailableListener {

        }
        //val recordSurface = mMediaRecorder.surface

        try {
            val builder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            builder.addTarget(surface)
            /*builder.addTarget(recordSurface)
            builder.addTarget(mImageReader.surface)*/

            mCameraDevice!!.createCaptureSession(
                listOf(surface/*, recordSurface, mImageReader.surface*/),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(session: CameraCaptureSession) {
                        mSession = session
                        try {
                            mSession.setRepeatingRequest(builder.build(), null, mBackgroundHandler)
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    fun startRecordingVideo(){
        numberFrame = 0
        startTime = Calendar.getInstance().time.time
        startCameraRecordSession()
        //mMediaRecorder.start()
    }
    fun stopRecordingVideo() {
        try {
            mSession.stopRepeating()
            mSession.abortCaptures()
            mSession.close()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        /*mMediaRecorder.stop()
        mMediaRecorder.release()*/

        originalVideoDuration =  Calendar.getInstance().time.time - startTime
        videoDuration = frames.size


        val watchVideo = Intent(activity, DistortedVideoActivity::class.java)
        watchVideo.putExtra("uri", mCurrentFile.toUri().toString())

        startActivity(activity, watchVideo, null)

        //setUpMediaRecorder()
    }

    /*private fun setUpMediaRecorder() {
        mMediaRecorder = MediaRecorder()
        mMediaRecorder.setOrientationHint(90)

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mCurrentFile = File(
            activity.getExternalFilesDir(Environment.DIRECTORY_DCIM),
            "test${Date().time}.mp4"
        )
        mMediaRecorder.setOutputFile(mCurrentFile.absolutePath)
        val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P)
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate)
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate)
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)

        try {
            mMediaRecorder.prepare()
            Log.e("staat", " запустили медиа рекордер")

        } catch (e: Exception) {
            Log.e("errrr", "не запустили медиа рекордер")
        }
    }*/
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

class makeFrames(private var image:Image, private val numberOfFrame:Int):Runnable {
    override fun run() {
        val planes = image.planes
        val buffer = planes[0].buffer
        /*val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * mWidth
        var offset = 0*/
        Log.e("frameMaked", numberOfFrame.toString())
        // create bitmap
        var bitmap = Bitmap.createBitmap(
            mWidth,
            mHeight,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        bitmap = bitmap.copy(Bitmap.Config.RGB_565, false)
        if(typeCapturing==1) {
            frames.add(bitmap.rotate(90f))
        }
        frames.add(bitmap)
        readyFrames++
        if(readyFrames == numberFrame-1)
            isPixeled = true
        image.close()
    }
}



