package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.PropertyEntity
import kotlinx.coroutines.flow.Flow

class PropertyRepository(private val database: AppDatabase) {
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
        val id = database.propertyDao().insert(
            PropertyEntity(
                name = trimmedName,
                address = trimmedAddress,
                region = trimmedRegion,
                checkInTime = checkInTime.ifBlank { "15:00" },
                checkOutTime = checkOutTime.ifBlank { "11:00" },
                isActive = true
            )
        )
        return Result.success(id)
    }

    suspend fun updateProperty(property: PropertyEntity): Result<Unit> {
        if (property.name.isBlank()) return Result.failure(IllegalArgumentException("Property name is required"))
        if (property.address.isBlank()) return Result.failure(IllegalArgumentException("Address is required"))
        if (property.region.isBlank()) return Result.failure(IllegalArgumentException("Region is required"))
        database.propertyDao().update(
            property.copy(
                name = property.name.trim(),
                address = property.address.trim(),
                region = property.region.trim()
            )
        )
        return Result.success(Unit)
    }

    suspend fun setPropertyActive(propertyId: Long, active: Boolean) {
        database.propertyDao().setActive(propertyId, active)
    }
}
