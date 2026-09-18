package com.padil.stickly

import android.app.Application
import com.padil.stickly.data.StickerRepository

class StickLyApplication : Application() {

    val repository: StickerRepository by lazy {
        StickerRepository(this)
    }

    val authority: String
        get() = "$packageName.stickercontentprovider"
}