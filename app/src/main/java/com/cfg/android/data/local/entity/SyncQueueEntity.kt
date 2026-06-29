package com.cfg.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val clientUuid: String,
    val eventType: String,
    val payload: String,        // JSON
    val localTimestamp: Long,   // epoch ms
    val status: String = "PENDING",  // PENDING | SYNCED | ERROR
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
