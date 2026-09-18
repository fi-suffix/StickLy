package com.padil.stickly.util

object EmojiData {

    val common: List<String> = listOf(
        "\uD83D\uDE00", "\uD83D\uDE02", "\uD83D\uDE05", "\uD83D\uDE0D", "\uD83D\uDE0E",
        "\uD83D\uDE18", "\uD83D\uDE22", "\uD83D\uDE2D", "\uD83D\uDE21", "\uD83D\uDE24",
        "\uD83E\uDD2F", "\uD83E\uDD73", "\uD83D\uDE10", "\uD83D\uDE34", "\uD83E\uDD14",
        "\uD83D\uDE4C", "\uD83D\uDC4D", "\uD83D\uDC4F", "\uD83D\uDE4F", "\uD83D\uDC4C",
        "\u2764\uFE0F", "\uD83D\uDC94", "\uD83D\uDC95", "\uD83D\uDC96", "\uD83D\uDE0B",
        "\uD83C\uDF89", "\uD83C\uDF8A", "\uD83C\uDF82", "\uD83C\uDF81", "\uD83C\uDF88",
        "\uD83D\uDD25", "\u2b50", "\u2728", "\uD83C\uDF1F", "\uD83D\uDCAB",
        "\uD83D\uDE80", "\uD83C\uDF08", "\u2614", "\u2600\uFE0F", "\uD83C\uDF19",
        "\uD83C\uDF55", "\uD83C\uDF54", "\uD83C\uDF5F", "\uD83C\uDF69", "\uD83C\uDF6A",
        "\u2615", "\uD83C\uDF7A", "\uD83C\uDFB5", "\u26bd", "\uD83C\uDFC6",
        "\uD83D\uDC36", "\uD83D\uDC31", "\uD83D\uDC3C", "\uD83E\uDD84", "\uD83D\uDC35",
        "\uD83D\uDE4B", "\uD83D\uDC4B", "\uD83D\uDE02", "\uD83E\uDD17", "\uD83D\uDE33",
    )

    fun parse(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split(" ").filter { it.isNotBlank() }
    }

    fun join(emojis: List<String>): String = emojis.take(3).joinToString(" ")
}