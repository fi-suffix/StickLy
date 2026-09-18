package com.padil.stickly.sticker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

object StickerRenderer {

    const val SIZE = 512

    fun render(project: StickerProject): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (project.fillWhiteBackground) {
            canvas.drawColor(Color.WHITE)
        }
        try {
            project.source?.let { drawSource(canvas, it, project.sourceScale, project.sourceRotation) }
            project.layers.forEach { drawLayer(canvas, it) }
        } catch (e: Exception) {
            canvas.drawColor(project.fillWhiteBackground.let { if (it) Color.WHITE else Color.TRANSPARENT })
        }
        return bitmap
    }

    private fun drawSource(canvas: Canvas, source: Bitmap, scaleFactor: Float, rotationDeg: Float) {
        val srcW = source.width.coerceAtLeast(1)
        val srcH = source.height.coerceAtLeast(1)
        val fitScale = minOf(SIZE.toFloat() / srcW, SIZE.toFloat() / srcH)
        val w = srcW * fitScale
        val h = srcH * fitScale
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.save()
        canvas.translate(SIZE / 2f, SIZE / 2f)
        canvas.rotate(rotationDeg)
        canvas.scale(scaleFactor, scaleFactor)
        canvas.drawBitmap(source, null, RectF(-w / 2f, -h / 2f, w / 2f, h / 2f), paint)
        canvas.restore()
    }

    private fun drawLayer(canvas: Canvas, layer: StickerTextLayer) {
        if (layer.text.isBlank()) return
        val alpha = (layer.alpha.coerceIn(0f, 1f) * 255).toInt()
        val basePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (alpha shl 24) or (layer.colorArgb and 0xFFFFFF)
            textSize = layer.fontSize
            typeface = typefaceFor(layer.fontFamily, layer.fontStyle)
        }
        val paints = ArrayList<TextPaint>(2)
        if (layer.outlineEnabled) {
            paints.add(
                TextPaint(basePaint).apply {
                    color = (alpha shl 24) or (layer.outlineColorArgb and 0xFFFFFF)
                    style = Paint.Style.STROKE
                    strokeWidth = layer.outlineWidth
                    strokeJoin = Paint.Join.ROUND
                },
            )
        }
        paints.add(basePaint)
        val layouts = paints.map { paint ->
            StaticLayout.Builder
                .obtain(layer.text, 0, layer.text.length, paint, SIZE)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(12)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
        }
        val width = layouts.maxOf { it.width }
        val height = layouts.maxOf { it.height }
        canvas.save()
        canvas.translate(layer.anchorX, layer.anchorY)
        canvas.rotate(layer.rotationDeg)
        canvas.translate(-width / 2f, -height / 2f)
        layouts.forEach { it.draw(canvas) }
        canvas.restore()
    }

    private fun typefaceFor(family: String, style: String): Typeface {
        val familyName = when (family) {
            StickerTextLayer.FONT_SERIF -> "serif"
            StickerTextLayer.FONT_MONO -> "monospace"
            StickerTextLayer.FONT_CURSIVE -> "cursive"
            else -> "sans-serif"
        }
        val styleType = when (style) {
            StickerTextLayer.STYLE_NORMAL -> Typeface.NORMAL
            StickerTextLayer.STYLE_ITALIC -> Typeface.ITALIC
            StickerTextLayer.STYLE_BOLD_ITALIC -> Typeface.BOLD_ITALIC
            else -> Typeface.BOLD
        }
        return Typeface.create(familyName, styleType)
    }
}