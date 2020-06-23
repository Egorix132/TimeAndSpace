package com.example.timespace

import android.app.ActivityManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView

class RollingShutterMode(private val mTextureView: TextureView) : IProcessMode {
    private var lastFrame = 0

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        inputBitmaps[capturedFrames % 200] = mTextureView.getBitmap(1080, 1920)
    }

    override fun compressFrames(frameNum : Int){
        inputBitmaps[frameNum % 200]!!.compress(Bitmap.CompressFormat.JPEG,70, compressedBitmaps[frameNum])
        inputBitmaps[frameNum % 200] = null
        numberOfCompressedFrames++
        Log.e("compres","$numberOfCompressedFrames $capturedFrames")
    }

    override fun remainsTimeAndFPS(memoryInfo: ActivityManager.MemoryInfo): Pair<Int, Int> {
        val ret = Pair((numOfStreams - numberOfCompressedFrames) / (1 + capturedFrames - lastFrame), capturedFrames - lastFrame)
        lastFrame = capturedFrames
        return ret
    }
}