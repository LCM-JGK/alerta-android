package com.alertaturistica.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import org.maplibre.android.geometry.LatLng

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
fun requestCurrentLocation(
    context: Context,
    onSuccess: (LatLng) -> Unit,
    onError: (String) -> Unit,
) {
    if (!hasLocationPermission(context)) {
        onError("Se necesita permiso de ubicación para usar esta función.")
        return
    }

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = when {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }
    if (provider == null) {
        onError("Activa la ubicación del dispositivo e inténtalo nuevamente.")
        return
    }

    runCatching {
        LocationManagerCompat.getCurrentLocation(
            manager,
            provider,
            CancellationSignal(),
            ContextCompat.getMainExecutor(context),
        ) { location ->
            if (location == null) {
                onError("No fue posible obtener tu ubicación actual.")
            } else {
                onSuccess(LatLng(location.latitude, location.longitude))
            }
        }
    }.onFailure {
        onError("No fue posible obtener tu ubicación actual.")
    }
}
