package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.HandoverNoteEntity
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.HandoverNoteRepository
import com.guesthouse.booking.data.repository.PropertyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HandoverLogUiState(
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HandoverLogViewModel(
    private val handoverNoteRepository: HandoverNoteRepository,
    private val propertyRepository: PropertyRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _selectedPropertyId = MutableStateFlow<Long?>(null)
    private val _uiState = MutableStateFlow(HandoverLogUiState())
    val uiState: StateFlow<HandoverLogUiState> = _uiState.asStateFlow()

    val accessibleProperties: StateFlow<List<PropertyEntity>> = combine(
        propertyRepository.observeActiveProperties(),
        authRepository.session
    ) { props, session ->
        if (session == null) emptyList() else props.filter { session.canAccessProperty(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<HandoverNoteEntity>> = _selectedPropertyId
        .flatMapLatest { handoverNoteRepository.observeNotes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectProperty(propertyId: Long?) {
        _selectedPropertyId.value = propertyId
    }

    fun addNote(propertyId: Long, note: String) {
        viewModelScope.launch {
            _uiState.value = HandoverLogUiState(isSaving = true)
            handoverNoteRepository.addNote(propertyId, note)
                .onSuccess { _uiState.value = HandoverLogUiState(message = "Handover note added") }
                .onFailure { e -> _uiState.value = HandoverLogUiState(error = e.message ?: "Could not save note") }
        }
    }

    fun clearMessage() {
        _uiState.value = HandoverLogUiState()
    }
}
