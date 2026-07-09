package com.cfg.android.data.remote.interceptor

import android.content.Context
import android.content.Intent
import com.cfg.android.ui.auth.LoginActivity
import com.cfg.android.util.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenManager.getAccessToken() }
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)

        // Un 401 sur /auth/login = mauvais identifiants (gere par LoginViewModel, pas une session expiree).
        val isLoginCall = request.url.encodedPath.endsWith("/auth/login")
        if (response.code == 401 && !isLoginCall) {
            runBlocking { tokenManager.clear() }
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }

        return response
    }
}
