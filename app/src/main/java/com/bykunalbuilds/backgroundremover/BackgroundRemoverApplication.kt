package com.bykunalbuilds.backgroundremover

import android.app.Application
import com.bykunalbuilds.backgroundremover.data.ImageDecoder
import com.bykunalbuilds.backgroundremover.data.ImageExporter
import com.bykunalbuilds.backgroundremover.inference.OnnxBackgroundRemover

class BackgroundRemoverApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val imageDecoder = ImageDecoder(application)
    val backgroundRemover = OnnxBackgroundRemover(application)
    val imageExporter = ImageExporter(application)
}
