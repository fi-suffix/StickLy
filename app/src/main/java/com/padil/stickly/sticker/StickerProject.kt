package com.padil.stickly.sticker

import android.graphics.Bitmap
import android.graphics.Color

data class StickerTextLayer(
    val id: Long,
    val text: String = "Halo!",
    val colorArgb: Int = Color.WHITE,
    val fontSize: Float = 96f,
    val rotationDeg: Float = 0f,
    val alpha: Float = 1f,
    val fontFamily: String = FONT_SANS,
    val fontStyle: String = STYLE_BOLD,
    val outlineEnabled: Boolean = false,
    val outlineColorArgb: Int = Color.BLACK,
    val outlineWidth: Float = 8f,
    val anchorX: Float = StickerRenderer.SIZE / 2f,
    val anchorY: Float = StickerRenderer.SIZE / 2f,
) {
    companion object {
        const val FONT_SANS = "sans"
        const val FONT_SERIF = "serif"
        const val FONT_MONO = "monospace"
        const val FONT_CURSIVE = "cursive"

        const val STYLE_NORMAL = "normal"
        const val STYLE_BOLD = "bold"
        const val STYLE_ITALIC = "italic"
        const val STYLE_BOLD_ITALIC = "bold_italic"
    }
}

data class StickerProject(
    val source: Bitmap? = null,
    val fillWhiteBackground: Boolean = false,
    val sourceScale: Float = 1f,
    val sourceRotation: Float = 0f,
    val layers: List<StickerTextLayer> = emptyList(),
)