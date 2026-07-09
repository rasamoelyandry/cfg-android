package com.cfg.android.ui.tables

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.cfg.android.databinding.ActivityTableListBinding
import com.cfg.android.service.KitchenNotificationService
import com.cfg.android.service.SyncWorker
import com.cfg.android.ui.auth.LoginActivity
import com.cfg.android.util.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TableListActivity : AppCompatActivity() {

    @Inject lateinit var tokenManager: TokenManager

    private lateinit var binding: ActivityTableListBinding

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* refuse = pas d'alerte cuisine, l'appli reste utilisable */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTableListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start periodic background sync
        SyncWorker.schedulePeriodicSync(this)

        requestNotificationPermissionIfNeeded()
        startKitchenNotifications()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startKitchenNotifications() {
        lifecycleScope.launch {
            // La cuisine n'a pas besoin d'etre alertee que ses propres commandes sont pretes
            if (tokenManager.getRole() != "KITCHEN") {
                KitchenNotificationService.start(this@TableListActivity)
            }
        }
    }

    fun navigateToLogin() {
        KitchenNotificationService.stop(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
