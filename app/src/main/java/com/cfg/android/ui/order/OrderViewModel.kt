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
    val modifierIds: List<String> = emptyList(),
    val modifierNames: List<String> = emptyList(),
    // Identifiant propre a la ligne de panier : deux clients qui commandent le meme plat avec
    // des options differentes doivent rester deux lignes distinctes, pas fusionnees.
    val cartItemId: String = UUID.randomUUID().toString()
)

data class OrderUiState(
    val cart: List<CartItem> = emptyList(),
    val tableId: String? = null,
    val tableNumber: Int = 0,
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

    /**
     * Ajoute un article au panier. Si un article identique (memes options, memes notes) existe
     * deja, incremente sa quantite ; sinon cree une nouvelle ligne.
     */
    fun addToCart(
        menuItemId: String,
        name: String,
        price: Double,
        modifierIds: List<String> = emptyList(),
        modifierNames: List<String> = emptyList(),
        notes: String? = null
    ) {
        val cart = _uiState.value.cart.toMutableList()
        val existing = cart.indexOfFirst {
            it.menuItemId == menuItemId &&
                it.modifierIds.sorted() == modifierIds.sorted() &&
                it.notes == notes
        }
        if (existing >= 0) {
            cart[existing] = cart[existing].copy(quantity = cart[existing].quantity + 1)
        } else {
            cart.add(CartItem(menuItemId, name, price, notes = notes, modifierIds = modifierIds, modifierNames = modifierNames))
        }
        _uiState.value = _uiState.value.copy(cart = cart)
    }

    fun removeFromCart(cartItemId: String) {
        _uiState.value = _uiState.value.copy(
            cart = _uiState.value.cart.filter { it.cartItemId != cartItemId }
        )
    }

    fun updateQuantity(cartItemId: String, quantity: Int) {
        if (quantity <= 0) { removeFromCart(cartItemId); return }
        _uiState.value = _uiState.value.copy(
            cart = _uiState.value.cart.map {
                if (it.cartItemId == cartItemId) it.copy(quantity = quantity) else it
            }
        )
    }

    fun totalAmount(): Double = _uiState.value.cart.sumOf { it.price * it.quantity }

    /** Quantite totale d'un article donne, tous choix d'options confondus (pour le badge sur la tuile menu). */
    fun quantityFor(menuItemId: String): Int =
        _uiState.value.cart.filter { it.menuItemId == menuItemId }.sumOf { it.quantity }

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

    fun clearCart() {
        _uiState.value = _uiState.value.copy(cart = emptyList(), orderCreated = null, error = null)
    }

    fun setTableId(tableId: String?, tableNumber: Int) {
        _uiState.value = _uiState.value.copy(tableId = tableId, tableNumber = tableNumber)
    }

    fun setItemNotes(cartItemId: String, notes: String?) {
        _uiState.value = _uiState.value.copy(
            cart = _uiState.value.cart.map {
                if (it.cartItemId == cartItemId) it.copy(notes = notes) else it
            }
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearOrderCreated() {
        _uiState.value = _uiState.value.copy(orderCreated = null)
    }
}
