package com.padil.stickly.ui.pack

import android.app.Application
import android.content.Intent
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PackOverviewViewModel(
    app: Application,
    val packId: String,
) : ViewModel() {

    private val stickLyApp = app as StickLyApplication
    private val repo = stickLyApp.repository

    val pack: StateFlow<StickerPack?> = repo.packs
        .map { list -> list.firstOrNull { it.id == packId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _whatsAppInstalled = MutableStateFlow(false)
    val whatsAppInstalled: StateFlow<Boolean> = _whatsAppInstalled.asStateFlow()

    private val _added = MutableStateFlow(false)
    val added: StateFlow<Boolean> = _added.asStateFlow()

    fun refreshStatus() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _whatsAppInstalled.value = WhatsAppApi.isWhatsAppInstalled(stickLyApp)
                _added.value = WhatsAppApi.isPackAdded(stickLyApp, stickLyApp.authority, packId)
            }
        }
    }

    fun addToWhatsAppIntent(): Intent? {
        val current = pack.value ?: return null
        return WhatsAppApi.buildAddPackIntent(stickLyApp, stickLyApp.authority, current.id, current.name)
    }

    fun trayBitmap(): Bitmap? = ImageUtils.decodeSampledFile(repo.trayFile(packId), 128)

    fun stickerBitmap(fileName: String): Bitmap? =
        ImageUtils.decodeSampledFile(repo.stickerFile(packId, fileName), 384)

    fun deleteSticker(stickerId: String) = repo.removeSticker(packId, stickerId)

    fun updateEmojis(stickerId: String, emojis: String) = repo.updateEmojis(packId, stickerId, emojis)

    fun renamePack(name: String, publisher: String): Result<Unit> = repo.renamePack(packId, name, publisher)

    fun deletePack() = repo.deletePack(packId)

    companion object {
        fun factory(app: Application, packId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { PackOverviewViewModel(app, packId) }
        }
    }
}