package com.example.timespace

import android.app.ActivityManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView

class TimeToSpaceMode (private val mTextureView: TextureView) : IProcessMode {
    private var lastFrame = 0

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        inputFrames.add(mTextureView.getBitmap(1080, 1920))
    }

    override fun compressFrames(frameNum : Int){
        Log.e("what", "${frameNum}")
        inputFrames[frameNum] = inputFrames[frameNum]!!.copy(Bitmap.Config.RGB_565, false)
        numberOfCompressedFrames++
    }

    override fun remainsTimeAndFPS(memoryInfo : ActivityManager.MemoryInfo): Pair<Int, Int> {
        val ret = Pair((memoryInfo.availMem - 200000000 - memoryInfo.threshold).toInt() / (1920 * 1080 * 2 * (1 + inputFrames.size - lastFrame)), capturedFrames - lastFrame)
        lastFrame = capturedFrames
        return ret
    }


}