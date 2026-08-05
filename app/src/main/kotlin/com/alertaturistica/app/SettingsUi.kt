package com.alertaturistica.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    settings: AppSettings,
    isModerator: Boolean,
    onOpenModeration: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (isModerator) {
            SettingsSection("Administración", Icons.Outlined.AdminPanelSettings) {
                Text("Esta sesión pertenece a una cuenta moderadora.")
                Button(onClick = onOpenModeration, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null)
                    Spacer(Modifier.padding(5.dp))
                    Text("Revisar publicaciones pendientes")
                }
            }
        }
        SettingsSection("Mapa y orientación", Icons.Outlined.Explore) {
            SettingSwitch("Mostrar brújula", "Indica hacia dónde apunta el dispositivo.", settings.showCompass) {
                onUpdate { value -> value.copy(showCompass = it, orientMapWithDevice = value.orientMapWithDevice && it) }
            }
            SettingSwitch(
                "Orientar mapa automáticamente",
                "Gira el mapa con el magnetómetro, giroscopio y acelerómetro.",
                settings.orientMapWithDevice,
                enabled = settings.showCompass,
            ) { onUpdate { value -> value.copy(orientMapWithDevice = it) } }
            SettingSwitch("Reducir movimiento", "Evita animaciones largas en el mapa.", settings.reduceMotion) {
                onUpdate { value -> value.copy(reduceMotion = it) }
            }
        }
        SettingsSection("Sensores y reportes", Icons.Outlined.Sensors) {
            SettingSwitch(
                "Detectar impactos",
                "Ante un movimiento brusco pregunta si necesitas crear un reporte; nunca publica solo.",
                settings.impactDetection,
            ) { onUpdate { value -> value.copy(impactDetection = it) } }
            SettingSwitch(
                "Permitir fotografías",
                "Adjunta imágenes reducidas y sin metadatos; requieren moderación antes de publicarse.",
                settings.allowCameraAttachments,
            ) { onUpdate { value -> value.copy(allowCameraAttachments = it) } }
            SettingSwitch(
                "Tema según luz ambiental",
                "Usa alto contraste oscuro en lugares con poca iluminación.",
                settings.ambientLightTheme,
            ) { onUpdate { value -> value.copy(ambientLightTheme = it) } }
        }
        SettingsSection("Accesibilidad", Icons.Outlined.Accessibility) {
            SettingSwitch("Texto más grande", "Aumenta el tamaño general del texto.", settings.largeText) {
                onUpdate { value -> value.copy(largeText = it) }
            }
            SettingSwitch("Contraste alto", "Refuerza la separación entre texto, fondo y controles.", settings.highContrast) {
                onUpdate { value -> value.copy(highContrast = it) }
            }
            SettingSwitch(
                "Interfaz simplificada",
                "Oculta ayudas y elementos secundarios para reducir la carga visual.",
                settings.simplifiedInterface,
            ) { onUpdate { value -> value.copy(simplifiedInterface = it) } }
        }
        Text(
            "Los sensores se procesan en el teléfono. La aplicación no graba audio ni publica automáticamente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.padding(5.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
