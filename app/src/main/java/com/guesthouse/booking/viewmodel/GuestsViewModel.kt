package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.GuestRepository
import com.guesthouse.booking.data.repository.GuestStayBooking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

data class GuestFormUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedGuestId: Long? = null
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GuestsViewModel(
    private val guestRepository: GuestRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showInactive = MutableStateFlow(false)
    val showInactive: StateFlow<Boolean> = _showInactive.asStateFlow()

    private val _formUiState = MutableStateFlow(GuestFormUiState())
    val formUiState: StateFlow<GuestFormUiState> = _formUiState.asStateFlow()

    private val _editGuest = MutableStateFlow<GuestEntity?>(null)
    val editGuest: StateFlow<GuestEntity?> = _editGuest.asStateFlow()

    private val _editAccessDenied = MutableStateFlow(false)
    val editAccessDenied: StateFlow<Boolean> = _editAccessDenied.asStateFlow()

    private val _canEditGuest = MutableStateFlow(true)
    val canEditGuest: StateFlow<Boolean> = _canEditGuest.asStateFlow()

    private val _viewingGuestId = MutableStateFlow<Long?>(null)
    private val _guestLookup = MutableStateFlow(Triple("", "", ""))

    val similarGuests: StateFlow<List<GuestEntity>> = _guestLookup
        .debounce(400)
        .distinctUntilChanged()
        .flatMapLatest { (name, email, phone) ->
            flow {
                emit(guestRepository.findSimilarGuests(name, email, phone))
            }
        }
        .stateIn(viewModelScope, ViewModelSharing, emptyList())

    val guestStayHistory: StateFlow<List<GuestStayBooking>> = _viewingGuestId
        .flatMapLatest { guestId ->
            if (guestId == null) flowOf(emptyList()) else guestRepository.observeGuestStayHistory(guestId)
        }
        .stateIn(viewModelScope, ViewModelSharing, emptyList())

    val isChainAdmin: StateFlow<Boolean> = authRepository.session
        .map { it?.isChainAdmin == true }
        .stateIn(viewModelScope, ViewModelSharing, false)

    val guests: StateFlow<List<GuestEntity>> = combine(
        guestRepository.observeScopedActiveGuests(),
        guestRepository.observeScopedAllGuests(),
        _searchQuery,
        _showInactive
    ) { active, all, query, showInactive ->
        val base = if (showInactive) all else active
        if (query.isBlank()) base
        else {
            val q = query.trim()
            base.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.email.contains(q, ignoreCase = true) ||
                    it.phone.contains(q, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, ViewModelSharing, emptyList())

    val activeGuests: StateFlow<List<GuestEntity>> = guestRepository.observeScopedActiveGuests()
        .stateIn(viewModelScope, ViewModelSharing, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setShowInactive(show: Boolean) {
        _showInactive.value = show
    }

    fun loadGuestForEdit(guestId: Long) {
        _viewingGuestId.value = guestId
        viewModelScope.launch {
            val guest = guestRepository.getGuest(guestId)
            _editAccessDenied.value = guest == null
            _canEditGuest.value = guest != null && guestRepository.canEditGuest(guestId)
            _editGuest.value = guest
        }
    }

    fun clearEditGuest() {
        _viewingGuestId.value = null
        _editGuest.value = null
        _editAccessDenied.value = false
        _canEditGuest.value = true
    }

    fun createGuest(name: String, email: String, phone: String, notes: String, preferences: String = "") {
        viewModelScope.launch {
            _formUiState.value = GuestFormUiState(isSaving = true)
            guestRepository.createGuest(name, email, phone, notes, preferences)
                .onSuccess { id -> _formUiState.value = GuestFormUiState(savedGuestId = id) }
                .onFailure { error -> _formUiState.value = GuestFormUiState(errorMessage = error.message) }
        }
    }

    fun updateGuest(guest: GuestEntity) {
        viewModelScope.launch {
            _formUiState.value = GuestFormUiState(isSaving = true)
            guestRepository.updateGuest(guest)
                .onSuccess { _formUiState.value = GuestFormUiState(savedGuestId = guest.id) }
                .onFailure { error -> _formUiState.value = GuestFormUiState(errorMessage = error.message) }
        }
    }

    fun setGuestActive(guestId: Long, active: Boolean) {
        viewModelScope.launch {
            guestRepository.setGuestActive(guestId, active)
        }
    }

    fun updateGuestLookup(name: String, email: String, phone: String) {
        _guestLookup.value = Triple(name, email, phone)
    }

    fun clearGuestLookup() {
        _guestLookup.value = Triple("", "", "")
    }

    fun clearFormState() {
        _formUiState.value = GuestFormUiState()
    }
}
