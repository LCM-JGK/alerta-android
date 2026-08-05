package com.alertaturistica.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.sqrt

data class ImpactEvent(val id: Long, val forceG: Float)

@Composable
fun rememberHeadingDegrees(enabled: Boolean): Float? {
    val context = LocalContext.current
    var heading by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            heading = null
            return@DisposableEffect onDispose { }
        }
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationSensor == null) {
            heading = null
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val measured = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                val previous = heading
                if (previous == null || angularDistance(previous, measured) >= 2f) heading = measured
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    return heading
}

@Composable
fun rememberAmbientLightLux(enabled: Boolean): Float? {
    val context = LocalContext.current
    var lux by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            lux = null
            return@DisposableEffect onDispose { }
        }
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (sensor == null) {
            lux = null
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val measured = event.values.firstOrNull() ?: return
                if (lux == null || abs((lux ?: measured) - measured) >= 3f) lux = measured
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { manager.unregisterListener(listener) }
    }
    return lux
}

@Composable
fun rememberImpactEvent(enabled: Boolean): ImpactEvent? {
    val context = LocalContext.current
    var impact by remember { mutableStateOf<ImpactEvent?>(null) }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            impact = null
            return@DisposableEffect onDispose { }
        }
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) return@DisposableEffect onDispose { }
        val listener = object : SensorEventListener {
            private var lastImpactAt = 0L

            override fun onSensorChanged(event: SensorEvent) {
                val force = sqrt(
                    event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2],
                ) / SensorManager.GRAVITY_EARTH
                val now = System.currentTimeMillis()
                if (force >= IMPACT_THRESHOLD_G && now - lastImpactAt >= IMPACT_COOLDOWN_MS) {
                    lastImpactAt = now
                    impact = ImpactEvent(now, force)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { manager.unregisterListener(listener) }
    }
    return impact
}

private fun angularDistance(first: Float, second: Float): Float {
    val difference = abs(first - second) % 360f
    return minOf(difference, 360f - difference)
}

private const val IMPACT_THRESHOLD_G = 2.7f
private const val IMPACT_COOLDOWN_MS = 15_000L
