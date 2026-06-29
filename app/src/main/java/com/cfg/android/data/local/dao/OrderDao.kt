package com.cfg.android.data.local.dao

import androidx.room.*
import com.cfg.android.data.local.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE restaurantId = :restaurantId ORDER BY createdAt DESC")
    fun getOrdersFlow(restaurantId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE restaurantId = :restaurantId AND status NOT IN ('PAID','CANCELLED') ORDER BY createdAt DESC")
    suspend fun getActiveOrders(restaurantId: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(orders: List<OrderEntity>)

    @Query("UPDATE orders SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}
