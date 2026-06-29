package com.cfg.android.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cfg.android.data.remote.dto.CreateOrderRequest
import com.cfg.android.data.remote.dto.OrderDto
import com.cfg.android.data.remote.dto.OrderItemRequest
import com.cfg.android.data.repository.OrderRepository
import com.cfg.android.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CartItem(
    val menuItemId: String,
    val name: String,
    val price: Double,
    var quantity: Int = 1,
    var notes: String? = null,
    val modifierIds: List<String> = emptyList()
)

data class OrderUiState(
    val cart: List<CartItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val orderCreated: OrderDto? = null
)

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState

    fun addToCart(menuItemId: String, name: String, price: Double) {
        val cart = _uiState.value.cart.toMutableList()
        val existing = cart.indexOfFirst { it.menuItemId == menuItemId }
        if (existing >= 0) {
            cart[existing] = cart[existing].copy(quantity = cart[existing].quantity + 1)
        } else {
            cart.add(CartItem(menuItemId, name, price))
        }
        _uiState.value = _uiState.value.copy(cart = cart)
    }

    fun removeFromCart(menuItemId: String) {
        _uiState.value = _uiState.value.copy(
            cart = _uiState.value.cart.filter { it.menuItemId != menuItemId }
        )
    }

    fun updateQuantity(menuItemId: String, quantity: Int) {
        if (quantity <= 0) { removeFromCart(menuItemId); return }
        _uiState.value = _uiState.value.copy(
            cart = _uiState.value.cart.map {
                if (it.menuItemId == menuItemId) it.copy(quantity = quantity) else it
            }
        )
    }

    fun totalAmount(): Double = _uiState.value.cart.sumOf { it.price * it.quantity }

    fun submitOrder(tableId: String?, customerName: String?, notes: String?) {
        val cart = _uiState.value.cart
        if (cart.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val restaurantId = tokenManager.getRestaurantId() ?: run {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "No restaurant context")
                return@launch
            }
            val request = CreateOrderRequest(
                tableId = tableId,
                customerName = customerName,
                notes = notes,
                clientUuid = UUID.randomUUID().toString(),
                items = cart.map {
                    OrderItemRequest(it.menuItemId, it.quantity, it.notes, it.modifierIds)
                }
            )
            val result = orderRepository.createOrder(restaurantId, request)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    orderCreated = result.getOrNull(),
                    cart = emptyList()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearOrderCreated() {
        _uiState.value = _uiState.value.copy(orderCreated = null)
    }
}
