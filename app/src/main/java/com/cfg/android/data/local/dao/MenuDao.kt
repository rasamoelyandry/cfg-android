package com.cfg.android.data.local.dao

import androidx.room.*
import com.cfg.android.data.local.entity.MenuItemEntity

@Dao
interface MenuDao {
    @Query("SELECT * FROM menu_items WHERE restaurantId = :restaurantId AND isAvailable = 1 ORDER BY categoryId, sortOrder")
    suspend fun getMenuItems(restaurantId: String): List<MenuItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MenuItemEntity>)

    @Query("DELETE FROM menu_items WHERE restaurantId = :restaurantId")
    suspend fun clearForRestaurant(restaurantId: String)
}
