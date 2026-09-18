package com.padil.stickly.sticker

import android.graphics.BitmapFactory

data class ValidationResult(
    val ok: Boolean,
    val isWebP: Boolean,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Int = 0,
    val message: String = "",
)

object StickerValidator {

    fun validate(bytes: ByteArray, requireStickerDims: Boolean = false): ValidationResult {
        val size = bytes.size
        val webp = isWebP(bytes)

        var width = 0
        var height = 0
        if (webp) {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            width = opts.outWidth
            height = opts.outHeight
        }

        val dimOk = width == StickerCodec.STICKER_SIZE && height == StickerCodec.STICKER_SIZE
        val sizeOk = size <= StickerCodec.STICKER_MAX_BYTES
        val ok = webp && dimOk && sizeOk

        val problems = buildList {
            if (!webp) add("bukan WebP")
            if (requireStickerDims && !dimOk) add("dimensi harus 512x512 (sekarang ${width}x$height)")
            if (requireStickerDims && !sizeOk) add("ukuran $size byte > 100KB")
        }

        return ValidationResult(
            ok = ok,
            isWebP = webp,
            width = width,
            height = height,
            sizeBytes = size,
            message = problems.joinToString(", "),
        )
    }

    fun isWebP(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val riffOk = bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()
        val webpOk = bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        return riffOk && webpOk
    }
}