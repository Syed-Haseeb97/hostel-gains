package com.hostelgains.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hostelgains.data.ApiKeyRepository
import com.hostelgains.ui.theme.HostelGainsTheme
import com.hostelgains.ui.viewmodel.ApiKeyViewModel

class MainActivity : ComponentActivity() {
    private lateinit var apiKeyViewModel: ApiKeyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Build the repository with application Context (never leaks Activity)
        val apiKeyRepository = ApiKeyRepository(applicationContext)
        apiKeyViewModel = ApiKeyViewModel(apiKeyRepository)

        setContent {
            HostelGainsTheme {
                HostelGainsApp(apiKeyViewModel = apiKeyViewModel)
            }
        }
    }
}
