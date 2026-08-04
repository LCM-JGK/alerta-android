package com.alertaturistica.app

import com.alertaturistica.app.db.AlertaDatabase
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.Locale

class ZonesRepository(
    private val database: AlertaDatabase,
    private val sessionStore: SecureSessionStore,
) {
    private val placeSearchMutex = Mutex()
    private val placeSearchCache = mutableMapOf<String, List<ReferencePlace>>()
    private var lastPlaceRequestAt = 0L

    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun refresh(): List<ZoneDto> {
        val zones: List<ZoneDto> = client.get("$BASE_URL/api/zones").body()
        database.transaction {
            database.zoneQueries.deleteAll()
            zones.forEach(::cache)
        }
        return zones
    }

    suspend fun create(request: CreateZoneRequest): ZoneDto {
        val token = sessionStore.readToken() ?: error("Inicia sesión para publicar un aviso.")
        val zone: ZoneDto = client.post("$BASE_URL/api/zones") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        database.zoneQueries.insert(
            zone.id,
            zone.title,
            zone.description,
            zone.category,
            zone.latitude,
            zone.longitude,
            zone.radiusMeters.toLong(),
            zone.riskLevel.toLong(),
            zone.createdAt,
        )
        return zone
    }

    suspend fun searchPlaces(rawQuery: String): List<ReferencePlace> = placeSearchMutex.withLock {
        val query = rawQuery.trim()
        require(query.length >= 3) { "Escribe al menos 3 caracteres para buscar un lugar." }
        val cacheKey = query.lowercase(Locale.ROOT)
        placeSearchCache[cacheKey]?.let { return@withLock it }

        val elapsed = System.currentTimeMillis() - lastPlaceRequestAt
        if (elapsed < NOMINATIM_MIN_INTERVAL_MS) delay(NOMINATIM_MIN_INTERVAL_MS - elapsed)

        val response: List<PlaceSearchDto> = client.get("$NOMINATIM_BASE_URL/search") {
            header(HttpHeaders.UserAgent, NOMINATIM_USER_AGENT)
            header(HttpHeaders.AcceptLanguage, "es-MX,es;q=0.9")
            parameter("q", query)
            parameter("format", "jsonv2")
            parameter("limit", 6)
            parameter("addressdetails", 0)
        }.body()
        lastPlaceRequestAt = System.currentTimeMillis()

        response.mapNotNull { place ->
            val latitude = place.lat.toDoubleOrNull()
            val longitude = place.lon.toDoubleOrNull()
            if (latitude == null || longitude == null) null else ReferencePlace(
                id = place.placeId,
                name = place.displayName.substringBefore(',').ifBlank { "Lugar de referencia" },
                address = place.displayName,
                category = place.categoryLabel(),
                latitude = latitude,
                longitude = longitude,
            )
        }.also { placeSearchCache[cacheKey] = it }
    }

    fun cached(): List<ZoneDto> = database.zoneQueries.selectAll().executeAsList().map {
        ZoneDto(
            id = it.id,
            title = it.title,
            description = it.description,
            category = it.category,
            latitude = it.latitude,
            longitude = it.longitude,
            radiusMeters = it.radius_meters.toInt(),
            riskLevel = it.risk_level.toInt(),
            createdAt = it.created_at,
        )
    }

    private fun cache(zone: ZoneDto) {
        database.zoneQueries.insert(
            zone.id,
            zone.title,
            zone.description,
            zone.category,
            zone.latitude,
            zone.longitude,
            zone.radiusMeters.toLong(),
            zone.riskLevel.toLong(),
            zone.createdAt,
        )
    }

    private companion object {
        const val BASE_URL = "https://alerta-backend-production.up.railway.app"
        const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org"
        const val NOMINATIM_USER_AGENT =
            "AlertaLocalAndroid/1.0 (https://github.com/LCM-JGK/alerta-android)"
        const val NOMINATIM_MIN_INTERVAL_MS = 1_100L
    }
}

private fun PlaceSearchDto.categoryLabel(): String = when (category) {
    "shop" -> "Tienda"
    "leisure" -> "Parque o recreación"
    "amenity" -> when (type) {
        "place_of_worship" -> "Iglesia o templo"
        "school", "college", "university" -> "Centro educativo"
        "hospital", "clinic", "pharmacy" -> "Servicio de salud"
        else -> "Punto de referencia"
    }
    "tourism" -> "Sitio turístico"
    "place" -> "Localidad"
    else -> "Lugar de referencia"
}
