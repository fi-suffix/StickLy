package com.padil.stickly.sticker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import java.io.ByteArrayOutputStream

object StickerCodec {

    const val STICKER_SIZE = 512
    const val STICKER_MAX_BYTES = 100 * 1024
    const val TRAY_SIZE = 96
    const val TRAY_MAX_BYTES = 50 * 1024

    fun encodeSticker(project: StickerProject): ByteArray {
        val bitmap = StickerRenderer.render(project)
        return encodeStickerBitmap(bitmap)
    }

    fun encodeStickerBitmap(bitmap: Bitmap): ByteArray {
        val lossless = compressWebP(bitmap, lossless = true, quality = 100)
        if (lossless.size <= STICKER_MAX_BYTES) return lossless

        var quality = 92
        var out = compressWebP(bitmap, lossless = false, quality = quality)
        while (out.size > STICKER_MAX_BYTES && quality > 45) {
            quality -= 6
            out = compressWebP(bitmap, lossless = false, quality = quality)
        }
        return out
    }

    fun compressWebP(bitmap: Bitmap, lossless: Boolean, quality: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (lossless) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        bitmap.compress(format, quality, out)
        return out.toByteArray()
    }

    fun trayBytesFromSticker(stickerBytes: ByteArray): ByteArray {
        val decoded = BitmapFactory.decodeByteArray(stickerBytes, 0, stickerBytes.size)
            ?: return defaultTray("StickLy")
        return trayBytesFromBitmap(decoded)
    }

    fun trayBytesFromBitmap(source: Bitmap): ByteArray {
        val scaled = centerFit(source, TRAY_SIZE)
        var out = compressWebP(scaled, lossless = true, quality = 100)
        if (out.size > TRAY_MAX_BYTES) out = compressWebP(scaled, lossless = false, quality = 85)
        if (out.size > TRAY_MAX_BYTES) out = compressWebP(scaled, lossless = false, quality = 70)
        return out
    }

    fun defaultTray(packName: String): ByteArray {
        val bmp = Bitmap.createBitmap(TRAY_SIZE, TRAY_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val rect = RectF(2f, 2f, TRAY_SIZE - 2f, TRAY_SIZE - 2f)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B4EFF") }
        canvas.drawRoundRect(rect, 22f, 22f, bg)

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val letter = packName.trim().firstOrNull()?.uppercase() ?: "S"
        val metrics = text.fontMetrics
        val y = (TRAY_SIZE - (metrics.ascent + metrics.descent)) / 2f
        canvas.drawText(letter, TRAY_SIZE / 2f, y, text)
        return compressWebP(bmp, lossless = true, quality = 100)
    }

    private fun centerFit(source: Bitmap, size: Int): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val s = minOf(size.toFloat() / source.width, size.toFloat() / source.height)
        val w = source.width * s
        val h = source.height * s
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, null, RectF((size - w) / 2f, (size - h) / 2f, (size + w) / 2f, (size + h) / 2f), p)
        return out
    }
}