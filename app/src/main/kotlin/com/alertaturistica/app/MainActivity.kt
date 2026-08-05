package com.alertaturistica.app

import android.Manifest
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.fragment.app.FragmentActivity
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.alertaturistica.app.db.AlertaDatabase
import org.maplibre.android.geometry.LatLng
import java.util.Locale
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AppScreen(val label: String, val icon: ImageVector) {
    MAP("Mapa", Icons.Outlined.Map),
    ALERTS("Avisos", Icons.Outlined.ListAlt),
    REPORT("Reportar", Icons.Outlined.AddLocationAlt),
    ACCOUNT("Cuenta", Icons.Outlined.Person),
    SETTINGS("Ajustes", Icons.Outlined.Settings),
    MODERATION("Moderación", Icons.Outlined.AdminPanelSettings),
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = AlertaDatabase(
            AndroidSqliteDriver(AlertaDatabase.Schema, applicationContext, "alerta.db"),
        )
        val sessionStore = SecureSessionStore(applicationContext)
        val settingsStore = AppSettingsStore(applicationContext)
        setContent {
            val settings = settingsStore.settings
            val ambientLux = rememberAmbientLightLux(settings.ambientLightTheme)
            AlertaTheme(settings, ambientLux) {
                val zonesViewModel: ZonesViewModel = viewModel(
                    factory = ZonesFactory(ZonesRepository(database, sessionStore)),
                )
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthFactory(AuthRepository(sessionStore), sessionStore),
                )
                AlertaApp(
                    viewModel = zonesViewModel,
                    authViewModel = authViewModel,
                    biometricAvailable = canUseStrongBiometrics(this),
                    onBiometricRequest = { onSuccess, onError ->
                        requestBiometricAuthentication(
                            title = "Confirma tu identidad",
                            subtitle = "Usa la huella o rostro registrado en este dispositivo",
                            onSuccess = onSuccess,
                            onError = onError,
                        )
                    },
                    settings = settings,
                    onUpdateSettings = settingsStore::update,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertaApp(
    viewModel: ZonesViewModel,
    authViewModel: AuthViewModel,
    biometricAvailable: Boolean,
    onBiometricRequest: (() -> Unit, (String) -> Unit) -> Unit,
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
) {
    val state = viewModel.uiState
    val authState = authViewModel.uiState
    val context = LocalContext.current
    var screenName by rememberSaveable { mutableStateOf(AppScreen.MAP.name) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var pendingLocationAction by remember { mutableStateOf<((LatLng) -> Unit)?>(null) }
    val screen = AppScreen.valueOf(screenName)
    val snackbarHostState = remember { SnackbarHostState() }
    val heading = rememberHeadingDegrees(settings.showCompass)
    val impact = rememberImpactEvent(settings.impactDetection)
    var handledImpactId by rememberSaveable { mutableStateOf<Long?>(null) }
    var accidentPresetId by rememberSaveable { mutableStateOf<Long?>(null) }

    if (authState.isLocked) {
        BiometricLockScreen(
            onUnlock = {
                onBiometricRequest(
                    authViewModel::unlockWithBiometrics,
                    authViewModel::biometricFailed,
                )
            },
            onUsePassword = authViewModel::logout,
        )
        return
    }

    if (authState.isRestoring) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val locateFromDevice = {
        requestCurrentLocation(
            context = context,
            onSuccess = { location ->
                currentLocation = location
                pendingLocationAction?.invoke(location)
                pendingLocationAction = null
            },
            onError = { message ->
                pendingLocationAction = null
                viewModel.showMessage(message)
            },
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) {
            locateFromDevice()
        } else {
            pendingLocationAction = null
            viewModel.showMessage("Sin permiso no es posible mostrar tu ubicación actual.")
        }
    }
    val requestLocation: ((LatLng) -> Unit) -> Unit = { onLocated ->
        pendingLocationAction = onLocated
        if (hasLocationPermission(context)) {
            locateFromDevice()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(authState.message) {
        authState.message?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Alerta local", fontWeight = FontWeight.SemiBold)
                        if (!settings.simplifiedInterface) {
                        Text(
                            text = when (screen) {
                                AppScreen.MAP -> "Explora con precaución"
                                AppScreen.ALERTS -> "Información de la comunidad"
                                AppScreen.REPORT -> "Ayuda a otras personas"
                                AppScreen.ACCOUNT -> "Privacidad y seguridad"
                                    AppScreen.SETTINGS -> "Personaliza tu experiencia"
                                    AppScreen.MODERATION -> "Revisión de contenido"
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                        }
                    }
                },
                actions = {
                    if (screen == AppScreen.MAP || screen == AppScreen.ALERTS) {
                        IconButton(onClick = viewModel::refresh, enabled = !state.isLoading) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar avisos")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                val destinations = if (settings.simplifiedInterface) {
                    listOf(AppScreen.MAP, AppScreen.REPORT, AppScreen.ACCOUNT, AppScreen.SETTINGS)
                } else AppScreen.entries.filterNot { it == AppScreen.MODERATION }
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = screen == destination,
                        onClick = { screenName = destination.name },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                AppScreen.MAP -> MapScreen(
                    state = state,
                    currentLocation = currentLocation,
                    onRequestLocation = requestLocation,
                    onSearchPlaces = viewModel::searchPlaces,
                    onClearPlaceSearch = viewModel::clearPlaceSearch,
                    headingDegrees = heading,
                    settings = settings,
                )
                AppScreen.ALERTS -> AlertsScreen(state.zones, settings.simplifiedInterface)
                AppScreen.REPORT -> if (authState.user == null) {
                    SignInRequiredScreen { screenName = AppScreen.ACCOUNT.name }
                } else {
                    ReportScreen(
                        zones = state.zones,
                        isSubmitting = state.isSubmitting,
                        currentLocation = currentLocation,
                        onRequestLocation = requestLocation,
                        onSubmit = { request ->
                            viewModel.submit(request) { screenName = AppScreen.MAP.name }
                        },
                        accidentPresetId = accidentPresetId,
                        cameraEnabled = settings.allowCameraAttachments,
                        onMessage = viewModel::showMessage,
                        settings = settings,
                    )
                }
                AppScreen.ACCOUNT -> AccountScreen(
                    state = authState,
                    biometricAvailable = biometricAvailable,
                    onShow = authViewModel::show,
                    onLogin = authViewModel::login,
                    onRegister = authViewModel::register,
                    onReset = authViewModel::resetPassword,
                    onRecoveryCodeSaved = authViewModel::recoveryCodeSaved,
                    onEnableBiometric = {
                        onBiometricRequest(
                            authViewModel::enableBiometrics,
                            authViewModel::biometricFailed,
                        )
                    },
                    onDisableBiometric = authViewModel::disableBiometrics,
                    onLogout = authViewModel::logout,
                )
                AppScreen.SETTINGS -> SettingsScreen(
                    settings = settings,
                    isModerator = authState.user?.isModerator == true,
                    onOpenModeration = { screenName = AppScreen.MODERATION.name },
                    onUpdate = onUpdateSettings,
                )
                AppScreen.MODERATION -> if (authState.user?.isModerator == true) {
                    ModerationScreen(
                        state = state,
                        onRefresh = viewModel::loadPendingPhotos,
                        onSelect = viewModel::selectPendingPhoto,
                        onClose = viewModel::closePendingPhoto,
                        onApprove = viewModel::approvePendingPhoto,
                        onReject = viewModel::rejectPendingPhoto,
                    )
                } else {
                    SignInRequiredScreen { screenName = AppScreen.ACCOUNT.name }
                }
            }
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }

    val unhandledImpact = impact?.takeIf { it.id != handledImpactId }
    if (unhandledImpact != null) {
        AlertDialog(
            onDismissRequest = { handledImpactId = unhandledImpact.id },
            icon = { Icon(Icons.Outlined.WarningAmber, contentDescription = null) },
            title = { Text("¿Estás bien?") },
            text = {
                Text(
                    "Se detectó un movimiento brusco (${String.format(Locale.US, "%.1f", unhandledImpact.forceG)} g). " +
                        "La aplicación no publicará nada sin tu confirmación.",
                )
            },
            dismissButton = {
                OutlinedButton(onClick = { handledImpactId = unhandledImpact.id }) { Text("Estoy bien") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        handledImpactId = unhandledImpact.id
                        accidentPresetId = unhandledImpact.id
                        screenName = AppScreen.REPORT.name
                    },
                ) { Text("Preparar reporte") }
            },
        )
    }
}

@Composable
private fun MapScreen(
    state: AppUiState,
    currentLocation: LatLng?,
    onRequestLocation: ((LatLng) -> Unit) -> Unit,
    onSearchPlaces: (String) -> Unit,
    onClearPlaceSearch: () -> Unit,
    headingDegrees: Float?,
    settings: AppSettings,
) {
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var focusedLocation by remember { mutableStateOf<LatLng?>(null) }
    val visibleZones = state.zones.filterBy(selectedCategory).filterByQuery(query)
    val runPlaceSearch = {
        if (query.trim().length >= 3) onSearchPlaces(query)
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.isBlank()) onClearPlaceSearch()
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text("Buscar aviso, parque, iglesia, tienda...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                when {
                    state.isSearchingPlaces -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    query.isNotEmpty() -> IconButton(onClick = runPlaceSearch) {
                        Icon(Icons.Outlined.Search, contentDescription = "Buscar lugar")
                    }
                }
            },
            supportingText = { Text("Los lugares se buscan solamente al pulsar buscar.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { runPlaceSearch() }),
        )
        if (state.referencePlaces.isNotEmpty()) {
            ReferencePlaceResults(state.referencePlaces) { place ->
                focusedLocation = LatLng(place.latitude, place.longitude)
            }
        }
        CategoryFilters(selectedCategory) { selectedCategory = it }
        Box(Modifier.weight(1f)) {
            ZoneMap(
                zones = visibleZones,
                modifier = Modifier.fillMaxSize(),
                referencePlaces = state.referencePlaces,
                currentLocation = currentLocation,
                focusLocation = focusedLocation,
                onLocateClick = {
                    onRequestLocation { location -> focusedLocation = location }
                },
                headingDegrees = headingDegrees,
                orientToHeading = settings.orientMapWithDevice,
                reduceMotion = settings.reduceMotion,
            )
            if (visibleZones.isEmpty()) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 6.dp,
                ) {
                    Text(
                        "No hay avisos en esta categoría.",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    )
                }
            }
        }
        Text(
            "Mapa y lugares © OpenStreetMap contributors",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!settings.simplifiedInterface) RiskLegend()
    }
}

@Composable
private fun AlertsScreen(zones: List<ZoneDto>, simplifiedInterface: Boolean) {
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val visibleZones = zones.filterBy(selectedCategory).filterByQuery(query)
    Column(Modifier.fillMaxSize()) {
        if (!simplifiedInterface) Card(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Los avisos son comunitarios. Verifica siempre las indicaciones de autoridades y servicios de emergencia.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            label = { Text("Buscar entre los avisos") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Limpiar búsqueda") } }
            } else null,
            singleLine = true,
        )
        CategoryFilters(selectedCategory) { selectedCategory = it }
        if (visibleZones.isEmpty()) {
            EmptyAlerts(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(visibleZones, key = { it.id }) { ReportCard(it) }
            }
        }
    }
}

@Composable
private fun ReferencePlaceResults(
    places: List<ReferencePlace>,
    onPlaceSelected: (ReferencePlace) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Lugares de referencia", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("Toca uno para centrarlo", style = MaterialTheme.typography.labelSmall)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(places, key = { it.id }) { place ->
                Card(
                    modifier = Modifier.width(230.dp).clickable { onPlaceSelected(place) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(place.category, style = MaterialTheme.typography.labelMedium)
                        }
                        Text(place.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(place.address, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilters(selected: String?, onSelected: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelected(null) },
                label = { Text("Todos") },
            )
        }
        items(ReportCategory.entries) { category ->
            FilterChip(
                selected = selected == category.apiValue,
                onClick = { onSelected(category.apiValue) },
                label = { Text(category.label) },
            )
        }
    }
}

@Composable
private fun ReportCard(zone: ZoneDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = riskComposeColor(zone.riskLevel).copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        zone.reportCategory.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = riskComposeColor(zone.riskLevel),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Nivel ${zone.riskLevel}/3",
                    color = riskComposeColor(zone.riskLevel),
                    fontWeight = FontWeight.Bold,
                )
            }
            if (zone.hasPhoto) RemoteReportPhoto(zone.id)
            Text(zone.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(zone.description, style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Área aproximada de ${zone.radiusMeters} m · ${zone.createdAt.substringBefore('T')}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ReportScreen(
    zones: List<ZoneDto>,
    isSubmitting: Boolean,
    currentLocation: LatLng?,
    onRequestLocation: ((LatLng) -> Unit) -> Unit,
    onSubmit: (CreateZoneRequest) -> Unit,
    accidentPresetId: Long?,
    cameraEnabled: Boolean,
    onMessage: (String) -> Unit,
    settings: AppSettings,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var categoryName by rememberSaveable(accidentPresetId) {
        mutableStateOf(if (accidentPresetId == null) ReportCategory.INSECURITY.name else ReportCategory.ACCIDENT.name)
    }
    var riskLevel by rememberSaveable { mutableIntStateOf(1) }
    var title by rememberSaveable(accidentPresetId) {
        mutableStateOf(if (accidentPresetId == null) "" else "Posible accidente o caída")
    }
    var description by rememberSaveable { mutableStateOf("") }
    var radius by rememberSaveable { mutableFloatStateOf(200f) }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var capturedPhoto by remember { mutableStateOf<SafePhoto?>(null) }
    var photoPrivacyConfirmed by rememberSaveable { mutableStateOf(false) }
    var isProcessingPhoto by remember { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (!success || uri == null) return@rememberLauncherForActivityResult
        isProcessingPhoto = true
        coroutineScope.launch {
            runCatching { withContext(Dispatchers.IO) { sanitizeCapturedPhoto(context, uri) } }
                .onSuccess {
                    capturedPhoto = it
                    photoPrivacyConfirmed = false
                }
                .onFailure { onMessage(it.message ?: "No se pudo procesar la fotografía.") }
            isProcessingPhoto = false
        }
    }
    val category = ReportCategory.valueOf(categoryName)
    val location = if (latitude != null && longitude != null) LatLng(latitude!!, longitude!!) else null
    val missingRequirements = buildList {
        if (title.trim().length < 3) add("escribe un título de al menos 3 caracteres")
        if (description.trim().length < 3) add("agrega una descripción de al menos 3 caracteres")
        if (location == null) add("selecciona la ubicación en el mapa")
        if (capturedPhoto != null && !photoPrivacyConfirmed) add("confirma la autorización de la fotografía")
    }
    val isValid = missingRequirements.isEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AddLocationAlt, contentDescription = null, modifier = Modifier.size(32.dp))
                    Text("Crear un aviso", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Describe el riesgo con claridad y marca el lugar aproximado. No incluyas nombres, rostros ni datos personales.")
                }
            }
        }
        item { SectionTitle("1. ¿Qué ocurrió?", "Elige la categoría más cercana al incidente.") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReportCategory.entries) { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { categoryName = option.name },
                        label = { Text(option.label) },
                    )
                }
            }
        }
        item {
            Text(category.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { SectionTitle("2. Nivel de precaución", "Selecciona la intensidad sin exagerar el riesgo.") }
        items((1..3).toList()) { level ->
            SeverityOption(level, selected = riskLevel == level) { riskLevel = level }
        }
        item { SectionTitle("3. Describe el aviso", "Incluye condiciones visibles y cuándo ocurrió.") }
        item {
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 100) title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Título breve") },
                placeholder = { Text("Ej. Cruce con poca visibilidad") },
                supportingText = { Text("${title.length}/100") },
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descripción") },
                placeholder = { Text("Explica qué debe tener en cuenta otra persona...") },
                supportingText = { Text("${description.length}/500") },
                minLines = 4,
            )
        }
        item { SectionTitle("4. Marca el lugar", "Toca el mapa sobre el punto aproximado.") }
        item {
            OutlinedButton(
                onClick = {
                    onRequestLocation { point ->
                        latitude = point.latitude
                        longitude = point.longitude
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.MyLocation, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Usar mi ubicación actual")
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth().height(290.dp).clip(RoundedCornerShape(22.dp)),
            ) {
                ZoneMap(
                    zones = zones,
                    modifier = Modifier.fillMaxSize(),
                    selectedLocation = location,
                    currentLocation = currentLocation,
                    previewRadiusMeters = radius.toInt(),
                    onLocationSelected = {
                        latitude = it.latitude
                        longitude = it.longitude
                    },
                    onLocateClick = {
                        onRequestLocation { point ->
                            latitude = point.latitude
                            longitude = point.longitude
                        }
                    },
                    reduceMotion = settings.reduceMotion,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (location == null) Icons.Outlined.WarningAmber else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = if (location == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (location == null) "Falta seleccionar una ubicación"
                    else "Ubicación: ${formatCoordinate(location.latitude)}, ${formatCoordinate(location.longitude)}",
                )
            }
        }
        item { SectionTitle("5. Tamaño del área", "Ajusta el radio aproximado del aviso.") }
        item {
            Column {
                Text("${radius.toInt()} metros", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..1000f,
                    steps = 18,
                )
            }
        }
        if (cameraEnabled) {
            item { SectionTitle("6. Fotografía opcional", "Registra únicamente el lugar u obstáculo, nunca personas o placas.") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (capturedPhoto == null) {
                        OutlinedButton(
                            onClick = {
                                runCatching { createCameraOutputUri(context) }
                                    .onSuccess {
                                        pendingCameraUri = it
                                        cameraLauncher.launch(it)
                                    }
                                    .onFailure { onMessage("No se pudo abrir la cámara.") }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessingPhoto,
                        ) {
                            Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isProcessingPhoto) "Procesando…" else "Tomar fotografía")
                        }
                    } else {
                        Image(
                            bitmap = capturedPhoto!!.preview,
                            contentDescription = "Vista previa de la evidencia",
                            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Text(
                            "Imagen reducida a ${capturedPhoto!!.sizeBytes / 1024} KB y sin metadatos EXIF o GPS.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = photoPrivacyConfirmed, onCheckedChange = { photoPrivacyConfirmed = it })
                            Text("Confirmo que no muestra rostros, placas ni datos personales y autorizo su revisión y publicación.")
                        }
                        OutlinedButton(
                            onClick = {
                                capturedPhoto = null
                                photoPrivacyConfirmed = false
                            },
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Eliminar fotografía")
                        }
                        Text(
                            "La fotografía se enviará como pendiente y solo será pública después de la revisión de un moderador.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Publica información útil y verificable. Para una emergencia llama a los servicios locales; esta aplicación no los sustituye.")
                }
            }
        }
        item {
            if (!isValid) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        "Para publicar: ${missingRequirements.joinToString("; ")}.",
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            Button(
                onClick = {
                    if (!isValid) {
                        onMessage("Completa los requisitos indicados antes de publicar.")
                        return@Button
                    }
                    onSubmit(
                        CreateZoneRequest(
                            title = title.trim(),
                            description = description.trim(),
                            category = category.apiValue,
                            latitude = location!!.latitude,
                            longitude = location.longitude,
                            radiusMeters = radius.toInt(),
                            riskLevel = riskLevel,
                            photoBase64 = capturedPhoto?.base64,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !isSubmitting,
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.AddLocationAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Publicar aviso")
                }
            }
        }
    }
}

@Composable
private fun SeverityOption(level: Int, selected: Boolean, onClick: () -> Unit) {
    val color = riskComposeColor(level)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) color else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Nivel $level · ${riskLabel(level)}", fontWeight = FontWeight.SemiBold)
                Text(riskDescription(level), style = MaterialTheme.typography.bodySmall)
            }
            if (selected) Icon(Icons.Outlined.CheckCircle, contentDescription = "Seleccionado", tint = color)
        }
    }
}

@Composable
private fun SectionTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RiskLegend() {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            (1..3).forEach { level ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = riskComposeColor(level), shape = RoundedCornerShape(50), modifier = Modifier.size(10.dp)) {}
                    Spacer(Modifier.width(6.dp))
                    Text(riskLabel(level), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun EmptyAlerts(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Sin avisos por ahora", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("Puedes actualizar o crear el primer reporte.", textAlign = TextAlign.Center)
    }
}

private fun List<ZoneDto>.filterBy(category: String?): List<ZoneDto> =
    if (category == null) this else filter { it.category == category }

private fun List<ZoneDto>.filterByQuery(query: String): List<ZoneDto> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return this
    return filter { zone ->
        zone.title.contains(normalized, ignoreCase = true) ||
            zone.description.contains(normalized, ignoreCase = true) ||
            zone.reportCategory.label.contains(normalized, ignoreCase = true)
    }
}

private fun riskComposeColor(level: Int): Color = Color(riskColor(level))

private fun riskLabel(level: Int): String = when (level) {
    1 -> "Precaución"
    2 -> "Alerta"
    else -> "Alto riesgo"
}

private fun riskDescription(level: Int): String = when (level) {
    1 -> "Conviene prestar atención, pero es posible transitar."
    2 -> "Existe un riesgo relevante; considera otra ruta."
    else -> "Evita la zona y sigue indicaciones oficiales."
}

private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.5f", value)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C4C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8BF8C7),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF4D6358),
    secondaryContainer = Color(0xFFCFE9DA),
    tertiary = Color(0xFF3D6373),
    tertiaryContainer = Color(0xFFC1E8FB),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6CDBAC),
    primaryContainer = Color(0xFF005138),
    secondary = Color(0xFFB3CCBE),
    secondaryContainer = Color(0xFF354B40),
    tertiary = Color(0xFFA5CCDF),
    tertiaryContainer = Color(0xFF244C5B),
)

private val HighContrastLightColors = lightColorScheme(
    primary = Color(0xFF00432E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB5F5D6),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF243D32),
    secondaryContainer = Color(0xFFD8F2E3),
    onSecondaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    error = Color(0xFF8C0009),
)

private val HighContrastDarkColors = darkColorScheme(
    primary = Color(0xFF8DFFCB),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF006B4B),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFCFFBE5),
    secondaryContainer = Color(0xFF294A3B),
    onSecondaryContainer = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    error = Color(0xFFFFB4AB),
)

@Composable
private fun AlertaTheme(settings: AppSettings, ambientLux: Float?, content: @Composable () -> Unit) {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val useDarkTheme = if (settings.ambientLightTheme && ambientLux != null) ambientLux < 45f else systemDark
    val colors = when {
        settings.highContrast && useDarkTheme -> HighContrastDarkColors
        settings.highContrast -> HighContrastLightColors
        useDarkTheme -> DarkColors
        else -> LightColors
    }
    val currentDensity = LocalDensity.current
    val fontScale = if (settings.largeText) maxOf(currentDensity.fontScale, 1.22f) else currentDensity.fontScale
    CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, fontScale)) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

@Composable
private fun RemoteReportPhoto(zoneId: Long) {
    val url = approvedPhotoUrl(zoneId)
    val image by produceState<ImageBitmap?>(initialValue = null, url) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(url).openConnection().apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }
                connection.getInputStream().use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }
    image?.let {
        Image(
            bitmap = it,
            contentDescription = "Fotografía aprobada del reporte",
            modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}
