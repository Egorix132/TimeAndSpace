package com.example.timespace

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.TextView

class RecordTimer(private val timerText : TextView, private val fpsText : TextView, private val context: Context) {
    fun startTimer(modeSwitcher: ModeSwitcher, videoManager: VideoManager) {
        val handler = Handler()
        val activityManager =  context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val  memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val handlerTask = object : Runnable {
            override fun run() {
                val timeAndFPS = modeSwitcher.getMode().remainsTimeAndFPS(memoryInfo)

                timerText.text = timeAndFPS.first.toString()
                fpsText.text = "FPS : ${timeAndFPS.second}"

                activityManager.getMemoryInfo(memoryInfo)
                Log.e("memory","avM: ${memoryInfo.availMem} treshM: ${memoryInfo.threshold} stop?: ${memoryInfo.availMem-200000000<memoryInfo.threshold} total: ${memoryInfo.totalMem}")
                if((memoryInfo.availMem-200000000<memoryInfo.threshold || numberOfCompressedFrames+35>numOfStreams) && recording){
                    videoManager.stopCapture()
                }
                else
                    handler.postDelayed(this, 1000)
            }

        }
        handlerTask.run()
    }
}