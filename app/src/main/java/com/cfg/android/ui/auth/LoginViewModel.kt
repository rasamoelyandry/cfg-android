package com.cfg.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cfg.android.data.remote.ApiService
import com.cfg.android.data.remote.dto.LoginRequest
import com.cfg.android.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val restaurantId: String?) : LoginState()
    data class Error(val message: String) : LoginState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun checkExistingSession() {
        viewModelScope.launch {
            if (tokenManager.isLoggedIn()) {
                _state.value = LoginState.Success(tokenManager.getRestaurantId())
            }
        }
    }

    fun login(emailOrPhone: String, password: String) {
        if (emailOrPhone.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("Veuillez remplir tous les champs")
            return
        }
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val request = if (emailOrPhone.contains("@"))
                    LoginRequest(email = emailOrPhone, phone = null, password = password)
                else
                    LoginRequest(email = null, phone = emailOrPhone, password = password)

                val response = apiService.login(request)
                if (response.isSuccessful && response.body()?.data != null) {
                    val data = response.body()!!.data!!
                    tokenManager.saveTokens(data.accessToken, data.refreshToken)
                    tokenManager.saveUserInfo(data.user.id, data.user.restaurantId, data.user.role)
                    _state.value = LoginState.Success(data.user.restaurantId)
                } else {
                    _state.value = LoginState.Error("Identifiants incorrects")
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error("Erreur réseau : ${e.message}")
            }
        }
    }
}
