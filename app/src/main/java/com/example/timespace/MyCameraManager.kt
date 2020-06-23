package com.example.timespace

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class MyCameraManager (private val camManager : CameraManager) {
    private val myCameras: MutableList<CameraService> = mutableListOf()

    public fun PrepareCameras(){
        try {
            for (camId in camManager.cameraIdList) {
                // Получение списка камер с устройства
                val id = Integer.parseInt(camId)

                // создаем обработчик для камеры
                //myCameras.add(id, CameraService(camManager, camId, this, mImageView, mBackgroundHandler))
                val char = camManager.getCameraCharacteristics(camId)
                val config = char.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                config!!.highSpeedVideoFpsRanges.forEach {
                    if(it.upper> maxFPS.upper) maxFPS = it
                }
                if(maxFPS.upper>60){
                    highSpeed = true
                }
                //Log.e("addcam", myCameras[cam1].toString())
            }
        } catch (e: CameraAccessException){
            e.printStackTrace()
        }
        //myCameras.add(0, CameraService(camManager, "0", this, mImageView, mBackgroundHandler))
    }
}