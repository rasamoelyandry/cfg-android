package com.cfg.android.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cfg.android.data.remote.dto.CategoryDto
import com.cfg.android.data.remote.dto.MenuItemDto
import com.cfg.android.data.repository.MenuRepository
import com.cfg.android.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuUiState(
    val categories: List<CategoryDto> = emptyList(),
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val displayedItems: List<MenuItemDto>
        get() = categories.firstOrNull { it.id == selectedCategoryId }?.items
            ?: categories.firstOrNull()?.items
            ?: emptyList()
}

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            val restaurantId = tokenManager.getRestaurantId() ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            menuRepository.getMenu(restaurantId).fold(
                onSuccess = { menu ->
                    _uiState.value = _uiState.value.copy(
                        categories = menu.categories,
                        selectedCategoryId = menu.categories.firstOrNull()?.id,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur de chargement du menu"
                    )
                }
            )
        }
    }

    fun selectCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }
}
