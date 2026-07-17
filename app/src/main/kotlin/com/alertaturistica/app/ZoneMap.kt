package com.alertaturistica.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.fillOutlineColor
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ZoneMap(
    zones: List<ZoneDto>,
    modifier: Modifier = Modifier,
    referencePlaces: List<ReferencePlace> = emptyList(),
    selectedLocation: LatLng? = null,
    currentLocation: LatLng? = null,
    previewRadiusMeters: Int? = null,
    focusLocation: LatLng? = null,
    onLocationSelected: ((LatLng) -> Unit)? = null,
    onLocateClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestZones by rememberUpdatedState(zones)
    val latestPlaces by rememberUpdatedState(referencePlaces)
    val latestSelection by rememberUpdatedState(selectedLocation)
    val latestCurrentLocation by rememberUpdatedState(currentLocation)
    val latestPreviewRadius by rememberUpdatedState(previewRadiusMeters)
    val latestFocusLocation by rememberUpdatedState(focusLocation)
    val latestClickHandler by rememberUpdatedState(onLocationSelected)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).also { it.onCreate(null) }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        mapView.onStart()
        mapView.onResume()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(map, zones, referencePlaces, selectedLocation, currentLocation, previewRadiusMeters) {
        map?.getStyle { style ->
            renderZones(style, zones)
            renderReferencePlaces(style, referencePlaces)
            renderCurrentLocation(style, currentLocation)
            renderSelection(style, selectedLocation, previewRadiusMeters)
        }
    }

    LaunchedEffect(map, focusLocation) {
        val target = focusLocation ?: return@LaunchedEffect
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15.0), 500)
    }

    LaunchedEffect(map, selectedLocation, previewRadiusMeters) {
        val selected = selectedLocation ?: return@LaunchedEffect
        val radius = previewRadiusMeters ?: return@LaunchedEffect
        mapView.post { map?.let { fitSelectionArea(it, selected, radius) } }
    }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    getMapAsync { readyMap ->
                        map = readyMap
                        readyMap.uiSettings.isZoomGesturesEnabled = true
                        readyMap.uiSettings.isScrollGesturesEnabled = true
                        readyMap.uiSettings.isRotateGesturesEnabled = true
                        readyMap.addOnMapClickListener { point ->
                            latestClickHandler?.invoke(point)
                            latestClickHandler != null
                        }
                        readyMap.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { style ->
                            renderZones(style, latestZones)
                            renderReferencePlaces(style, latestPlaces)
                            renderCurrentLocation(style, latestCurrentLocation)
                            renderSelection(style, latestSelection, latestPreviewRadius)
                            centerMap(
                                readyMap,
                                latestFocusLocation,
                                latestCurrentLocation,
                                latestSelection,
                                latestZones.firstOrNull(),
                            )
                            latestSelection?.let { selected ->
                                latestPreviewRadius?.let { radius ->
                                    mapView.post { fitSelectionArea(readyMap, selected, radius) }
                                }
                            }
                        }
                    }
                }
            },
        )
        MapControls(
            map = map,
            onLocateClick = onLocateClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        )
    }
}

@Composable
private fun MapControls(
    map: MapLibreMap?,
    onLocateClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallFloatingActionButton(onClick = { map?.animateCamera(CameraUpdateFactory.zoomIn(), 250) }) {
            Icon(Icons.Outlined.Add, contentDescription = "Acercar mapa")
        }
        SmallFloatingActionButton(onClick = { map?.animateCamera(CameraUpdateFactory.zoomOut(), 250) }) {
            Icon(Icons.Outlined.Remove, contentDescription = "Alejar mapa")
        }
        if (onLocateClick != null) {
            SmallFloatingActionButton(onClick = onLocateClick) {
                Icon(Icons.Outlined.MyLocation, contentDescription = "Usar mi ubicación")
            }
        }
    }
}

private fun renderZones(style: Style, zones: List<ZoneDto>) {
    (1..3).forEach { risk ->
        style.removeLayer("zone-points-$risk")
        style.removeLayer("zone-areas-$risk")
        style.removeSource("zone-points-source-$risk")
        style.removeSource("zone-areas-source-$risk")
    }

    (1..3).forEach { risk ->
        val matching = zones.filter { it.riskLevel.coerceIn(1, 3) == risk }
        if (matching.isEmpty()) return@forEach
        val color = riskColor(risk)
        val areaSourceId = "zone-areas-source-$risk"
        val pointSourceId = "zone-points-source-$risk"
        val areaFeatures = matching.map { Feature.fromGeometry(radiusPolygon(it)) }
        val pointFeatures = matching.map { zone ->
            Feature.fromGeometry(Point.fromLngLat(zone.longitude, zone.latitude)).apply {
                addStringProperty("title", zone.title)
                addStringProperty("category", zone.category)
            }
        }

        style.addSource(GeoJsonSource(areaSourceId, FeatureCollection.fromFeatures(areaFeatures)))
        style.addLayer(
            FillLayer("zone-areas-$risk", areaSourceId).withProperties(
                fillColor(color),
                fillOpacity(0.26f),
                fillOutlineColor(color),
            ),
        )
        style.addSource(GeoJsonSource(pointSourceId, FeatureCollection.fromFeatures(pointFeatures)))
        style.addLayer(
            CircleLayer("zone-points-$risk", pointSourceId).withProperties(
                circleColor(color),
                circleRadius(7f),
                circleStrokeColor(android.graphics.Color.WHITE),
                circleStrokeWidth(2f),
            ),
        )
    }
}

private fun renderReferencePlaces(style: Style, places: List<ReferencePlace>) {
    style.removeLayer(REFERENCE_LAYER)
    style.removeSource(REFERENCE_SOURCE)
    if (places.isEmpty()) return
    val features = places.map { place ->
        Feature.fromGeometry(Point.fromLngLat(place.longitude, place.latitude)).apply {
            addStringProperty("name", place.name)
            addStringProperty("category", place.category)
        }
    }
    style.addSource(GeoJsonSource(REFERENCE_SOURCE, FeatureCollection.fromFeatures(features)))
    style.addLayer(
        CircleLayer(REFERENCE_LAYER, REFERENCE_SOURCE).withProperties(
            circleColor(android.graphics.Color.rgb(0, 121, 107)),
            circleRadius(8f),
            circleStrokeColor(android.graphics.Color.WHITE),
            circleStrokeWidth(3f),
        ),
    )
}

private fun renderCurrentLocation(style: Style, location: LatLng?) {
    style.removeLayer(CURRENT_LOCATION_LAYER)
    style.removeLayer(CURRENT_LOCATION_HALO_LAYER)
    style.removeSource(CURRENT_LOCATION_SOURCE)
    if (location == null) return
    val feature = Feature.fromGeometry(Point.fromLngLat(location.longitude, location.latitude))
    style.addSource(GeoJsonSource(CURRENT_LOCATION_SOURCE, feature))
    style.addLayer(
        CircleLayer(CURRENT_LOCATION_HALO_LAYER, CURRENT_LOCATION_SOURCE).withProperties(
            circleColor(android.graphics.Color.argb(75, 25, 118, 210)),
            circleRadius(18f),
        ),
    )
    style.addLayer(
        CircleLayer(CURRENT_LOCATION_LAYER, CURRENT_LOCATION_SOURCE).withProperties(
            circleColor(android.graphics.Color.rgb(25, 118, 210)),
            circleRadius(7f),
            circleStrokeColor(android.graphics.Color.WHITE),
            circleStrokeWidth(3f),
        ),
    )
}

private fun renderSelection(style: Style, selected: LatLng?, radiusMeters: Int?) {
    style.removeLayer(SELECTION_LAYER)
    style.removeLayer(SELECTION_AREA_LAYER)
    style.removeSource(SELECTION_SOURCE)
    style.removeSource(SELECTION_AREA_SOURCE)
    if (selected == null) return
    if (radiusMeters != null) {
        val area = Feature.fromGeometry(radiusPolygon(selected.latitude, selected.longitude, radiusMeters))
        style.addSource(GeoJsonSource(SELECTION_AREA_SOURCE, area))
        style.addLayer(
            FillLayer(SELECTION_AREA_LAYER, SELECTION_AREA_SOURCE).withProperties(
                fillColor(android.graphics.Color.rgb(0, 94, 184)),
                fillOpacity(0.24f),
                fillOutlineColor(android.graphics.Color.rgb(0, 62, 130)),
            ),
        )
    }
    val feature = Feature.fromGeometry(Point.fromLngLat(selected.longitude, selected.latitude))
    style.addSource(GeoJsonSource(SELECTION_SOURCE, feature))
    style.addLayer(
        CircleLayer(SELECTION_LAYER, SELECTION_SOURCE).withProperties(
            circleColor(android.graphics.Color.rgb(0, 94, 184)),
            circleRadius(10f),
            circleStrokeColor(android.graphics.Color.WHITE),
            circleStrokeWidth(3f),
        ),
    )
}

private fun centerMap(
    map: MapLibreMap,
    focus: LatLng?,
    current: LatLng?,
    selected: LatLng?,
    zone: ZoneDto?,
) {
    val target = focus ?: selected ?: current ?: zone?.let { LatLng(it.latitude, it.longitude) } ?: DEFAULT_LOCATION
    map.cameraPosition = CameraPosition.Builder()
        .target(target)
        .zoom(if (focus != null || selected != null || current != null || zone != null) 13.0 else 10.5)
        .build()
}

private fun fitSelectionArea(map: MapLibreMap, selected: LatLng, radiusMeters: Int) {
    val latitudeDelta = radiusMeters / 111_320.0
    val longitudeDelta = radiusMeters /
        (111_320.0 * cos(Math.toRadians(selected.latitude)).coerceAtLeast(0.2))
    val bounds = LatLngBounds.Builder()
        .include(LatLng(selected.latitude - latitudeDelta, selected.longitude - longitudeDelta))
        .include(LatLng(selected.latitude + latitudeDelta, selected.longitude + longitudeDelta))
        .build()
    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 72), 350)
}

private fun radiusPolygon(zone: ZoneDto): Polygon =
    radiusPolygon(zone.latitude, zone.longitude, zone.radiusMeters)

private fun radiusPolygon(latitudeValue: Double, longitudeValue: Double, radiusMeters: Int): Polygon {
    val latitude = Math.toRadians(latitudeValue)
    val longitude = Math.toRadians(longitudeValue)
    val angularDistance = radiusMeters.toDouble() / EARTH_RADIUS_METERS
    val ring = (0..CIRCLE_SEGMENTS).map { step ->
        val bearing = 2.0 * Math.PI * step / CIRCLE_SEGMENTS
        val targetLatitude = asin(
            sin(latitude) * cos(angularDistance) +
                cos(latitude) * sin(angularDistance) * cos(bearing),
        )
        val targetLongitude = longitude + atan2(
            sin(bearing) * sin(angularDistance) * cos(latitude),
            cos(angularDistance) - sin(latitude) * sin(targetLatitude),
        )
        Point.fromLngLat(Math.toDegrees(targetLongitude), Math.toDegrees(targetLatitude))
    }
    return Polygon.fromLngLats(listOf(ring))
}

fun riskColor(risk: Int): Int = when (risk) {
    1 -> android.graphics.Color.rgb(255, 183, 3)
    2 -> android.graphics.Color.rgb(245, 124, 0)
    else -> android.graphics.Color.rgb(198, 40, 40)
}

private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val CIRCLE_SEGMENTS = 64
private const val SELECTION_SOURCE = "report-selection-source"
private const val SELECTION_LAYER = "report-selection-layer"
private const val SELECTION_AREA_SOURCE = "report-selection-area-source"
private const val SELECTION_AREA_LAYER = "report-selection-area-layer"
private const val REFERENCE_SOURCE = "reference-places-source"
private const val REFERENCE_LAYER = "reference-places-layer"
private const val CURRENT_LOCATION_SOURCE = "current-location-source"
private const val CURRENT_LOCATION_HALO_LAYER = "current-location-halo-layer"
private const val CURRENT_LOCATION_LAYER = "current-location-layer"
private val DEFAULT_LOCATION = LatLng(19.4326, -99.1332)
