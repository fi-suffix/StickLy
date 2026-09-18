package com.padil.stickly.data

import android.content.Context
import com.padil.stickly.sticker.StickerCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class PersistedSticker(
    val fileName: String,
    val emojis: String = "",
)

@Serializable
data class PersistedPack(
    val id: String,
    val name: String,
    val publisher: String,
    val trayImage: String = "tray.webp",
    val dataVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val stickers: List<PersistedSticker> = emptyList(),
)

@Serializable
data class PersistedStore(
    val packs: List<PersistedPack> = emptyList(),
)

data class StickerPack(
    val id: String,
    val name: String,
    val publisher: String,
    val trayImage: String,
    val dataVersion: Int,
    val createdAt: Long,
    val stickers: List<Sticker>,
)

data class Sticker(
    val id: String,
    val fileName: String,
    val emojis: String,
)

class StickerRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val storeFile: File get() = File(context.filesDir, "stickly_store.json")
    private val stickersRoot: File get() = File(context.filesDir, "stickers")

    private val _store = MutableStateFlow(PersistedStore())
    private val _packs = MutableStateFlow<List<StickerPack>>(emptyList())

    val store: StateFlow<PersistedStore> = _store.asStateFlow()
    val packs: StateFlow<List<StickerPack>> = _packs.asStateFlow()

    init {
        load()
    }

    private fun load() {
        try {
            if (storeFile.exists()) {
                _store.value = json.decodeFromString(PersistedStore.serializer(), storeFile.readText())
            }
        } catch (_: Exception) {
            _store.value = PersistedStore()
        }
        _packs.value = _store.value.packs.map { it.toUi() }
    }

    fun getPack(packId: String): StickerPack? = _packs.value.firstOrNull { it.id == packId }

    fun packsList(): List<StickerPack> = _packs.value

    fun createPack(name: String, publisher: String): Result<StickerPack> {
        val trimmedName = name.trim()
        val trimmedPublisher = publisher.trim()
        if (trimmedName.length < 3) {
            return Result.failure(IllegalArgumentException("Nama paket minimal 3 karakter"))
        }
        if (trimmedPublisher.length < 3) {
            return Result.failure(IllegalArgumentException("Nama publisher minimal 3 karakter"))
        }

        val id = UUID.randomUUID().toString()
        val persisted = PersistedPack(
            id = id,
            name = trimmedName,
            publisher = trimmedPublisher,
            trayImage = "tray.webp",
            dataVersion = 1,
        )
        mutate { packs -> packs + persisted }
        writeTrayFile(id, StickerCodec.defaultTray(trimmedName))
        return Result.success(persisted.toUi())
    }

    fun renamePack(packId: String, name: String, publisher: String): Result<Unit> {
        val trimmedName = name.trim()
        val trimmedPublisher = publisher.trim()
        if (trimmedName.isBlank() || trimmedPublisher.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama tidak boleh kosong"))
        }
        mutate { packs ->
            packs.map {
                if (it.id == packId) {
                    it.copy(name = trimmedName, publisher = trimmedPublisher, dataVersion = it.dataVersion + 1)
                } else it
            }
        }
        return Result.success(Unit)
    }

    fun deletePack(packId: String) {
        mutate { packs -> packs.filterNot { it.id == packId } }
        stickersRoot.resolve(packId).deleteRecursively()
    }

    fun addSticker(packId: String, webpBytes: ByteArray, emojis: String): Result<Sticker> {
        val current = _store.value.packs.firstOrNull { it.id == packId }
            ?: return Result.failure(IllegalArgumentException("Paket tidak ditemukan"))
        if (current.stickers.size >= 30) {
            return Result.failure(IllegalArgumentException("Paket maksimal berisi 30 stiker"))
        }

        val id = UUID.randomUUID().toString()
        val fileName = "$id.webp"
        writeStickerFile(packId, fileName, webpBytes)

        if (current.stickers.isEmpty()) {
            writeTrayFile(packId, StickerCodec.trayBytesFromSticker(webpBytes))
        }

        mutate { packs ->
            packs.map {
                if (it.id == packId) {
                    it.copy(
                        stickers = it.stickers + PersistedSticker(fileName, emojis),
                        dataVersion = it.dataVersion + 1,
                    )
                } else it
            }
        }
        return Result.success(Sticker(id = id, fileName = fileName, emojis = emojis))
    }

    fun removeSticker(packId: String, stickerId: String) {
        val current = _store.value.packs.firstOrNull { it.id == packId } ?: return
        val target = current.stickers.firstOrNull { it.fileName.removeSuffix(".webp") == stickerId }

        mutate { packs ->
            packs.map {
                if (it.id == packId) {
                    it.copy(
                        stickers = it.stickers.filterNot { s -> s.fileName.removeSuffix(".webp") == stickerId },
                        dataVersion = it.dataVersion + 1,
                    )
                } else it
            }
        }
        target?.let { File(stickersRoot.resolve(packId), it.fileName).delete() }
        if (current.stickers.size == 1) {
            writeTrayFile(packId, StickerCodec.defaultTray(current.name))
        }
    }

    fun updateEmojis(packId: String, stickerId: String, emojis: String) {
        mutate { packs ->
            packs.map {
                if (it.id == packId) {
                    it.copy(
                        stickers = it.stickers.map { s ->
                            if (s.fileName.removeSuffix(".webp") == stickerId) s.copy(emojis = emojis) else s
                        },
                        dataVersion = it.dataVersion + 1,
                    )
                } else it
            }
        }
    }

    fun stickerFile(packId: String, fileName: String): File =
        File(stickersRoot.resolve(packId), fileName)

    fun trayFile(packId: String): File = File(stickersRoot.resolve(packId), "tray.webp")

    fun assetFile(packId: String, fileName: String): File {
        val safe = fileName.replace("..", "").replace("\\", "_").replace("/", "_")
        return File(stickersRoot.resolve(packId), safe)
    }

    fun writeStickerFile(packId: String, fileName: String, bytes: ByteArray) {
        val dir = stickersRoot.resolve(packId).apply { mkdirs() }
        File(dir, fileName).writeBytes(bytes)
    }

    fun writeTrayFile(packId: String, bytes: ByteArray) {
        val dir = stickersRoot.resolve(packId).apply { mkdirs() }
        File(dir, "tray.webp").writeBytes(bytes)
    }

    private fun mutate(transform: (List<PersistedPack>) -> List<PersistedPack>) {
        val updated = transform(_store.value.packs)
        _store.value = _store.value.copy(packs = updated)
        _packs.value = updated.map { it.toUi() }
        persist()
    }

    private fun persist() {
        try {
            storeFile.writeText(json.encodeToString(PersistedStore.serializer(), _store.value))
        } catch (_: Exception) {
        }
    }

    private fun PersistedPack.toUi(): StickerPack = StickerPack(
        id = id,
        name = name,
        publisher = publisher,
        trayImage = trayImage,
        dataVersion = dataVersion,
        createdAt = createdAt,
        stickers = stickers.map {
            Sticker(
                id = it.fileName.removeSuffix(".webp"),
                fileName = it.fileName,
                emojis = it.emojis,
            )
        },
    )
}