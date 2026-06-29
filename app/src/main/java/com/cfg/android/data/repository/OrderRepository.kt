package com.cfg.android.data.repository

import com.cfg.android.data.local.dao.OrderDao
import com.cfg.android.data.local.dao.SyncQueueDao
import com.cfg.android.data.local.entity.OrderEntity
import com.cfg.android.data.local.entity.SyncQueueEntity
import com.cfg.android.data.remote.ApiService
import com.cfg.android.data.remote.dto.CreateOrderRequest
import com.cfg.android.data.remote.dto.OrderDto
import com.cfg.android.util.NetworkMonitor
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val apiService: ApiService,
    private val orderDao: OrderDao,
    private val syncQueueDao: SyncQueueDao,
    private val networkMonitor: NetworkMonitor,
    private val gson: Gson
) {
    fun getOrdersFlow(restaurantId: String): Flow<List<OrderEntity>> =
        orderDao.getOrdersFlow(restaurantId)

    suspend fun createOrder(restaurantId: String, request: CreateOrderRequest): Result<OrderDto> {
        return if (networkMonitor.isOnline.value) {
            try {
                val response = apiService.createOrder(restaurantId, request)
                if (response.isSuccessful && response.body()?.data != null) {
                    val dto = response.body()!!.data!!
                    orderDao.upsert(dto.toEntity())
                    Result.success(dto)
                } else {
                    Result.failure(Exception("Server error: ${response.code()}"))
                }
            } catch (e: Exception) {
                enqueueOffline(request)
                Result.failure(e)
            }
        } else {
            enqueueOffline(request)
            Result.failure(Exception("Offline: order queued for sync"))
        }
    }

    private suspend fun enqueueOffline(request: CreateOrderRequest) {
        val uuid = request.clientUuid ?: UUID.randomUUID().toString()
        syncQueueDao.insert(
            SyncQueueEntity(
                clientUuid = uuid,
                eventType = "CREATE_ORDER",
                payload = gson.toJson(request.copy(clientUuid = uuid)),
                localTimestamp = System.currentTimeMillis()
            )
        )
        // Persist locally for immediate display
        orderDao.upsert(OrderEntity(
            id = "local-$uuid",
            restaurantId = request.tableId ?: "",
            tableId = request.tableId,
            customerName = request.customerName,
            status = "PENDING",
            notes = request.notes,
            totalAmount = 0.0,
            clientUuid = uuid,
            itemsJson = gson.toJson(request.items),
            createdAt = System.currentTimeMillis()
        ))
    }
}

private fun OrderDto.toEntity() = OrderEntity(
    id = id,
    restaurantId = restaurantId,
    tableId = tableId,
    customerName = customerName,
    status = status,
    notes = notes,
    totalAmount = totalAmount,
    clientUuid = clientUuid,
    itemsJson = "[]",
    createdAt = System.currentTimeMillis(),
    syncedAt = System.currentTimeMillis()
)
