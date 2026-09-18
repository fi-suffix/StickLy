package com.padil.stickly.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsAppApi {

    const val PACKAGE_WHATSAPP = "com.whatsapp"
    const val PACKAGE_BUSINESS = "com.whatsapp.w4b"
    const val ACTION_ENABLE_PACK = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"

    fun isWhatsAppInstalled(context: Context): Boolean =
        isInstalled(context, PACKAGE_WHATSAPP) || isInstalled(context, PACKAGE_BUSINESS)

    private fun isInstalled(context: Context, packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }

    fun buildAddPackIntent(
        context: Context,
        authority: String,
        identifier: String,
        name: String,
    ): Intent? {
        val candidates = listOf(PACKAGE_WHATSAPP, PACKAGE_BUSINESS)
        candidates.forEach { packageName ->
            val intent = Intent(ACTION_ENABLE_PACK).apply {
                putExtra("sticker_pack_id", identifier)
                putExtra("sticker_pack_authority", authority)
                putExtra("sticker_pack_name", name)
                setPackage(packageName)
            }
            if (intent.resolveActivity(context.packageManager) != null) return intent
        }
        return null
    }

    fun isPackAdded(context: Context, authority: String, identifier: String): Boolean {
        val bases = listOf(
            "content://com.whatsapp.provider.sticker_whitelist_check/is_whitelisted",
            "content://com.whatsapp.w4b.provider.sticker_whitelist_check/is_whitelisted",
        )
        bases.forEach { base ->
            try {
                val uri = Uri.parse("$base?authority='$authority'&identifier='$identifier'")
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex("result")
                        if (index >= 0 && cursor.getInt(index) == 1) return true
                    }
                }
            } catch (_: Exception) {
            }
        }
        return false
    }
}