package com.example.timespace

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.core.content.ContextCompat.checkSelfPermission


private var mCameraManager: CameraManager? = null

class CameraService(
    cameraManager: CameraManager,
    cameraID:String,
    private val activity: MainActivity,
    var mBackgroundHandler: android.os.Handler?
) {

    private val mCameraID:String
    private var mCameraDevice: CameraDevice? = null
    private lateinit var mSession: CameraCaptureSession
    private lateinit var mTextureView: TextureView

    val isOpen:Boolean
        get() {
            return mCameraDevice != null
        }
    init{
        mCameraManager = cameraManager
        mCameraID = cameraID
    }
    fun openCamera(mTextureView: TextureView) {
        try
        {
            if (checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            {
                mCameraManager!!.openCamera(mCameraID, mCameraCallback, mBackgroundHandler)
                this.mTextureView = mTextureView
            }
        }
        catch (e: CameraAccessException) {
            Log.i("Can't open camera", e.message!!)
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
            Log.e("On camera opened", "Open camera  with id:" + mCameraDevice!!.id)

            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            mCameraDevice!!.close()
            Log.i("On camera disconnected", "disconnect camera  with id:" + mCameraDevice!!.id)
            mCameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.i("On camera Error", "error! camera id:" + camera.id + " error:" + error)
        }
    }

    private fun createCameraPreviewSession() {
        val texture : SurfaceTexture = mTextureView.surfaceTexture
        Log.e("FPS", "${CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE} $highSpeed ${maxFPS.upper} ${maxFPS.lower}")

        try {
            texture.setDefaultBufferSize(1920,1080)
            val surface = Surface(texture)
            val builder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            builder.addTarget(surface)
            mCameraDevice!!.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(session: CameraCaptureSession) {
                        mSession = session
                        try {
                            mSession.setRepeatingRequest(
                                builder.build(),
                                null,
                                mBackgroundHandler
                            )
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
}
