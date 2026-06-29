package com.cfg.android.data.remote

import com.cfg.android.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginData>>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: Map<String, String>): Response<ApiResponse<RefreshData>>

    @GET("restaurants/{restaurantId}/menu")
    suspend fun getMenu(@Path("restaurantId") restaurantId: String): Response<ApiResponse<MenuResponse>>

    @GET("restaurants/{restaurantId}/tables")
    suspend fun getTables(@Path("restaurantId") restaurantId: String): Response<ApiResponse<List<TableDto>>>

    @GET("restaurants/{restaurantId}/orders/active")
    suspend fun getActiveOrders(@Path("restaurantId") restaurantId: String): Response<ApiResponse<List<OrderDto>>>

    @POST("restaurants/{restaurantId}/orders")
    suspend fun createOrder(
        @Path("restaurantId") restaurantId: String,
        @Body request: CreateOrderRequest
    ): Response<ApiResponse<OrderDto>>

    @PATCH("restaurants/{restaurantId}/orders/{orderId}/status")
    suspend fun updateOrderStatus(
        @Path("restaurantId") restaurantId: String,
        @Path("orderId") orderId: String,
        @Body request: Map<String, String>
    ): Response<ApiResponse<OrderDto>>

    @POST("restaurants/{restaurantId}/orders/{orderId}/transfer")
    suspend fun transferOrder(
        @Path("restaurantId") restaurantId: String,
        @Path("orderId") orderId: String,
        @Body request: Map<String, String>
    ): Response<ApiResponse<OrderDto>>

    @POST("restaurants/{restaurantId}/orders/{orderId}/payment")
    suspend fun createPayment(
        @Path("restaurantId") restaurantId: String,
        @Path("orderId") orderId: String,
        @Body request: PaymentRequest
    ): Response<ApiResponse<PaymentDto>>

    @POST("sync/batch")
    suspend fun syncBatch(@Body request: SyncBatchRequest): Response<ApiResponse<List<Map<String, Any>>>>
}
