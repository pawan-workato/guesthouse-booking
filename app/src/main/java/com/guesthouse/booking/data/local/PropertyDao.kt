package com.guesthouse.booking.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guesthouse.booking.data.local.entities.PropertyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties WHERE isActive = 1 ORDER BY region ASC, name ASC")
    fun observeAll(): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties ORDER BY region ASC, name ASC")
    fun observeAllIncludingInactive(): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties ORDER BY region ASC, name ASC")
    suspend fun getAllIncludingInactive(): List<PropertyEntity>

    @Query("SELECT * FROM properties WHERE id = :id")
    fun observeById(id: Long): Flow<PropertyEntity?>

    @Query("SELECT * FROM properties WHERE id = :id")
    suspend fun getById(id: Long): PropertyEntity?

    @Query("SELECT COUNT(*) FROM properties")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(properties: List<PropertyEntity>)

    @Insert
    suspend fun insert(property: PropertyEntity): Long

    @Update
    suspend fun update(property: PropertyEntity)

    @Query("UPDATE properties SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
}
