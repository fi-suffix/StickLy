package com.padil.stickly.ui.editor

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.padil.stickly.StickLyApplication
import com.padil.stickly.data.Sticker
import com.padil.stickly.data.StickerPack
import com.padil.stickly.sticker.StickerCodec
import com.padil.stickly.sticker.StickerProject
import com.padil.stickly.sticker.StickerTextLayer
import com.padil.stickly.sticker.StickerValidator
import com.padil.stickly.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(
    private val app: Application,
    private val targetPackId: String?,
) : ViewModel() {

    private val repo = (app as StickLyApplication).repository

    val packs: StateFlow<List<StickerPack>> = repo.packs

    private val _project = MutableStateFlow(StickerProject())
    val project: StateFlow<StickerProject> = _project.asStateFlow()

    private val _selectedLayerId = MutableStateFlow<Long?>(null)
    val selectedLayerId: StateFlow<Long?> = _selectedLayerId.asStateFlow()

    private val _loadingSource = MutableStateFlow(false)
    val loadingSource: StateFlow<Boolean> = _loadingSource.asStateFlow()

    private var nextLayerId = 1L

    val hasContent: Boolean
        get() = _project.value.source != null || _project.value.layers.isNotEmpty()

    fun setSource(bitmap: Bitmap?) {
        _project.value = _project.value.copy(source = bitmap, sourceScale = 1f, sourceRotation = 0f)
    }

    fun loadSource(uri: Uri) {
        viewModelScope.launch {
            _loadingSource.value = true
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    ImageUtils.decodeSampledUri(app, uri)
                } catch (_: Exception) {
                    null
                }
            }
            _loadingSource.value = false
            if (bitmap != null) {
                _project.value = _project.value.copy(source = bitmap, sourceScale = 1f, sourceRotation = 0f)
            }
        }
    }

    fun transformSource(zoom: Float, rotation: Float) {
        if (zoom == 1f && rotation == 0f) return
        val p = _project.value
        val newScale = (p.sourceScale * zoom).coerceIn(0.2f, 6f)
        _project.value = p.copy(
            sourceScale = newScale,
            sourceRotation = (((p.sourceRotation + rotation) % 360f) + 360f) % 360f,
        )
    }

    fun resetSourceTransform() {
        if (_project.value.sourceScale != 1f || _project.value.sourceRotation != 0f) {
            _project.value = _project.value.copy(sourceScale = 1f, sourceRotation = 0f)
        }
    }

    fun setFillWhite(value: Boolean) {
        _project.value = _project.value.copy(fillWhiteBackground = value)
    }

    fun addTextLayer() {
        val layer = StickerTextLayer(id = nextLayerId++)
        _project.value = _project.value.copy(layers = _project.value.layers + layer)
        _selectedLayerId.value = layer.id
    }

    fun selectLayer(id: Long?) {
        _selectedLayerId.value = id
    }

    fun deleteLayer(id: Long) {
        _project.value = _project.value.copy(layers = _project.value.layers.filterNot { it.id == id })
        if (_selectedLayerId.value == id) {
            _selectedLayerId.value = _project.value.layers.lastOrNull()?.id
        }
    }

    fun updateLayer(id: Long, transform: (StickerTextLayer) -> StickerTextLayer) {
        _project.value = _project.value.copy(
            layers = _project.value.layers.map { if (it.id == id) transform(it) else it },
        )
    }

    fun clearProject() {
        _project.value = StickerProject()
        _selectedLayerId.value = null
    }

    fun isTargetPack(packId: String): Boolean = targetPackId == packId

    fun saveSticker(
        emojis: String,
        targetPackId: String?,
        newPackName: String?,
        newPublisher: String?,
        onResult: (Result<Pair<Sticker, String>>) -> Unit,
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val project = _project.value
                val bytes = StickerCodec.encodeSticker(project)
                val validation = StickerValidator.validate(bytes, requireStickerDims = true)
                if (!validation.ok) {
                    return@withContext Result.failure<Pair<Sticker, String>>(
                        IllegalStateException("Stiker gagal dibuat: ${validation.message}"),
                    )
                }
                val resolvedPackId = targetPackId ?: run {
                    val created = repo.createPack(newPackName.orEmpty(), newPublisher.orEmpty())
                        .getOrElse { return@withContext Result.failure(it) }
                    created.id
                }
                repo.addSticker(resolvedPackId, bytes, emojis).map { it to resolvedPackId }
            }
            if (result.isSuccess) {
                clearProject()
            }
            onResult(result)
        }
    }

    companion object {
        fun factory(app: Application, packId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer { EditorViewModel(app, packId) }
        }
    }
}