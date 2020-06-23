package com.example.timespace

import android.app.ActivityManager
import android.graphics.SurfaceTexture

interface IProcessMode {
    fun onSurfaceTextureUpdated(surface: SurfaceTexture?)

    fun compressFrames(frameNum : Int)

    fun remainsTimeAndFPS(memoryInfo: ActivityManager.MemoryInfo) : Pair<Int, Int>
}