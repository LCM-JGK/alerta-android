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
                        message = "Reporte publicado. Gracias por ayudar a la comunidad.",
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
