package com.cfg.android.data.local.dao

import androidx.room.*
import com.cfg.android.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = :status WHERE clientUuid = :uuid")
    suspend fun updateStatus(uuid: String, status: String)

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1 WHERE clientUuid = :uuid")
    suspend fun incrementRetry(uuid: String)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    suspend fun pendingCount(): Int

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun clearSynced()
}
