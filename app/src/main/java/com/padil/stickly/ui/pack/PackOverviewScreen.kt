package com.padil.stickly.ui.pack

import android.app.Application
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.padil.stickly.data.Sticker
import com.padil.stickly.ui.components.ConfirmDialog
import com.padil.stickly.ui.components.EmojiPicker
import com.padil.stickly.ui.components.PackInfoDialog
import com.padil.stickly.ui.components.TransparencyGrid
import com.padil.stickly.util.EmojiData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackOverviewScreen(
    packId: String,
    onBack: () -> Unit,
    onAddSticker: (String) -> Unit,
    externalViewModel: PackOverviewViewModel? = null,
) {
    val context = LocalContext.current
    val viewModel = externalViewModel ?: viewModel(
        key = "pack_$packId",
        factory = PackOverviewViewModel.factory(context.applicationContext as Application, packId),
    )
    val pack by viewModel.pack.collectAsStateWithLifecycle()
    val whatsAppInstalled by viewModel.whatsAppInstalled.collectAsStateWithLifecycle()
    val added by viewModel.added.collectAsStateWithLifecycle()

    var showRename by remember { mutableStateOf(false) }
    var showDeletePack by remember { mutableStateOf(false) }
    var emojiSticker by remember { mutableStateOf<Sticker?>(null) }
    var deleteSticker by remember { mutableStateOf<Sticker?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val trayBitmap = remember(pack?.id, pack?.dataVersion) {
        pack?.let { viewModel.trayBitmap() }
    }

    LaunchedEffect(packId) {
        viewModel.refreshStatus()
    }

    val addLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        viewModel.refreshStatus()
    }

    val addIntent = remember(pack?.id, pack?.dataVersion) { viewModel.addToWhatsAppIntent() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pack?.name ?: "Paket", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Ubah nama paket") },
                            onClick = { showMenu = false; showRename = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus paket") },
                            onClick = { showMenu = false; showDeletePack = true },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddSticker(packId) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Tambah Stiker")
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            val current = pack
            if (current == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                Column(Modifier.fillMaxSize()) {
                    HeaderCard(
                        name = current.name,
                        publisher = current.publisher,
                        stickerCount = current.stickers.size,
                        trayBitmap = trayBitmap,
                        whatsAppInstalled = whatsAppInstalled,
                        added = added,
                        onAddToWhatsApp = {
                            if (current.stickers.size in 3..30) {
                                if (addIntent != null) {
                                    addLauncher.launch(addIntent)
                                } else {
                                    Toast.makeText(context, "WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Paket butuh 3–30 stiker untuk ditambahkan ke WhatsApp",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )

                    if (current.stickers.isEmpty()) {
                        Column(
                            Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Filled.Collections,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Paket masih kosong.\nMulai tambahkan stiker pertama (minimal 3 agar bisa masuk WhatsApp).",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 88.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(current.stickers, key = { it.id }) { sticker ->
                                StickerCell(
                                    fileName = sticker.fileName,
                                    dataVersion = current.dataVersion,
                                    bitmapLoader = { viewModel.stickerBitmap(sticker.fileName) },
                                    onClick = { emojiSticker = sticker },
                                    onLongClick = { deleteSticker = sticker },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRename) {
        pack?.let {
            PackInfoDialog(
                title = "Ubah Paket",
                initialName = it.name,
                initialPublisher = it.publisher,
                onDismiss = { showRename = false },
                onConfirm = { name, publisher ->
                    val result = viewModel.renamePack(name, publisher)
                    if (result.isFailure) {
                        Toast.makeText(context, result.exceptionOrNull()?.message ?: "Gagal", Toast.LENGTH_SHORT).show()
                    }
                    showRename = false
                },
            )
        }
    }

    if (showDeletePack) {
        ConfirmDialog(
            title = "Hapus Paket?",
            message = "Paket \"${pack?.name}\" dan stikernya akan dihapus dari aplikasi. Stiker yang sudah masuk WhatsApp tetap ada.",
            onDismiss = { showDeletePack = false },
            onConfirm = {
                viewModel.deletePack()
                showDeletePack = false
                onBack()
            },
        )
    }

    deleteSticker?.let { sticker ->
        ConfirmDialog(
            title = "Hapus Stiker?",
            message = "Stiker ini akan dihapus dari paket.",
            onDismiss = { deleteSticker = null },
            onConfirm = {
                viewModel.deleteSticker(sticker.id)
                deleteSticker = null
            },
        )
    }

    emojiSticker?.let { sticker ->
        EmojiEditDialog(
            initialEmojis = EmojiData.parse(sticker.emojis),
            onDismiss = { emojiSticker = null },
            onConfirm = { list ->
                viewModel.updateEmojis(sticker.id, EmojiData.join(list))
                emojiSticker = null
            },
        )
    }
}

@Composable
private fun HeaderCard(
    name: String,
    publisher: String,
    stickerCount: Int,
    trayBitmap: Bitmap?,
    whatsAppInstalled: Boolean,
    added: Boolean,
    onAddToWhatsApp: () -> Unit,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (trayBitmap != null) {
                        Image(
                            bitmap = trayBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "$stickerCount stiker • $publisher",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when {
                !whatsAppInstalled -> Text(
                    "WhatsApp belum terpasang di perangkat ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                added -> Text(
                    "✓ Sudah terpasang di WhatsApp",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                else -> Button(
                    onClick = onAddToWhatsApp,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = stickerCount in 3..30,
                ) {
                    Icon(Icons.Filled.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (stickerCount in 3..30) "Tambah ke WhatsApp" else "Butuh 3–30 stiker")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StickerCell(
    fileName: String,
    dataVersion: Int,
    bitmapLoader: () -> Bitmap?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val bitmap = remember(fileName, dataVersion) { bitmapLoader() }
    Card(shape = RoundedCornerShape(16.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
            TransparencyGrid(Modifier.matchParentSize())
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun EmojiEditDialog(
    initialEmojis: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    var emojis by remember(initialEmojis) { mutableStateOf(initialEmojis) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Emoji Stiker") },
        text = {
            EmojiPicker(
                selected = emojis,
                onToggle = { emoji ->
                    emojis = when {
                        emoji in emojis -> emojis.filterNot { it == emoji }
                        emojis.size < 3 -> emojis + emoji
                        else -> emojis
                    }
                },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(emojis) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}