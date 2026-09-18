package com.padil.stickly.ui.home

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.padil.stickly.StickLyApplication
import com.padil.stickly.data.StickerPack
import com.padil.stickly.util.ImageUtils
import com.padil.stickly.whatsapp.WhatsAppApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : ViewModel() {

    private val stickLyApp = app as StickLyApplication
    private val repo = stickLyApp.repository

    val packs: StateFlow<List<StickerPack>> = repo.packs

    private val _whatsAppInstalled = MutableStateFlow(false)
    val whatsAppInstalled: StateFlow<Boolean> = _whatsAppInstalled.asStateFlow()

    private val _added = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val added: StateFlow<Map<String, Boolean>> = _added.asStateFlow()

    private val _refreshing = MutableStateFlow(true)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            withContext(Dispatchers.IO) {
                _whatsAppInstalled.value = WhatsAppApi.isWhatsAppInstalled(stickLyApp)
                val result = mutableMapOf<String, Boolean>()
                repo.packsList().forEach { pack ->
                    result[pack.id] = WhatsAppApi.isPackAdded(stickLyApp, stickLyApp.authority, pack.id)
                }
                _added.value = result
            }
            _refreshing.value = false
        }
    }

    fun trayBitmap(packId: String): Bitmap? {
        val pack = repo.getPack(packId) ?: return null
        return ImageUtils.decodeSampledFile(repo.trayFile(packId), 128)
    }

    fun createPack(name: String, publisher: String): Result<StickerPack> =
        repo.createPack(name, publisher)

    fun renamePack(packId: String, name: String, publisher: String): Result<Unit> =
        repo.renamePack(packId, name, publisher)

    fun deletePack(packId: String) = repo.deletePack(packId)

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(app) }
        }
    }
}