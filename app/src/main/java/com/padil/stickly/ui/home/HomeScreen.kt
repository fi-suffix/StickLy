package com.padil.stickly.ui.home

import android.app.Application
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.padil.stickly.StickLyApplication
import com.padil.stickly.data.StickerPack
import com.padil.stickly.ui.components.ConfirmDialog
import com.padil.stickly.ui.components.PackInfoDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPack: (String) -> Unit,
    onCreateSticker: () -> Unit,
    externalViewModel: HomeViewModel? = null,
) {
    val context = LocalContext.current
    val app = context.applicationContext as StickLyApplication
    val viewModel = externalViewModel ?: viewModel(factory = HomeViewModel.factory(app))
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    val whatsAppInstalled by viewModel.whatsAppInstalled.collectAsStateWithLifecycle()
    val added by viewModel.added.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    var showPackDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<StickerPack?>(null) }
    var deleteTarget by remember { mutableStateOf<StickerPack?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("StickLy", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = onCreateSticker) {
                            Icon(Icons.Filled.Create, contentDescription = "Buat stiker baru")
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showPackDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Paket baru")
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when {
                    refreshing -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    packs.isEmpty() -> EmptyState(onCreatePack = { showPackDialog = true })
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(packs, key = { it.id }) { pack ->
                            PackCard(
                                pack = pack,
                                trayBitmap = viewModel.trayBitmap(pack.id),
                                isAdded = added[pack.id] ?: false,
                                whatsAppInstalled = whatsAppInstalled,
                                packageName = app.packageName,
                                authority = app.authority,
                                onOpen = { onOpenPack(pack.id) },
                                onAddToWhatsApp = {
                                    viewModel.refresh()
                                    Toast.makeText(context, "Arahkan ke WhatsApp untuk menambah stiker", Toast.LENGTH_SHORT).show()
                                },
                                onRename = { renameTarget = pack },
                                onDelete = { deleteTarget = pack },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPackDialog) {
        PackInfoDialog(
            title = "Paket Stiker Baru",
            initialName = "",
            initialPublisher = "",
            onDismiss = { showPackDialog = false },
            onConfirm = { name, publisher ->
                val result = viewModel.createPack(name, publisher)
                result.onFailure {
                    Toast.makeText(context, it.message ?: "Gagal membuat paket", Toast.LENGTH_SHORT).show()
                }
                showPackDialog = false
            },
        )
    }

    renameTarget?.let { pack ->
        PackInfoDialog(
            title = "Ubah Paket",
            initialName = pack.name,
            initialPublisher = pack.publisher,
            onDismiss = { renameTarget = null },
            onConfirm = { name, publisher ->
                viewModel.renamePack(pack.id, name, publisher)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { pack ->
        ConfirmDialog(
            title = "Hapus Paket?",
            message = "Semua stiker di \"${pack.name}\" ikut terhapus. Stiker yang sudah masuk WhatsApp tidak akan hilang.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.deletePack(pack.id)
                deleteTarget = null
            },
        )
    }
}

@Composable
private fun PackCard(
    pack: StickerPack,
    trayBitmap: Bitmap?,
    isAdded: Boolean,
    whatsAppInstalled: Boolean,
    packageName: String,
    authority: String,
    onOpen: () -> Unit,
    onAddToWhatsApp: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val addLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        onAddToWhatsApp()
    }

    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
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
                Column(Modifier.weight(1f)) {
                    Text(
                        pack.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${pack.stickers.size} stiker • ${pack.publisher}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (!whatsAppInstalled) {
                    Text(
                        "WhatsApp belum terpasang",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (isAdded) {
                    Text(
                        "✓ Terpasang di WhatsApp",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    FilledTonalButton(
                        onClick = {
                            val intent = com.padil.stickly.whatsapp.WhatsAppApi.buildAddPackIntent(
                                context,
                                authority,
                                pack.id,
                                pack.name,
                            )
                            if (intent != null) {
                                addLauncher.launch(intent)
                            } else {
                                Toast.makeText(context, "WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
                            }
                        },
                    ) {
                        Icon(Icons.Filled.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tambah ke WhatsApp")
                    }
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onRename) {
                    Icon(Icons.Filled.Edit, contentDescription = "Ubah", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onCreatePack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Filled.Collections,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text("Belum ada paket stiker", style = MaterialTheme.typography.titleMedium)
            Text(
                "Buat paket, lalu tambahkan stiker agar bisa masuk ke WhatsApp tanpa satu-satu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            TextButton(onClick = onCreatePack) {
                Text("Buat Paket Pertama")
            }
        }
    }
}