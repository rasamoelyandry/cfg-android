package com.cfg.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey val id: String,
    val restaurantId: String,
    val categoryId: String,
    val categoryName: String,
    val name: String,
    val description: String?,
    val price: Double,
    val imageUrl: String?,
    val isAvailable: Boolean,
    val sortOrder: Int,
    val modifiersJson: String = "[]",
    val cachedAt: Long = System.currentTimeMillis()
)
