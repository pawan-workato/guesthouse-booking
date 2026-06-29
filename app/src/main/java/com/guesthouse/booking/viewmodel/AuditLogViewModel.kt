package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.AuditEventEntity
import com.guesthouse.booking.data.repository.AuditRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AuditLogViewModel(
    auditRepository: AuditRepository
) : ViewModel() {
    val events: StateFlow<List<AuditEventEntity>> = auditRepository.observeEvents()
        .stateIn(viewModelScope, ViewModelSharing, emptyList())
}
