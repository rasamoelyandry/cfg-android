package com.cfg.android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

// Auth
data class LoginRequest(val email: String?, val phone: String?, val password: String)
data class LoginData(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto
)
data class RefreshData(val accessToken: String)
data class UserDto(
    val id: String,
    val restaurantId: String?,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String?,
    val role: String
)

// Menu
data class MenuResponse(val categories: List<CategoryDto>)
data class CategoryDto(val id: String, val name: String, val items: List<MenuItemDto>)
data class MenuItemDto(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String?,
    val price: Double,
    val imageUrl: String?,
    @SerializedName("isAvailable") val isAvailable: Boolean,
    val sortOrder: Int,
    val modifiers: List<ModifierDto>
)
data class ModifierDto(val id: String, val name: String, val priceDelta: Double)

// Table
data class TableDto(val id: String, val number: Int, val label: String?, val capacity: Int)

// Order
data class OrderDto(
    val id: String,
    val restaurantId: String,
    val tableId: String?,
    val customerName: String?,
    val status: String,
    val notes: String?,
    val totalAmount: Double,
    val clientUuid: String?,
    val createdAt: String,
    val items: List<OrderItemDto>
)
data class OrderItemDto(
    val id: String,
    val menuItemId: String,
    val menuItemName: String,
    val unitPrice: Double,
    val quantity: Int,
    val notes: String?,
    val status: String
)

// Create Order
data class CreateOrderRequest(
    val tableId: String?,
    val customerName: String?,
    val notes: String?,
    val clientUuid: String?,
    val items: List<OrderItemRequest>
)
data class OrderItemRequest(
    val menuItemId: String,
    val quantity: Int,
    val notes: String?,
    val modifierIds: List<String>?
)

// Payment
data class PaymentRequest(val method: String, val amount: Double, val reference: String?, val notes: String?)
data class PaymentDto(val id: String, val method: String, val amount: Double, val paidAt: String)

// Sync
data class SyncBatchRequest(val events: List<SyncItem>)
data class SyncItem(val clientUuid: String, val eventType: String, val payload: String, val localTimestamp: String)
