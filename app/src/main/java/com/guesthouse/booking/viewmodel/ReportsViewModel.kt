package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.ChainOccupancyTotals
import com.guesthouse.booking.data.repository.DayRevenueForecast
import com.guesthouse.booking.data.repository.OccupancyRepository
import com.guesthouse.booking.data.repository.PropertyOccupancyStats
import com.guesthouse.booking.data.repository.PropertyRepository
import com.guesthouse.booking.data.repository.PropertyRevenueStats
import com.guesthouse.booking.data.repository.ReportsRepository
import com.guesthouse.booking.export.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CsvExportPayload(
    val fileName: String,
    val content: String
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val propertyRepository: PropertyRepository,
    private val occupancyRepository: OccupancyRepository,
    private val reportsRepository: ReportsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _reportEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())
    val reportEpochDay: StateFlow<Long> = _reportEpochDay.asStateFlow()

    private val _showWeekAheadForecast = MutableStateFlow(false)
    val showWeekAheadForecast: StateFlow<Boolean> = _showWeekAheadForecast.asStateFlow()

    private val _csvExport = MutableStateFlow<CsvExportPayload?>(null)
    val csvExport: StateFlow<CsvExportPayload?> = _csvExport.asStateFlow()

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

    val revenueStats: StateFlow<List<PropertyRevenueStats>> = combine(
        properties,
        _reportEpochDay
    ) { props, epochDay -> props to epochDay }
        .flatMapLatest { (props, epochDay) ->
            flow {
                emit(
                    reportsRepository.getRevenueStats(
                        props.filter { it.isActive },
                        epochDay
                    )
                )
            }
        }
        .stateIn(viewModelScope, ViewModelSharing, emptyList())

    val weekAheadForecast: StateFlow<List<DayRevenueForecast>> = combine(
        properties,
        _showWeekAheadForecast
    ) { props, enabled -> props to enabled }
        .flatMapLatest { (props, enabled) ->
            flow {
                if (!enabled || props.isEmpty()) {
                    emit(emptyList())
                } else {
                    emit(reportsRepository.getWeekAheadForecast(props.filter { it.isActive }))
                }
            }
        }
        .stateIn(viewModelScope, ViewModelSharing, emptyList())

    val chainTotals: StateFlow<ChainOccupancyTotals?> = occupancyStats
        .map { stats -> if (stats.isEmpty()) null else ChainOccupancyTotals.from(stats) }
        .stateIn(viewModelScope, ViewModelSharing, null)

    val chainRevenueTotal: StateFlow<Double> = revenueStats
        .map { stats -> stats.sumOf { it.totalRevenue } }
        .stateIn(viewModelScope, ViewModelSharing, 0.0)

    fun setReportDate(date: LocalDate) {
        _reportEpochDay.value = date.toEpochDay()
    }

    fun shiftReportDate(days: Long) {
        _reportEpochDay.value = LocalDate.ofEpochDay(_reportEpochDay.value).plusDays(days).toEpochDay()
    }

    fun setShowWeekAheadForecast(enabled: Boolean) {
        _showWeekAheadForecast.value = enabled
    }

    fun prepareBookingsCsvExport() {
        viewModelScope.launch {
            val props = properties.value.associateBy { it.id }
            val rooms = reportsRepository.getAllRooms().associateBy { it.id }
            val bookings = reportsRepository.getAllNonCancelledBookings()
                .filter { it.propertyId in props.keys }
            _csvExport.value = CsvExportPayload(
                fileName = "bookings-export.csv",
                content = CsvExporter.exportBookings(bookings, props, rooms)
            )
        }
    }

    fun prepareGuestsCsvExport() {
        viewModelScope.launch {
            val guests = reportsRepository.getAllGuests()
            _csvExport.value = CsvExportPayload(
                fileName = "guests-export.csv",
                content = CsvExporter.exportGuests(guests)
            )
        }
    }

    fun clearCsvExport() {
        _csvExport.value = null
    }
}
