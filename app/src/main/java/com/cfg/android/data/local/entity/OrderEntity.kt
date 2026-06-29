package com.cfg.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val restaurantId: String,
    val tableId: String?,
    val customerName: String?,
    val status: String,
    val notes: String?,
    val totalAmount: Double,
    val clientUuid: String?,
    val itemsJson: String,  // JSON sérialisé
    val createdAt: Long,
    val syncedAt: Long? = null
)
