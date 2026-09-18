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
        return when {
            segments.isEmpty() -> MatrixCursor(EMPTY_COLUMNS)
            segments[0] == "metadata" && segments.size == 1 -> allPacksCursor()
            segments[0] == "metadata" && segments.size == 2 -> packCursor(segments[1])
            segments[0] == "stickers" && segments.size == 2 -> stickersCursor(segments[1])
            else -> MatrixCursor(EMPTY_COLUMNS)
        }
    }

    private fun allPacksCursor(): Cursor {
        val cursor = MatrixCursor(METADATA_COLUMNS)
        repository.packsList().forEachIndexed { index, pack ->
            cursor.newRow()
                .add("_id", index)
                .add("identifier", pack.id)
                .add("name", pack.name)
                .add("publisher", pack.publisher)
                .add("tray_image_file", pack.trayImage)
                .add("image_data_version", pack.dataVersion.toLong())
                .add("avoid_cache", 0)
                .add("android_play_store_link", "")
                .add("ios_app_store_link", "")
                .add("publisher_email", "")
                .add("publisher_website", "")
                .add("privacy_policy_website", "")
                .add("license_agreement_website", "")
        }
        return cursor
    }

    private fun packCursor(identifier: String): Cursor {
        val cursor = MatrixCursor(METADATA_COLUMNS)
        val pack = repository.getPack(identifier)
        if (pack != null) {
            cursor.newRow()
                .add("_id", 0)
                .add("identifier", pack.id)
                .add("name", pack.name)
                .add("publisher", pack.publisher)
                .add("tray_image_file", pack.trayImage)
                .add("image_data_version", pack.dataVersion.toLong())
                .add("avoid_cache", 0)
                .add("android_play_store_link", "")
                .add("ios_app_store_link", "")
                .add("publisher_email", "")
                .add("publisher_website", "")
                .add("privacy_policy_website", "")
                .add("license_agreement_website", "")
        }
        return cursor
    }

    private fun stickersCursor(identifier: String): Cursor {
        val cursor = MatrixCursor(STICKER_COLUMNS)
        val pack = repository.getPack(identifier) ?: return cursor
        pack.stickers.forEachIndexed { index, sticker ->
            cursor.newRow()
                .add("_id", index)
                .add("pack_id", pack.id)
                .add("image_file", sticker.fileName)
                .add("emojis", sticker.emojis)
                .add("accessibility_text", "")
        }
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val segments = uri.pathSegments
        if (segments.size < 3 || segments[0] != "stickers_asset") return null
        val packId = segments[1]
        val fileName = segments.drop(2).joinToString("/")
        val file = repository.assetFile(packId, fileName)
        if (!file.exists()) return null
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
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
        private val EMPTY_COLUMNS = arrayOf("_id")

        private val METADATA_COLUMNS = arrayOf(
            "_id",
            "identifier",
            "name",
            "publisher",
            "tray_image_file",
            "image_data_version",
            "avoid_cache",
            "android_play_store_link",
            "ios_app_store_link",
            "publisher_email",
            "publisher_website",
            "privacy_policy_website",
            "license_agreement_website",
        )

        private val STICKER_COLUMNS = arrayOf(
            "_id",
            "pack_id",
            "image_file",
            "emojis",
            "accessibility_text",
        )
    }
}