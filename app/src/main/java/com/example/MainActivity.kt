package com.example

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.ui.OnyxFilesApp
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as OnyxFilesApplication
        MainViewModel.Factory(
            context = this,
            prefsRepo = app.preferencesRepository,
            fileRepo = app.fileOperationsRepository,
            audioController = app.audioPlayerController
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        val maxHz = detectMaxRefreshRateHz()
        applyPreferredRefreshRate(enabled = true)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            OnyxFilesApp(
                viewModel = viewModel,
                widthSizeClass = windowSizeClass.widthSizeClass,
                maxRefreshRateHz = maxHz,
                onApplyRefreshRate = { enabled -> applyPreferredRefreshRate(enabled) }
            )
        }
    }

    private fun currentDisplay(): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
    }

    private fun detectMaxRefreshRateHz(): Int {
        val disp = currentDisplay() ?: return 60
        val currentMode = disp.mode
        val maxRate = disp.supportedModes
            .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
            .maxOfOrNull { it.refreshRate }
            ?: currentMode.refreshRate
        return maxRate.roundToInt().coerceAtLeast(60)
    }

    private fun applyPreferredRefreshRate(enabled: Boolean) {
        val disp = currentDisplay() ?: return
        val currentMode = disp.mode
        val sameResModes = disp.supportedModes.filter {
            it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight
        }
        val targetMode = if (enabled) {
            sameResModes.maxByOrNull { it.refreshRate } ?: currentMode
        } else {
            sameResModes.minByOrNull { kotlin.math.abs(it.refreshRate - 60f) } ?: currentMode
        }

        val attrs: WindowManager.LayoutParams = window.attributes
        attrs.preferredDisplayModeId = targetMode.modeId
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            attrs.preferredRefreshRate = targetMode.refreshRate
        }
        window.attributes = attrs
    }
}
