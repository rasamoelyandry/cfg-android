package com.cfg.android.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cfg.android.data.remote.ApiService
import com.cfg.android.data.remote.dto.PaymentDto
import com.cfg.android.data.remote.dto.PaymentRequest
import com.cfg.android.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentUiState(
    val selectedMethod: String = "CASH",
    val amountPaid: Double = 0.0,
    val reference: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val paymentDone: PaymentDto? = null
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState

    fun selectMethod(method: String) {
        _uiState.value = _uiState.value.copy(selectedMethod = method)
    }

    fun setAmountPaid(amount: Double) {
        _uiState.value = _uiState.value.copy(amountPaid = amount)
    }

    fun setReference(ref: String?) {
        _uiState.value = _uiState.value.copy(reference = ref?.ifBlank { null })
    }

    fun pay(orderId: String, expectedAmount: Double) {
        viewModelScope.launch {
            val restaurantId = tokenManager.getRestaurantId() ?: run {
                _uiState.value = _uiState.value.copy(error = "Contexte restaurant manquant")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val state = _uiState.value
                val request = PaymentRequest(
                    method = state.selectedMethod,
                    amount = expectedAmount,
                    reference = state.reference,
                    notes = null
                )
                val resp = apiService.createPayment(restaurantId, orderId, request)
                if (resp.isSuccessful && resp.body()?.data != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, paymentDone = resp.body()!!.data)
                } else {
                    val msg = resp.body()?.message ?: "Erreur de paiement (${resp.code()})"
                    _uiState.value = _uiState.value.copy(isLoading = false, error = msg)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Erreur réseau")
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
