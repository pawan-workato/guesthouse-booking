package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.sync.NetworkMonitor
import kotlinx.coroutines.flow.Flow

class PropertyRepository(
    private val database: AppDatabase,
    private val networkMonitor: NetworkMonitor,
    private val firestore: FirestoreDataSource = FirestoreDataSource()
) {
    fun observeActiveProperties(): Flow<List<PropertyEntity>> = database.propertyDao().observeAll()

    fun observeAllProperties(): Flow<List<PropertyEntity>> =
        database.propertyDao().observeAllIncludingInactive()

    fun observeProperty(propertyId: Long): Flow<PropertyEntity?> =
        database.propertyDao().observeById(propertyId)

    suspend fun getProperty(propertyId: Long): PropertyEntity? =
        database.propertyDao().getById(propertyId)

    suspend fun createProperty(
        name: String,
        address: String,
        region: String,
        checkInTime: String,
        checkOutTime: String
    ): Result<Long> {
        val trimmedName = name.trim()
        val trimmedAddress = address.trim()
        val trimmedRegion = region.trim()
        if (trimmedName.isBlank()) return Result.failure(IllegalArgumentException("Property name is required"))
        if (trimmedAddress.isBlank()) return Result.failure(IllegalArgumentException("Address is required"))
        if (trimmedRegion.isBlank()) return Result.failure(IllegalArgumentException("Region is required"))
        val entity = PropertyEntity(
            name = trimmedName,
            address = trimmedAddress,
            region = trimmedRegion,
            checkInTime = checkInTime.ifBlank { "15:00" },
            checkOutTime = checkOutTime.ifBlank { "11:00" },
            isActive = true
        )
        val id = database.propertyDao().insert(entity)
        val saved = entity.copy(id = id)
        if (networkMonitor.isCurrentlyOnline()) {
            runCatching { firestore.upsertProperty(saved) }
        }
        return Result.success(id)
    }

    suspend fun updateProperty(property: PropertyEntity): Result<Unit> {
        if (property.name.isBlank()) return Result.failure(IllegalArgumentException("Property name is required"))
        if (property.address.isBlank()) return Result.failure(IllegalArgumentException("Address is required"))
        if (property.region.isBlank()) return Result.failure(IllegalArgumentException("Region is required"))
        val updated = property.copy(
            name = property.name.trim(),
            address = property.address.trim(),
            region = property.region.trim()
        )
        database.propertyDao().update(updated)
        if (networkMonitor.isCurrentlyOnline()) {
            runCatching { firestore.upsertProperty(updated) }
        }
        return Result.success(Unit)
    }

    suspend fun setPropertyActive(propertyId: Long, active: Boolean) {
        database.propertyDao().setActive(propertyId, active)
        val property = database.propertyDao().getById(propertyId) ?: return
        if (networkMonitor.isCurrentlyOnline()) {
            runCatching { firestore.upsertProperty(property) }
        }
    }
}
