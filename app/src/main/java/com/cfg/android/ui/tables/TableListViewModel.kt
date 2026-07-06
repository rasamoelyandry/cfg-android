package com.cfg.android.ui.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cfg.android.data.remote.ApiService
import com.cfg.android.data.remote.dto.OrderDto
import com.cfg.android.data.remote.dto.TableDto
import com.cfg.android.util.NetworkMonitor
import com.cfg.android.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TableWithOrder(
    val table: TableDto,
    val activeOrder: OrderDto? = null
) {
    // Occupation persistee (independante du paiement) : le serveur libere la table manuellement,
    // meme si la commande est deja payee (le client peut rester un moment apres avoir paye).
    val isOccupied get() = table.occupied
}

data class TableListUiState(
    val items: List<TableWithOrder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class TableListViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(TableListUiState())
    val uiState: StateFlow<TableListUiState> = _uiState

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.value = _uiState.value.copy(isOffline = !online)
                if (online) loadTables()
            }
        }
        loadTables()
    }

    fun loadTables() {
        viewModelScope.launch {
            val restaurantId = tokenManager.getRestaurantId() ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val tablesResp = apiService.getTables(restaurantId)
                val ordersResp = apiService.getActiveOrders(restaurantId)

                if (tablesResp.isSuccessful && tablesResp.body()?.data != null) {
                    val tables = tablesResp.body()!!.data!!
                    val orders = if (ordersResp.isSuccessful) ordersResp.body()?.data.orEmpty() else emptyList()
                    val items = tables
                        .sortedBy { it.number }
                        .map { table ->
                            TableWithOrder(table, orders.firstOrNull { it.tableId == table.id })
                        }
                    _uiState.value = _uiState.value.copy(items = items, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Impossible de charger les tables (${tablesResp.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    suspend fun logout() = tokenManager.clear()

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
