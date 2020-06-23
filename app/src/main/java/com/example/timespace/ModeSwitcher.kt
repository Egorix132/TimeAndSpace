package com.example.timespace

import android.view.TextureView

class ModeSwitcher(mTextureView: TextureView) {
    private val modeList = listOf(
        TimeToSpaceMode(mTextureView),
        RollingShutterMode(mTextureView))

    var currentModeId = 0
        private set

    fun nextMode(){
        currentModeId++
    }

    fun getMode() : IProcessMode{
        return modeList[currentModeId]
    }
}