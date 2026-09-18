package com.padil.stickly.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun InfoScreen() {
    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("StickLy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Buat stiker WhatsApp sendiri langsung dari ponsel: pilih gambar atau buat tulisan, tambahkan teks, lalu simpan dalam paket agar masuk WhatsApp sekaligus — tanpa tambah satu per satu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            InfoSection(
                title = "Cara pakai",
                items = listOf(
                    "1. Tab \"Buat\" di bawah, pilih Galeri / Kamera / Teks saja.",
                    "2. Tambahkan teks, atur warna, font, ukuran & rotasi.",
                    "3. Tekan \u201cSimpan\u201d, pilih paket tujuan (atau buat paket baru).",
                    "4. Buka paket, lalu tekan \u201cTambah ke WhatsApp\u201d.",
                    "5. Konfirmasi di WhatsApp — seluruh isi paket langsung terpasang.",
                ),
            )

            InfoSection(
                title = "Ketentuan WhatsApp (dari panduan resmi)",
                items = listOf(
                    "• Stiker: WebP 512×512, latar transparan, ≤100 KB.",
                    "• Satu paket berisi 3–30 stiker.",
                    "• Ikon paket (tray) 96×96, ≤50 KB — dibuat otomatis.",
                    "• Emoji (maks 3 per stiker) untuk memudahkan pencarian.",
                    "• Perubahan paket otomatis memperbarui versi data di WhatsApp.",
                ),
            )

            InfoSection(
                title = "Software untuk pengembangan",
                items = listOf(
                    "• Android Studio (IDE + emulator + SDK Manager)",
                    "• JDK 17+ (terbundle di Android Studio)",
                    "• Android SDK: platform android-35, build-tools 36",
                    "• Kotlin 2.0.x + Jetpack Compose (Material 3)",
                    "• Perangkat/emulator dengan WhatsApp untuk pengujian",
                ),
            )

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text(
                        "Tips: stiker dengan teks sebaiknya memakai latar transparan agar menyatu dengan chat. Gunakan sedikit tepi gelap untuk teks agar mudah dibaca di latar terang atau gelap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoSection(title: String, items: List<String>) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            items.forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}