package com.example.timespace

import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView
import android.view.View
import androidx.core.content.ContextCompat

class VideoManager(private val mTextureView: TextureView, private val context: Context) {
    var recording = false
        private set
    private val modeSwitcher = ModeSwitcher(mTextureView)

    fun startCapture(){
        recording = true
        val currentMode = modeSwitcher.getMode()

        mTextureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
                currentMode.onSurfaceTextureUpdated(surface)
                capturedFrames++
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {}
        }

        Thread {
            var i = 0
            while (recording || numberOfCompressedFrames < capturedFrames - 1) {
                if (capturedFrames > i) {

                    threads[i % 4]!!.postTask(object : Runnable {
                        var frameNum = i
                        override fun run() {
                            currentMode.compressFrames(frameNum)
                        }})
                    i++
                }
            }
            AllFramesIsCompressed = true
        }.start()
    }

    fun stopCapture(){
        recording = false
        mTextureView.surfaceTextureListener = null
        val watchVideo = Intent(context, DistortedVideoActivity::class.java)
        ContextCompat.startActivity(context, watchVideo, null)
    }
}