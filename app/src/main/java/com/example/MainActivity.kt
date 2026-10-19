package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.navigation.NafsiMainScaffold
import com.example.ui.theme.NafsiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        viewModel.refreshAccessibilityStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Audio permission launcher
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    viewModel.repository.logDiagnostic("Microphone permission granted.")
                    if (viewModel.voiceSettings.value.continuousListening) {
                        viewModel.repository.listeningManager.startContinuousListening()
                    }
                } else {
                    viewModel.repository.logDiagnostic("Microphone permission denied.")
                }
            }

            // Screen capture projection launcher
            val projectionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    viewModel.repository.screenCaptureManager.setConsentResult(result.resultCode, result.data!!)
                    viewModel.repository.logDiagnostic("Screen capture consent granted.")
                } else {
                    viewModel.repository.logDiagnostic("Screen capture consent denied.")
                }
            }

            LaunchedEffect(Unit) {
                // Check Record Audio permission
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }

                // If floating mascot enabled and overlay permission is granted, launch overlay service
                if (viewModel.floatingMascotEnabled.value && android.provider.Settings.canDrawOverlays(this@MainActivity)) {
                    viewModel.setFloatingMascotEnabled(true)
                }
            }

            NafsiTheme {
                NafsiMainScaffold(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
