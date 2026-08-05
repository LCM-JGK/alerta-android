package com.alertaturistica.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ModerationScreen(
    state: AppUiState,
    onRefresh: () -> Unit,
    onSelect: (Long) -> Unit,
    onClose: () -> Unit,
    onApprove: (Long) -> Unit,
    onReject: (Long) -> Unit,
) {
    LaunchedEffect(Unit) { onRefresh() }
    Column(Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(Modifier.padding(16.dp)) {
                Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Panel de moderación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Solo las fotografías aprobadas se muestran públicamente.")
                }
                OutlinedButton(onClick = onRefresh, enabled = !state.isModerating) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar pendientes")
                }
            }
        }
        if (state.isModerating && state.pendingPhotos.isEmpty()) {
            CircularProgressIndicator(Modifier.padding(24.dp))
        } else if (state.pendingPhotos.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                Text("No hay fotografías pendientes", style = MaterialTheme.typography.titleMedium)
                Text("Los nuevos envíos aparecerán aquí para su revisión.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.pendingPhotos, key = { it.zoneId }) { photo ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(photo.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(photo.description, maxLines = 3)
                            Text(
                                "${photo.category} · ${photo.sizeBytes / 1024} KB · ${photo.createdAt.substringBefore('T')}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Button(onClick = { onSelect(photo.zoneId) }, enabled = !state.isModerating) {
                                Icon(Icons.Outlined.ImageSearch, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Revisar fotografía")
                            }
                        }
                    }
                }
            }
        }
    }

    val selectedId = state.selectedPendingPhotoId
    if (selectedId != null) {
        val selected = state.pendingPhotos.firstOrNull { it.zoneId == selectedId }
        var confirmReject by remember(selectedId) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = onClose,
            icon = { Icon(Icons.Outlined.ImageSearch, contentDescription = null) },
            title = { Text(selected?.title ?: "Revisar fotografía") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.moderationPhotoBytes?.let { bytes ->
                        remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }?.let { image ->
                            Image(
                                bitmap = image,
                                contentDescription = "Fotografía pendiente de moderación",
                                modifier = Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    } ?: CircularProgressIndicator()
                    selected?.let {
                        Text(it.description)
                        Text("Comprueba que no aparezcan rostros, placas, documentos o datos personales.")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onApprove(selectedId) },
                    enabled = state.moderationPhotoBytes != null && !state.isModerating,
                ) { Text("Aprobar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onClose) { Text("Cerrar") }
                    TextButton(
                        onClick = { confirmReject = true },
                        enabled = state.moderationPhotoBytes != null && !state.isModerating,
                    ) { Text("Rechazar", color = MaterialTheme.colorScheme.error) }
                }
            },
        )

        if (confirmReject) {
            AlertDialog(
                onDismissRequest = { confirmReject = false },
                title = { Text("¿Rechazar y eliminar?") },
                text = { Text("La fotografía se eliminará del servidor y no podrá recuperarse.") },
                confirmButton = {
                    Button(onClick = { confirmReject = false; onReject(selectedId) }) { Text("Eliminar") }
                },
                dismissButton = { TextButton(onClick = { confirmReject = false }) { Text("Cancelar") } },
            )
        }
    }
}
