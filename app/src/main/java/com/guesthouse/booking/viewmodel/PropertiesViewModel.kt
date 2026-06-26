package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.PropertyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PropertyFormUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedPropertyId: Long? = null
)

class PropertiesViewModel(
    private val propertyRepository: PropertyRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showInactive = MutableStateFlow(false)
    val showInactive: StateFlow<Boolean> = _showInactive.asStateFlow()

    private val _formUiState = MutableStateFlow(PropertyFormUiState())
    val formUiState: StateFlow<PropertyFormUiState> = _formUiState.asStateFlow()

    private val _editProperty = MutableStateFlow<PropertyEntity?>(null)
    val editProperty: StateFlow<PropertyEntity?> = _editProperty.asStateFlow()

    val properties: StateFlow<List<PropertyEntity>> = combine(
        propertyRepository.observeActiveProperties(),
        propertyRepository.observeAllProperties(),
        authRepository.session,
        _searchQuery,
        _showInactive
    ) { active, all, session, query, showInactive ->
        if (session == null) return@combine emptyList()
        val base = if (session.isChainAdmin && showInactive) all else active
        val accessible = if (session.isChainAdmin) base else base.filter { session.canAccessProperty(it.id) }
        if (query.isBlank()) accessible
        else {
            val q = query.trim()
            accessible.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.region.contains(q, ignoreCase = true) ||
                    it.address.contains(q, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setShowInactive(show: Boolean) {
        if (authRepository.currentSession()?.isChainAdmin == true) {
            _showInactive.value = show
        }
    }

    fun loadPropertyForEdit(propertyId: Long) {
        viewModelScope.launch {
            _editProperty.value = propertyRepository.getProperty(propertyId)
        }
    }

    fun clearEditProperty() {
        _editProperty.value = null
    }

    fun createProperty(
        name: String,
        address: String,
        region: String,
        checkInTime: String,
        checkOutTime: String
    ) {
        if (authRepository.currentSession()?.isChainAdmin != true) return
        viewModelScope.launch {
            _formUiState.value = PropertyFormUiState(isSaving = true)
            propertyRepository.createProperty(name, address, region, checkInTime, checkOutTime)
                .onSuccess { id ->
                    _formUiState.value = PropertyFormUiState(savedPropertyId = id)
                }
                .onFailure { error ->
                    _formUiState.value = PropertyFormUiState(errorMessage = error.message)
                }
        }
    }

    fun updateProperty(property: PropertyEntity) {
        if (authRepository.currentSession()?.isChainAdmin != true) return
        viewModelScope.launch {
            _formUiState.value = PropertyFormUiState(isSaving = true)
            propertyRepository.updateProperty(property)
                .onSuccess {
                    _formUiState.value = PropertyFormUiState(savedPropertyId = property.id)
                }
                .onFailure { error ->
                    _formUiState.value = PropertyFormUiState(errorMessage = error.message)
                }
        }
    }

    fun setPropertyActive(propertyId: Long, active: Boolean) {
        if (authRepository.currentSession()?.isChainAdmin != true) return
        viewModelScope.launch {
            propertyRepository.setPropertyActive(propertyId, active)
        }
    }

    fun clearFormState() {
        _formUiState.value = PropertyFormUiState()
    }
}
