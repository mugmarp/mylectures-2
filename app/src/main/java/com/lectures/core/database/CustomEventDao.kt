package com.lectures.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lectures.features.timetable.domain.CustomEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomEventDao {
    @Query("SELECT * FROM custom_events WHERE isDeleted = 0 ORDER BY dayOfWeek, startTime")
    fun getAll(): Flow<List<CustomEvent>>

    @Query("SELECT * FROM custom_events WHERE isDeleted = 0 ORDER BY dayOfWeek, startTime")
    suspend fun getAllActiveSync(): List<CustomEvent>

    @Query("SELECT * FROM custom_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CustomEvent?

    @Upsert
    suspend fun upsert(event: CustomEvent): Long

    @Upsert
    suspend fun upsertAll(events: List<CustomEvent>)

    @Query("UPDATE custom_events SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM custom_events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM custom_events WHERE clientUuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): CustomEvent?

    @Query("SELECT * FROM custom_events")
    suspend fun getAllRawSync(): List<CustomEvent>

    @Query("SELECT * FROM custom_events WHERE ownerId = :userId OR ownerId IS NULL")
    suspend fun getAllRawSyncForUser(userId: String): List<CustomEvent>

    @Query("UPDATE custom_events SET ownerId = :userId WHERE ownerId IS NULL")
    suspend fun promoteGuestData(userId: String)

    @Query("DELETE FROM custom_events WHERE isDeleted = 1 AND updatedAt < :cutoffTimestamp")
    suspend fun purgeTombstones(cutoffTimestamp: Long): Int

    @Query("UPDATE custom_events SET isDeleted = 1, updatedAt = :timestamp WHERE isDeleted = 0")
    suspend fun softDeleteAll(timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE custom_events SET ownerId = NULL WHERE ownerId = :userId AND isDeleted = 0")
    suspend fun demoteUserDataToGuest(userId: String)

    @Query("DELETE FROM custom_events WHERE ownerId = :userId AND isDeleted = 1")
    suspend fun purgeUserDeletedTombstones(userId: String)

    @Query("DELETE FROM custom_events WHERE ownerId = :userId")
    suspend fun purgeUserData(userId: String)

    @Query("DELETE FROM custom_events")
    suspend fun deleteAll()
}
