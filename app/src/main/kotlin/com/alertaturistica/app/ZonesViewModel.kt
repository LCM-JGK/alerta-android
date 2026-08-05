package com.alertaturistica.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ZonesViewModel(private val repository: ZonesRepository) : ViewModel() {
    var uiState by mutableStateOf(AppUiState(zones = repository.cached()))
        private set

    fun refresh() {
        if (uiState.isLoading) return
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            runCatching { withContext(Dispatchers.IO) { repository.refresh() } }
                .onSuccess { zones -> uiState = uiState.copy(zones = zones, isLoading = false) }
                .onFailure {
                    uiState = uiState.copy(
                        isLoading = false,
                        message = "No se pudo actualizar. Se muestran los avisos guardados.",
                    )
                }
        }
    }

    fun submit(request: CreateZoneRequest, onSuccess: () -> Unit) {
        if (uiState.isSubmitting) return
        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true)
            runCatching { withContext(Dispatchers.IO) { repository.create(request) } }
                .onSuccess { created ->
                    uiState = uiState.copy(
                        zones = listOf(created) + uiState.zones.filterNot { it.id == created.id },
                        isSubmitting = false,
                        message = if (created.photoStatus == "PENDING") {
                            "Reporte publicado. La fotografía está pendiente de moderación."
                        } else {
                            "Reporte publicado. Gracias por ayudar a la comunidad."
                        },
                    )
                    onSuccess()
                }
                .onFailure {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        message = "No se pudo publicar. Revisa tu conexión e inténtalo nuevamente.",
                    )
                }
        }
    }

    fun searchPlaces(query: String) {
        if (uiState.isSearchingPlaces) return
        viewModelScope.launch {
            uiState = uiState.copy(isSearchingPlaces = true)
            runCatching { withContext(Dispatchers.IO) { repository.searchPlaces(query) } }
                .onSuccess { places ->
                    uiState = uiState.copy(
                        referencePlaces = places,
                        isSearchingPlaces = false,
                        message = if (places.isEmpty()) "No se encontraron lugares con ese nombre." else null,
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isSearchingPlaces = false,
                        message = error.message ?: "No se pudo buscar el lugar.",
                    )
                }
        }
    }

    fun loadPendingPhotos() {
        if (uiState.isModerating) return
        viewModelScope.launch {
            uiState = uiState.copy(isModerating = true)
            runCatching { withContext(Dispatchers.IO) { repository.pendingPhotos() } }
                .onSuccess { photos -> uiState = uiState.copy(pendingPhotos = photos, isModerating = false) }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isModerating = false,
                        message = error.message ?: "No se pudieron cargar las fotografías pendientes.",
                    )
                }
        }
    }

    fun selectPendingPhoto(zoneId: Long) {
        if (uiState.isModerating) return
        viewModelScope.launch {
            uiState = uiState.copy(
                selectedPendingPhotoId = zoneId,
                moderationPhotoBytes = null,
                isModerating = true,
            )
            runCatching { withContext(Dispatchers.IO) { repository.pendingPhotoContent(zoneId) } }
                .onSuccess { bytes -> uiState = uiState.copy(moderationPhotoBytes = bytes, isModerating = false) }
                .onFailure { error ->
                    uiState = uiState.copy(
                        selectedPendingPhotoId = null,
                        isModerating = false,
                        message = error.message ?: "No se pudo cargar la fotografía.",
                    )
                }
        }
    }

    fun closePendingPhoto() {
        uiState = uiState.copy(selectedPendingPhotoId = null, moderationPhotoBytes = null)
    }

    fun approvePendingPhoto(zoneId: Long) = moderatePhoto(zoneId, approve = true)

    fun rejectPendingPhoto(zoneId: Long) = moderatePhoto(zoneId, approve = false)

    private fun moderatePhoto(zoneId: Long, approve: Boolean) {
        if (uiState.isModerating) return
        viewModelScope.launch {
            uiState = uiState.copy(isModerating = true)
            runCatching {
                withContext(Dispatchers.IO) {
                    if (approve) repository.approvePhoto(zoneId) else repository.rejectPhoto(zoneId)
                    repository.pendingPhotos() to repository.refresh()
                }
            }.onSuccess { (pending, zones) ->
                uiState = uiState.copy(
                    pendingPhotos = pending,
                    zones = zones,
                    selectedPendingPhotoId = null,
                    moderationPhotoBytes = null,
                    isModerating = false,
                    message = if (approve) "Fotografía aprobada y publicada." else "Fotografía rechazada y eliminada.",
                )
            }.onFailure { error ->
                uiState = uiState.copy(
                    isModerating = false,
                    message = error.message ?: "No se pudo completar la moderación.",
                )
            }
        }
    }

    fun clearPlaceSearch() {
        uiState = uiState.copy(referencePlaces = emptyList())
    }

    fun showMessage(message: String) {
        uiState = uiState.copy(message = message)
    }

    fun consumeMessage() {
        uiState = uiState.copy(message = null)
    }
}

class ZonesFactory(private val repository: ZonesRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ZonesViewModel(repository) as T
}
