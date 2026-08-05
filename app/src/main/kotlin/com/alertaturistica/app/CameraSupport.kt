package com.alertaturistica.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

data class SafePhoto(
    val base64: String,
    val preview: ImageBitmap,
    val sizeBytes: Int,
)

fun createCameraOutputUri(context: Context): Uri {
    val directory = File(context.cacheDir, "report_photos").apply { mkdirs() }
    val file = File.createTempFile("capture_", ".jpg", directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun sanitizeCapturedPhoto(context: Context, uri: Uri): SafePhoto {
    val orientation = context.contentResolver.openInputStream(uri).use { input ->
        if (input == null) ExifInterface.ORIENTATION_NORMAL else ExifInterface(input).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }
    val decoded = context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "No se pudo abrir la fotografía." }
        requireNotNull(BitmapFactory.decodeStream(input)) { "La fotografía no es válida." }
    }
    val rotation = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    val oriented = if (rotation == 0f) decoded else {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true)
            .also { decoded.recycle() }
    }
    val scale = minOf(1f, MAX_IMAGE_DIMENSION.toFloat() / maxOf(oriented.width, oriented.height))
    val scaled = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            oriented,
            (oriented.width * scale).roundToInt(),
            (oriented.height * scale).roundToInt(),
            true,
        ).also { oriented.recycle() }
    } else oriented

    var quality = 82
    var bytes: ByteArray
    do {
        bytes = ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            output.toByteArray()
        }
        quality -= 8
    } while (bytes.size > MAX_PHOTO_BYTES && quality >= 42)
    require(bytes.size <= MAX_PHOTO_BYTES) { "La imagen sigue siendo demasiado grande." }
    runCatching { context.contentResolver.delete(uri, null, null) }

    // Al volver a codificar solamente los píxeles se eliminan EXIF, GPS y otros metadatos.
    return SafePhoto(
        base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
        preview = scaled.asImageBitmap(),
        sizeBytes = bytes.size,
    )
}

private const val MAX_IMAGE_DIMENSION = 1280
private const val MAX_PHOTO_BYTES = 500_000
