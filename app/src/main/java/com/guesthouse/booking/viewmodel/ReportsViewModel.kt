package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.ChainOccupancyTotals
import com.guesthouse.booking.data.repository.OccupancyRepository
import com.guesthouse.booking.data.repository.PropertyOccupancyStats
import com.guesthouse.booking.data.repository.PropertyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val propertyRepository: PropertyRepository,
    private val occupancyRepository: OccupancyRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _reportEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())
    val reportEpochDay: StateFlow<Long> = _reportEpochDay.asStateFlow()

    val reportDate: StateFlow<LocalDate> = _reportEpochDay
        .map { LocalDate.ofEpochDay(it) }
        .stateIn(viewModelScope, ViewModelSharing, LocalDate.now())

    val properties: StateFlow<List<PropertyEntity>> = combine(
        propertyRepository.observeActiveProperties(),
        authRepository.session
    ) { active, session ->
        if (session?.isChainAdmin != true) emptyList()
        else active
    }.stateIn(viewModelScope, ViewModelSharing, emptyList())

    val occupancyStats: StateFlow<List<PropertyOccupancyStats>> = combine(
        properties,
        _reportEpochDay
    ) { props, epochDay -> props to epochDay }
        .flatMapLatest { (props, epochDay) ->
            flow {
                emit(
                    occupancyRepository.getStatsForProperties(
                        props.filter { it.isActive },
                        epochDay
                    )
                )
            }
        }
        .stateIn(viewModelScope, ViewModelSharing, emptyList())

    val chainTotals: StateFlow<ChainOccupancyTotals?> = occupancyStats
        .map { stats -> if (stats.isEmpty()) null else ChainOccupancyTotals.from(stats) }
        .stateIn(viewModelScope, ViewModelSharing, null)

    fun setReportDate(date: LocalDate) {
        _reportEpochDay.value = date.toEpochDay()
    }

    fun shiftReportDate(days: Long) {
        _reportEpochDay.value = LocalDate.ofEpochDay(_reportEpochDay.value).plusDays(days).toEpochDay()
    }
}
