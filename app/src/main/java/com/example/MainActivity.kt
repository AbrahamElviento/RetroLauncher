package com.example

import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainHomeScreen
import com.example.ui.theme.RetroLauncherTheme
import com.example.ui.viewmodel.LauncherViewModel

import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.Coil

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private var sleepJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Coil with SVG decoder support
        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
        Coil.setImageLoader(imageLoader)

        enableEdgeToEdge()
        lifecycleScope.launch {
            viewModel.displaySettings.collect { settings ->
                updateSystemUIVisibility(settings.enableImmersiveMode)
            }
        }
        checkAndRequestStoragePermissions()
        setContent {
            val displaySettings by viewModel.displaySettings.collectAsState()
            RetroLauncherTheme(displaySettings = displaySettings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainHomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    } catch (e2: Exception) {
                        // Ignore
                    }
                }
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val missing = permissions.filter {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                requestPermissions(missing.toTypedArray(), 1001)
            }
        }
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_CLASS_JOYSTICK != 0 && event.action == MotionEvent.ACTION_MOVE) {
            // Check right stick vertical axis (AXIS_RZ or AXIS_RY)
            var ry = event.getAxisValue(MotionEvent.AXIS_RZ)
            if (kotlin.math.abs(ry) < 0.05f) {
                ry = event.getAxisValue(MotionEvent.AXIS_RY)
            }

            if (kotlin.math.abs(ry) > 0.15f) {
                // Synthesize a vertical scroll MotionEvent (ACTION_SCROLL)
                val scrollFactor = -ry * 35f
                val coords = MotionEvent.PointerCoords().apply {
                    x = event.x
                    y = event.y
                    setAxisValue(MotionEvent.AXIS_VSCROLL, scrollFactor)
                }
                val props = MotionEvent.PointerProperties().apply {
                    id = 0
                }
                val scrollEvent = MotionEvent.obtain(
                    event.downTime,
                    event.eventTime,
                    MotionEvent.ACTION_SCROLL,
                    1,
                    arrayOf(props),
                    arrayOf(coords),
                    0, 0, 1f, 1f, event.deviceId, 0, event.source, 0
                )
                window.superDispatchGenericMotionEvent(scrollEvent)
                scrollEvent.recycle()
                return true
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Maintain current active UI state when home button is pressed
    }

    override fun onPause() {
        super.onPause()
        sleepJob?.cancel()
        com.example.util.SoundManager.pauseBgm()
    }

    override fun onResume() {
        super.onResume()
        val displaySettings = viewModel.displaySettings.value
        updateSystemUIVisibility(displaySettings.enableImmersiveMode)
        if (displaySettings.enableBgm) {
            com.example.util.SoundManager.resumeBgm(this)
        }
        startSleepTimer()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            updateSystemUIVisibility(viewModel.displaySettings.value.enableImmersiveMode)
        }
    }

    private fun updateSystemUIVisibility(enableImmersive: Boolean) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (enableImmersive) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun startSleepTimer() {
        sleepJob?.cancel()
        sleepJob = lifecycleScope.launch {
            viewModel.displaySettings.collect { displaySettings ->
                when (displaySettings.sleepTimeoutMode) {
                    "ALWAYS_ON" -> {
                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    else -> {
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }
    }
}

