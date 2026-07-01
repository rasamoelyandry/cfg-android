package com.cfg.android.ui.tables

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.cfg.android.R
import com.cfg.android.databinding.ActivityTableListBinding
import com.cfg.android.service.SyncWorker
import com.cfg.android.ui.auth.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TableListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTableListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTableListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start periodic background sync
        SyncWorker.schedulePeriodicSync(this)
    }

    fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
