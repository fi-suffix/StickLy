package com.padil.stickly.ui.editor

import android.app.Application
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.padil.stickly.StickLyApplication
import com.padil.stickly.data.StickerPack
import com.padil.stickly.sticker.StickerProject
import com.padil.stickly.sticker.StickerRenderer
import com.padil.stickly.sticker.StickerTextLayer
import com.padil.stickly.ui.components.EmojiPicker
import com.padil.stickly.ui.components.TransparencyGrid
import com.padil.stickly.util.EmojiData
import com.padil.stickly.util.ImageUtils
import java.io.File
import kotlin.math.roundToInt

private val TEXT_COLORS = listOf(
    Color.WHITE,
    Color.BLACK,
    0xFFE53935.toInt(),
    0xFF1E88E5.toInt(),
    0xFF43A047.toInt(),
    0xFFFDD835.toInt(),
    0xFFFF7043.toInt(),
    0xFF8E24AA.toInt(),
    0xFF00ACC1.toInt(),
    0xFFD81B60.toInt(),
)

private val OUTLINE_COLORS = listOf(
    Color.BLACK,
    Color.WHITE,
    0xFFE53935.toInt(),
    0xFF1E88E5.toInt(),
    0xFFF57C00.toInt(),
    0xFFFDD835.toInt(),
    0xFF8E24AA.toInt(),
)

private val FONT_STYLES = listOf(
    StickerTextLayer.STYLE_NORMAL to "Normal",
    StickerTextLayer.STYLE_BOLD to "Bold",
    StickerTextLayer.STYLE_ITALIC to "Italic",
    StickerTextLayer.STYLE_BOLD_ITALIC to "Bold Italic",
)

private val FONT_FAMILY_LABELS = listOf(
    StickerTextLayer.FONT_SANS to "Sans",
    StickerTextLayer.FONT_SERIF to "Serif",
    StickerTextLayer.FONT_MONO to "Mono",
    StickerTextLayer.FONT_CURSIVE to "Kursif",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    packId: String,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    externalViewModel: EditorViewModel? = null,
) {
    val context = LocalContext.current
    val app = context.applicationContext as StickLyApplication
    val viewModel = externalViewModel
        ?: viewModel(key = "editor", factory = EditorViewModel.factory(app, packId.takeIf { it.isNotBlank() }))
    val project by viewModel.project.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedLayerId.collectAsStateWithLifecycle()
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    val loading by viewModel.loadingSource.collectAsStateWithLifecycle()

    val showEditor = loading || project.source != null || project.layers.isNotEmpty()

    var saving by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadSource(uri)
        }
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            pendingCameraUri?.let { uri ->
                viewModel.loadSource(uri)
            }
        }
    }

    fun launchGallery() {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    fun launchCamera() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${app.packageName}.fileprovider", file)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Buat Stiker", fontWeight = FontWeight.Bold)
                        packId.takeIf { it.isNotBlank() }?.let { id ->
                            packs.firstOrNull { it.id == id }?.let {
                                Text(it.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    when {
                        saving -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        else -> Button(
                            onClick = { showSaveDialog = true },
                            enabled = showEditor,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Menyiapkan…", fontWeight = FontWeight.Bold)
                            } else {
                                Text("Simpan", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            if (showEditor) {
                EditorBody(
                    project = project,
                    selectedId = selectedId,
                    loading = loading,
                    onModifyLayer = viewModel::updateLayer,
                    onAddLayer = viewModel::addTextLayer,
                    onDeleteLayer = viewModel::deleteLayer,
                    onSelectLayer = viewModel::selectLayer,
                    onFillWhite = viewModel::setFillWhite,
                    onTransformSource = viewModel::transformSource,
                    onResetTransform = viewModel::resetSourceTransform,
                )
            } else {
                SourceChooser(
                    onGallery = ::launchGallery,
                    onCamera = ::launchCamera,
                    onText = {
                        viewModel.setSource(null)
                        viewModel.addTextLayer()
                    },
                )
            }
        }
    }

    if (showSaveDialog) {
        SaveStickerDialog(
            packs = packs,
            fixedPackId = packId.takeIf { it.isNotBlank() },
            stickerSummary = buildString {
                if (project.source != null) append("gambar")
                if (project.layers.isNotEmpty()) {
                    if (isNotEmpty()) append(" + ")
                    append("${project.layers.size} teks")
                }
            },
            onDismiss = { showSaveDialog = false },
            onSave = { emojis, targetPackId, newName, newPublisher ->
                saving = true
                showSaveDialog = false
                viewModel.saveSticker(
                    emojis = emojis,
                    targetPackId = if (targetPackId.isNotBlank()) targetPackId else null,
                    newPackName = if (targetPackId.isBlank()) newName else null,
                    newPublisher = if (targetPackId.isBlank()) newPublisher else null,
                ) { result ->
                    saving = false
                    result.onSuccess { (_, savedPackId) ->
                        Toast.makeText(context, "Stiker disimpan ✓", Toast.LENGTH_SHORT).show()
                        onSaved(savedPackId)
                    }.onFailure { e ->
                        Toast.makeText(context, e.message ?: "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }
}

@Composable
private fun SourceChooser(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onText: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Pilih sumber stiker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Stiker 512×512 transparan, siap untuk WhatsApp.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SourceOption("Teks saja", "Buat stiker tulisan/kata-kata langsung", Icons.Filled.TextFields, MaterialTheme.colorScheme.tertiary, onText)
        SourceOption("Galeri", "Ambil gambar yang sudah ada di ponselmu", Icons.Filled.PhotoLibrary, MaterialTheme.colorScheme.primary, onGallery)
        SourceOption("Kamera", "Ambil foto baru untuk stiker", Icons.Filled.PhotoCamera, MaterialTheme.colorScheme.secondary, onCamera)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SourceOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = tint.copy(alpha = 0.15f), shape = RoundedCornerShape(14.dp)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EditorBody(
    project: StickerProject,
    selectedId: Long?,
    loading: Boolean,
    onModifyLayer: (Long, (StickerTextLayer) -> StickerTextLayer) -> Unit,
    onAddLayer: () -> Unit,
    onDeleteLayer: (Long) -> Unit,
    onSelectLayer: (Long?) -> Unit,
    onFillWhite: (Boolean) -> Unit,
    onTransformSource: (Float, Float) -> Unit,
    onResetTransform: () -> Unit,
) {
    val preview = remember(project) { StickerRenderer.render(project) }
    val selectedLayer = project.layers.firstOrNull { it.id == selectedId }
    val sourceTransformed = project.source != null && (project.sourceScale != 1f || project.sourceRotation != 0f)

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val scale = if (canvasSize.width > 0) StickerRenderer.SIZE.toFloat() / canvasSize.width.toFloat() else 1f

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            TransparencyGrid(Modifier.matchParentSize())
            Image(
                bitmap = preview.asImageBitmap(),
                contentDescription = "Pratinjau stiker",
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(selectedId) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            if (selectedId != null && (pan.x != 0f || pan.y != 0f)) {
                                val dx = pan.x * scale
                                val dy = pan.y * scale
                                onModifyLayer(selectedId) { it.copy(anchorX = it.anchorX + dx, anchorY = it.anchorY + dy) }
                            }
                            if (zoom != 1f || rotation != 0f) {
                                onTransformSource(zoom, rotation)
                            }
                        }
                    },
                contentScale = ContentScale.Fit,
            )
            if (loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }

        LayerBar(
            layers = project.layers,
            selectedId = selectedId,
            onAdd = onAddLayer,
            onSelect = onSelectLayer,
            onDelete = onDeleteLayer,
        )

        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = project.fillWhiteBackground,
                onClick = { onFillWhite(!project.fillWhiteBackground) },
                label = { Text("Latar putih") },
            )
            if (sourceTransformed) {
                TextButton(onClick = onResetTransform) {
                    Text("Reset gambar")
                }
            }
            Text(
                "Seret teks • Cubit utk besar/kecil • Putar 2 jari utk miring",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(Modifier.padding(horizontal = 16.dp))

        if (selectedLayer != null) {
            LayerControls(
                layer = selectedLayer,
                onUpdate = { transform -> onModifyLayer(selectedLayer.id, transform) },
            )
        } else {
            Text(
                "Pilih chip teks di atas untuk mengubah warna, font, ukuran, dan lainnya.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun LayerBar(
    layers: List<StickerTextLayer>,
    selectedId: Long?,
    onAdd: () -> Unit,
    onSelect: (Long?) -> Unit,
    onDelete: (Long) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilledTonalButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Teks")
            }
        }
        items(layers, key = { it.id }) { layer ->
            val label = layer.text.ifBlank { "Teks" }
            val index = layers.indexOf(layer)
            val selected = layer.id == selectedId
            InputChip(
                selected = selected,
                onClick = { onSelect(if (selected) null else layer.id) },
                label = { Text("$label ($index)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = {
                    if (selected) {
                        IconButton(onClick = { onDelete(layer.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Hapus teks",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun LayerControls(
    layer: StickerTextLayer,
    onUpdate: ((StickerTextLayer) -> StickerTextLayer) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var showFontMenu by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedTextField(
            value = layer.text,
            onValueChange = { newText ->
                onUpdate { it.copy(text = newText.take(200)) }
            },
            label = { Text("Tulisan") },
            minLines = 1,
            maxLines = 6,
            supportingText = { Text("Tekan Enter untuk baris baru") },
            modifier = Modifier.fillMaxWidth(),
        )

        Column {
            Text("Warna", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TEXT_COLORS.forEach { color ->
                    val isSelected = layer.colorArgb == color.toInt()
                    val isDark = isDarkColor(color)
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(androidx.compose.ui.graphics.Color(color))
                            .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
                            .clickable { onUpdate { it.copy(colorArgb = color.toInt()) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black,
                            )
                        }
                    }
                }
            }
        }

        Column {
            Text("Outline", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !layer.outlineEnabled,
                    onClick = { onUpdate { it.copy(outlineEnabled = false) } },
                    label = { Text("Tanpa") },
                )
                FilterChip(
                    selected = layer.outlineEnabled,
                    onClick = { onUpdate { it.copy(outlineEnabled = true) } },
                    label = { Text("Outline") },
                )
            }
            if (layer.outlineEnabled) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OUTLINE_COLORS.forEach { color ->
                        val isSelected = layer.outlineColorArgb == color.toInt()
                        val isDark = isDarkColor(color)
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(androidx.compose.ui.graphics.Color(color))
                                .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
                                .clickable { onUpdate { it.copy(outlineColorArgb = color.toInt()) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                SliderValueRow("Tebal", "${layer.outlineWidth.roundToInt()}", layer.outlineWidth, 2f..16f) { v ->
                    onUpdate { it.copy(outlineWidth = v) }
                }
            }
        }

        Column {
            Text("Jenis huruf", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FONT_FAMILY_LABELS.forEach { (key, label) ->
                    FilterChip(
                        selected = layer.fontFamily == key,
                        onClick = { onUpdate { it.copy(fontFamily = key) } },
                        label = { Text(label) },
                    )
                }
            }
        }

        Column {
            Text("Gaya", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                FONT_STYLES.forEach { (key, label) ->
                    FilterChip(
                        selected = layer.fontStyle == key,
                        onClick = { onUpdate { it.copy(fontStyle = key) } },
                        label = { Text(label) },
                    )
                }
            }
        }

        SliderValueRow("Ukuran", "${layer.fontSize.roundToInt()}", layer.fontSize, 24f..160f) { v ->
            onUpdate { it.copy(fontSize = v) }
        }

        SliderValueRow("Rotasi", "${layer.rotationDeg.roundToInt()}°", layer.rotationDeg, -180f..180f) { v ->
            onUpdate { it.copy(rotationDeg = v) }
        }

        SliderValueRow("Transparansi", "${(layer.alpha * 100).roundToInt()}%", layer.alpha, 0.1f..1f) { v ->
            onUpdate { it.copy(alpha = v) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Font", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Box {
                FilledTonalButton(onClick = { showFontMenu = true }) {
                    Text(FONT_FAMILY_LABELS.firstOrNull { it.first == layer.fontFamily }?.second ?: layer.fontFamily)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showFontMenu, onDismissRequest = { showFontMenu = false }) {
                    FONT_FAMILY_LABELS.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onUpdate { it.copy(fontFamily = key) }
                                showFontMenu = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { focusManager.clearFocus() }) { Text("Selesai") }
        }
    }
}

@Composable
private fun SliderValueRow(
    label: String,
    display: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp),
        )
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = Modifier.weight(1f))
        Text(display, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(56.dp))
    }
}

@Composable
private fun SaveStickerDialog(
    packs: List<StickerPack>,
    fixedPackId: String?,
    stickerSummary: String,
    onDismiss: () -> Unit,
    onSave: (emojis: String, targetPackId: String, newName: String, newPublisher: String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var isNew by remember { mutableStateOf(packs.isEmpty()) }
    var selectedPackId by remember { mutableStateOf(fixedPackId ?: packs.firstOrNull()?.id ?: "") }
    var newName by remember { mutableStateOf("") }
    var newPublisher by remember { mutableStateOf("") }
    var emojis by remember { mutableStateOf<List<String>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simpan Stiker") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (fixedPackId != null) {
                    val target = packs.firstOrNull { it.id == fixedPackId }
                    Text("Ke paket: ${target?.name ?: ""}", style = MaterialTheme.typography.titleSmall)
                } else {
                    Text("Pilih paket tujuan:", style = MaterialTheme.typography.labelMedium)
                    packs.forEach { pack ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isNew = false
                                    selectedPackId = pack.id
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = !isNew && selectedPackId == pack.id, onClick = {
                                isNew = false
                                selectedPackId = pack.id
                            })
                            Text("${pack.name} (${pack.stickers.size}/30)", modifier = Modifier.weight(1f))
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { isNew = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isNew, onClick = { isNew = true })
                        Text("Buat paket baru", modifier = Modifier.weight(1f))
                    }
                    if (isNew) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Nama paket baru (min 3)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = newPublisher,
                            onValueChange = { newPublisher = it },
                            label = { Text("Nama publisher (Anda)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                EmojiPicker(selected = emojis, onToggle = { emoji ->
                    emojis = when {
                        emoji in emojis -> emojis.filterNot { it == emoji }
                        emojis.size < 3 -> emojis + emoji
                        else -> emojis
                    }
                })
                Text(
                    "Stiker: $stickerSummary",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                focusManager.clearFocus()
                onSave(
                    EmojiData.join(emojis),
                    if (isNew) "" else selectedPackId,
                    newName,
                    newPublisher,
                )
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

private fun isDarkColor(argb: Int): Boolean {
    val r = (argb shr 16 and 0xFF) / 255f
    val g = (argb shr 8 and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return 0.299f * r + 0.587f * g + 0.114f * b < 0.5f
}