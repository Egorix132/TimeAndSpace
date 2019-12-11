
import android.graphics.SurfaceTexture
import android.view.TextureView

open class MSurfaceTextureListener : TextureView.SurfaceTextureListener {
    var isAvailable = false
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        isAvailable = true
    }

}