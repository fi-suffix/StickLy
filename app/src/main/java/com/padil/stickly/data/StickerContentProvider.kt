package com.padil.stickly.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.padil.stickly.StickLyApplication

class StickerContentProvider : ContentProvider() {

    private lateinit var repository: StickerRepository

    override fun onCreate(): Boolean {
        val app = context?.applicationContext as? StickLyApplication ?: return false
        repository = app.repository
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val segments = uri.pathSegments
        return try {
            when {
                segments.isEmpty() -> MatrixCursor(EMPTY_COLUMNS)
                segments[0] == "metadata" && segments.size == 1 -> allPacksCursor()
                segments[0] == "metadata" && segments.size == 2 -> packCursor(segments[1])
                segments[0] == "stickers" && segments.size == 2 -> stickersCursor(segments[1])
                else -> MatrixCursor(EMPTY_COLUMNS)
            }
        } catch (e: Exception) {
            MatrixCursor(EMPTY_COLUMNS)
        }
    }

    private fun allPacksCursor(): Cursor {
        val cursor = MatrixCursor(METADATA_COLUMNS)
        repository.packsList().forEachIndexed { index, pack ->
            fillMetadataRow(cursor.newRow(), pack, index)
        }
        return cursor
    }

    private fun packCursor(identifier: String): Cursor {
        val cursor = MatrixCursor(METADATA_COLUMNS)
        val pack = repository.getPack(identifier)
        if (pack != null) {
            fillMetadataRow(cursor.newRow(), pack, 0)
        }
        return cursor
    }

    private fun fillMetadataRow(row: MatrixCursor.RowBuilder, pack: StickerPack, index: Int) {
        row.add(pack.id)
            .add(pack.name)
            .add(pack.publisher)
            .add(pack.trayImage)
            .add("")
            .add("")
            .add("")
            .add("")
            .add("")
            .add("")
            .add(pack.dataVersion.toLong())
            .add(0)
            .add(0)
            .add(index)
            .add(pack.id)
            .add(pack.name)
            .add(pack.publisher)
            .add(pack.trayImage)
            .add(0)
            .add("")
            .add("")
            .add("")
            .add("")
    }

    private fun stickersCursor(identifier: String): Cursor {
        val cursor = MatrixCursor(STICKER_COLUMNS)
        val pack = repository.getPack(identifier) ?: return cursor
        pack.stickers.forEachIndexed { index, sticker ->
            val emojis = sticker.emojis.ifBlank { FALLBACK_EMOJI }
            val singleEmoji = emojis.split("\\s+".toRegex()).firstOrNull { it.isNotBlank() } ?: FALLBACK_EMOJI
            cursor.newRow()
                .add(sticker.fileName)
                .add(singleEmoji)
                .add("")
                .add(0)
                .add(index)
                .add(pack.id)
                .add(sticker.fileName)
                .add(singleEmoji)
                .add("")
        }
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return try {
            val segments = uri.pathSegments
            if (segments.size < 3 || segments[0] != "stickers_asset") {
                return null
            }
            val packId = segments[1]
            val fileName = segments.drop(2).joinToString("/")
            val file = repository.assetFile(packId, fileName)
            if (!file.exists()) {
                return null
            }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (e: Exception) {
            null
        }
    }

    override fun getType(uri: Uri): String? = when (uri.pathSegments.firstOrNull()) {
        "metadata" -> "vnd.android.cursor.dir/vnd.stickly.metadata"
        "stickers" -> "vnd.android.cursor.dir/vnd.stickly.stickers"
        "stickers_asset" -> "image/webp"
        else -> "vnd.android.cursor.item/vnd.stickly.metadata"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        private const val FALLBACK_EMOJI = "😊"

        private val EMPTY_COLUMNS = arrayOf("_id")

        private val METADATA_OFFICIAL = arrayOf(
            "sticker_pack_identifier",
            "sticker_pack_name",
            "sticker_pack_publisher",
            "sticker_pack_icon",
            "android_play_store_link",
            "ios_app_download_link",
            "sticker_pack_publisher_email",
            "sticker_pack_publisher_website",
            "sticker_pack_privacy_policy_website",
            "sticker_pack_license_agreement_website",
            "image_data_version",
            "whatsapp_will_not_cache_stickers",
            "animated_sticker_pack",
        )

        private val METADATA_LEGACY = arrayOf(
            "_id",
            "identifier",
            "name",
            "publisher",
            "tray_image_file",
            "avoid_cache",
            "publisher_email",
            "publisher_website",
            "privacy_policy_website",
            "license_agreement_website",
        )

        private val METADATA_COLUMNS = METADATA_OFFICIAL + METADATA_LEGACY

        private val STICKER_OFFICIAL = arrayOf(
            "sticker_file_name",
            "sticker_emoji",
            "sticker_accessibility_text",
            "sticker_file_animated",
        )

        private val STICKER_LEGACY = arrayOf(
            "_id",
            "pack_id",
            "image_file",
            "emojis",
            "accessibility_text",
        )

        private val STICKER_COLUMNS = STICKER_OFFICIAL + STICKER_LEGACY
    }
}