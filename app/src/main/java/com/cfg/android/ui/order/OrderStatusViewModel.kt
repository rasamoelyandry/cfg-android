package com.cfg.android.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cfg.android.data.remote.dto.OrderDto
import com.cfg.android.data.repository.OrderRepository
import com.cfg.android.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderStatusUiState(
    val order: OrderDto? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OrderStatusViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderStatusUiState())
    val uiState: StateFlow<OrderStatusUiState> = _uiState

    fun load(orderId: String) {
        viewModelScope.launch {
            val restaurantId = tokenManager.getRestaurantId() ?: run {
                _uiState.value = _uiState.value.copy(error = "Contexte restaurant manquant")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = orderRepository.getOrder(restaurantId, orderId)
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isLoading = false, order = result.getOrNull())
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Erreur de chargement"
                )
            }
        }
    }

    fun markAsServed() {
        val order = _uiState.value.order ?: return
        viewModelScope.launch {
            val restaurantId = tokenManager.getRestaurantId() ?: return@launch
            _uiState.value = _uiState.value.copy(isUpdating = true, error = null)
            val result = orderRepository.updateStatus(restaurantId, order.id, "SERVED")
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isUpdating = false, order = result.getOrNull())
            } else {
                _uiState.value.copy(
                    isUpdating = false,
                    error = result.exceptionOrNull()?.message ?: "Impossible de marquer comme servi"
                )
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
