package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PropertiesViewModel(
    private val repository: BookingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val properties: StateFlow<List<PropertyEntity>> = combine(
        repository.observeProperties(),
        authRepository.session,
        _searchQuery
    ) { properties, session, query ->
        if (session == null) return@combine emptyList()
        val accessible = properties.filter { session.canAccessProperty(it.id) }
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
}
