package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Keeps shared flows alive for the ViewModel lifetime — avoids empty-state flash on tab switch. */
val ViewModelSharing: SharingStarted = SharingStarted.WhileSubscribed(stopTimeoutMillis = Long.MAX_VALUE)

fun <T> ViewModel.stateInViewModelScope(
    flow: Flow<T>,
    initialValue: T
): StateFlow<T> = flow.stateIn(viewModelScope, ViewModelSharing, initialValue)

fun <K, T> ViewModel.cachedStateFlow(
    cache: MutableMap<K, StateFlow<T>>,
    key: K,
    initialValue: T,
    flow: () -> Flow<T>
): StateFlow<T> = cache.getOrPut(key) { stateInViewModelScope(flow(), initialValue) }
