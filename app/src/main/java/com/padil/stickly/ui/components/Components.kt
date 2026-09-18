package com.padil.stickly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.padil.stickly.util.EmojiData
import kotlin.math.ceil

@Composable
fun TransparencyGrid(modifier: Modifier = Modifier, cell: Dp = 12.dp) {
    val color1 = MaterialTheme.colorScheme.surfaceVariant
    val color2 = MaterialTheme.colorScheme.background
    val cellPx = with(LocalDensity.current) { cell.toPx() }
    Canvas(modifier = modifier) {
        if (cellPx <= 0f) return@Canvas
        val cols = ceil(size.width / cellPx).toInt()
        val rows = ceil(size.height / cellPx).toInt()
        for (x in 0 until cols) {
            for (y in 0 until rows) {
                if ((x + y) % 2 == 0) {
                    drawRect(
                        color = color1,
                        topLeft = Offset(x * cellPx, y * cellPx),
                        size = Size(cellPx, cellPx),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun EmojiPicker(
    selected: List<String>,
    onToggle: (String) -> Unit,
    maxSelection: Int = 3,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Emoji (maks $maxSelection)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.Center) {
            EmojiData.common.forEach { emoji ->
                val isSelected = emoji in selected
                val enabled = isSelected || selected.size < maxSelection
                FilterChip(
                    selected = isSelected,
                    enabled = enabled,
                    onClick = { onToggle(emoji) },
                    label = { Text(emoji) },
                    modifier = Modifier.padding(2.dp),
                )
            }
        }
    }
}

@Composable
fun PackInfoDialog(
    title: String,
    initialName: String,
    initialPublisher: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var publisher by remember(initialPublisher) { mutableStateOf(initialPublisher) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama paket (min 3 karakter)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = publisher,
                    onValueChange = { publisher = it },
                    label = { Text("Nama publisher (Anda)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), publisher.trim()) }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Hapus",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Suppress("unused")
@Composable
fun DotIndicator(active: Boolean) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.size(8.dp)) {
        drawCircle(color)
    }
}