package com.cfg.android.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cfg.android.databinding.ActivityLoginBinding
import com.cfg.android.ui.tables.TableListActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.checkExistingSession()
        setupUi()
        observeState()
    }

    private fun setupUi() {
        binding.btnLogin.setOnClickListener { submitLogin() }
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { submitLogin(); true } else false
        }
    }

    private fun submitLogin() {
        val identifier = binding.etEmailOrPhone.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()
        viewModel.login(identifier, password)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is LoginState.Idle -> {
                            binding.progressBar.isVisible = false
                            binding.btnLogin.isEnabled = true
                            binding.tvError.isVisible = false
                        }
                        is LoginState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.btnLogin.isEnabled = false
                            binding.tvError.isVisible = false
                        }
                        is LoginState.Success -> {
                            binding.progressBar.isVisible = false
                            startActivity(Intent(this@LoginActivity, TableListActivity::class.java))
                            finish()
                        }
                        is LoginState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.btnLogin.isEnabled = true
                            binding.tvError.text = state.message
                            binding.tvError.isVisible = true
                        }
                    }
                }
            }
        }
    }
}
