package com.alertaturistica.app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ReportCategory(
    val apiValue: String,
    val label: String,
    val description: String,
) {
    INSECURITY("INSEGURIDAD", "Inseguridad", "Robo, acoso o actividad sospechosa"),
    ACCIDENT("ACCIDENTE", "Accidente", "Choque, caída u otro incidente reciente"),
    ROAD_RISK("RIESGO_VIAL", "Riesgo vial", "Bache, obra, semáforo o cruce peligroso"),
    OTHER("OTRO", "Otro riesgo", "Cualquier condición que requiera precaución");

    companion object {
        fun fromApi(value: String): ReportCategory = entries.firstOrNull { it.apiValue == value } ?: OTHER
    }
}

@Serializable
data class ZoneDto(
    val id: Long,
    val title: String,
    val description: String,
    val category: String = "INSEGURIDAD",
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val riskLevel: Int,
    val createdAt: String,
) {
    val reportCategory: ReportCategory get() = ReportCategory.fromApi(category)
}

@Serializable
data class CreateZoneRequest(
    val title: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val riskLevel: Int,
)

@Serializable
data class PlaceSearchDto(
    @SerialName("place_id") val placeId: Long,
    @SerialName("display_name") val displayName: String,
    val lat: String,
    val lon: String,
    val category: String? = null,
    val type: String? = null,
)

data class ReferencePlace(
    val id: Long,
    val name: String,
    val address: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
)

data class AppUiState(
    val zones: List<ZoneDto> = emptyList(),
    val referencePlaces: List<ReferencePlace> = emptyList(),
    val isLoading: Boolean = false,
    val isSearchingPlaces: Boolean = false,
    val isSubmitting: Boolean = false,
    val message: String? = null,
)
