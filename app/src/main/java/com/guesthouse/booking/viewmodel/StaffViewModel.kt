package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.PropertyRepository
import com.guesthouse.booking.data.repository.StaffRepository
import com.guesthouse.booking.data.repository.StaffWithAssignments
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StaffFormUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedStaffId: Long? = null
)

class StaffViewModel(
    private val staffRepository: StaffRepository,
    private val propertyRepository: PropertyRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showInactive = MutableStateFlow(false)
    val showInactive: StateFlow<Boolean> = _showInactive.asStateFlow()

    private val _formUiState = MutableStateFlow(StaffFormUiState())
    val formUiState: StateFlow<StaffFormUiState> = _formUiState.asStateFlow()

    private val _editStaff = MutableStateFlow<StaffWithAssignments?>(null)
    val editStaff: StateFlow<StaffWithAssignments?> = _editStaff.asStateFlow()

    val activeProperties: StateFlow<List<PropertyEntity>> =
        propertyRepository.observeActiveProperties()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val staffList: StateFlow<List<StaffWithAssignments>> = combine(
        staffRepository.observeStaff(false),
        staffRepository.observeStaff(true),
        _searchQuery,
        _showInactive
    ) { active, all, query, showInactive ->
        val base = if (showInactive) all else active
        if (query.isBlank()) base
        else {
            val q = query.trim()
            base.filter {
                it.staff.displayName.contains(q, ignoreCase = true) ||
                    it.staff.email.contains(q, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun setShowInactive(show: Boolean) {
        if (authRepository.currentSession()?.isChainAdmin == true) {
            _showInactive.value = show
        }
    }

    fun loadStaffForEdit(staffId: Long) {
        viewModelScope.launch { _editStaff.value = staffRepository.getStaff(staffId) }
    }

    fun clearEditStaff() { _editStaff.value = null }

    fun createManager(email: String, displayName: String, password: String, propertyIds: Set<Long>) {
        if (authRepository.currentSession()?.isChainAdmin != true) return
        viewModelScope.launch {
            _formUiState.value = StaffFormUiState(isSaving = true)
            staffRepository.createManager(email, displayName, password, propertyIds)
                .onSuccess { id -> _formUiState.value = StaffFormUiState(savedStaffId = id) }
                .onFailure { error -> _formUiState.value = StaffFormUiState(errorMessage = error.message) }
        }
    }

    fun updateStaff(staffId: Long, email: String, displayName: String, propertyIds: Set<Long>) {
        if (authRepository.currentSession()?.isChainAdmin != true) return
        viewModelScope.launch {
            _formUiState.value = StaffFormUiState(isSaving = true)
            staffRepository.updateStaff(staffId, email, displayName, propertyIds)
                .onSuccess { _formUiState.value = StaffFormUiState(savedStaffId = staffId) }
                .onFailure { error -> _formUiState.value = StaffFormUiState(errorMessage = error.message) }
        }
    }

    fun setStaffActive(staffId: Long, active: Boolean) {
        if (authRepository.currentSession()?.isChainAdmin != true) return
        viewModelScope.launch {
            staffRepository.setStaffActive(staffId, active)
                .onFailure { error -> _formUiState.value = StaffFormUiState(errorMessage = error.message) }
        }
    }

    fun clearFormState() { _formUiState.value = StaffFormUiState() }

    fun roleLabel(role: String): String = when (role) {
        StaffRole.CHAIN_ADMIN.name -> "Chain admin"
        StaffRole.PROPERTY_MANAGER.name -> "Property manager"
        else -> role
    }
}
