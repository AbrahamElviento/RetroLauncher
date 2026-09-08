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

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.view.MotionEvent
import android.view.InputDevice
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.Coil
import com.example.ui.components.CrownScrollView
import com.example.ui.components.CrownAccessibilityDelegate

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private var sleepJob: kotlinx.coroutines.Job? = null
    private var myScrollView: CrownScrollView? = null

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
        setupCrownScrollView()
    }

    private fun setupCrownScrollView() {
        val root = findViewById<ViewGroup>(android.R.id.content) ?: return
        
        // Line 1: Tell the system that this layout actively handles automated user scroll intents
        val scrollView = CrownScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            alpha = 0.001f // Invisible to eye, visible to accessibility & scroll system
            
            // Route crown rotation to ViewModel active item updates
            onCrownScrollUp = {
                viewModel.onRotaryScroll(1f)
            }
            onCrownScrollDown = {
                viewModel.onRotaryScroll(-1f)
            }

            // Line 1: Tell the system that this layout actively handles automated user scroll intents
            accessibilityDelegate = CrownAccessibilityDelegate(
                onEvent = { event -> onAccessibilityEvent(event) },
                onScrollUp = { viewModel.onRotaryScroll(1f) },
                onScrollDown = { viewModel.onRotaryScroll(-1f) }
            )
        }
        myScrollView = scrollView
        root.addView(scrollView, 0)

        // Also assign accessibility delegate to window DecorView for system-level crown listeners
        window.decorView.accessibilityDelegate = CrownAccessibilityDelegate(
            onEvent = { event -> onAccessibilityEvent(event) },
            onScrollUp = { viewModel.onRotaryScroll(1f) },
            onScrollDown = { viewModel.onRotaryScroll(-1f) }
        )
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean {
        onAccessibilityEvent(event)
        return super.dispatchPopulateAccessibilityEvent(event)
    }

    // Line 2: Route the standard framework container tracking to update your active item indices
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        /* Update list index here */
        handleCrownAccessibilityEvent(event)
    }

    private var lastAccScrollY = 5000
    private var lastAccFromIndex = -1
    private var lastAccEventTime = 0L

    private fun handleCrownAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val dy = event.scrollDeltaY
                    val dx = event.scrollDeltaX
                    if (dy > 0 || dx > 0) {
                        triggerCrownDownFromAcc()
                        return
                    } else if (dy < 0 || dx < 0) {
                        triggerCrownUpFromAcc()
                        return
                    }
                }

                if (event.action == android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
                    triggerCrownDownFromAcc()
                    return
                } else if (event.action == android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
                    triggerCrownUpFromAcc()
                    return
                }

                val curY = event.scrollY
                if (curY != -1 && curY != lastAccScrollY) {
                    if (curY > lastAccScrollY) {
                        triggerCrownDownFromAcc()
                    } else {
                        triggerCrownUpFromAcc()
                    }
                    lastAccScrollY = curY
                    return
                }

                val fromIdx = event.fromIndex
                if (fromIdx != -1 && fromIdx != lastAccFromIndex) {
                    if (fromIdx > lastAccFromIndex) {
                        triggerCrownDownFromAcc()
                    } else {
                        triggerCrownUpFromAcc()
                    }
                    lastAccFromIndex = fromIdx
                    return
                }
            }
            AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                val fromIdx = event.fromIndex
                if (fromIdx != -1 && fromIdx != lastAccFromIndex) {
                    if (fromIdx > lastAccFromIndex) {
                        triggerCrownDownFromAcc()
                    } else {
                        triggerCrownUpFromAcc()
                    }
                    lastAccFromIndex = fromIdx
                }
            }
        }
    }

    private fun triggerCrownUpFromAcc() {
        val now = System.currentTimeMillis()
        if (now - lastAccEventTime > 25L) {
            lastAccEventTime = now
            viewModel.onRotaryScroll(1f)
        }
    }

    private fun triggerCrownDownFromAcc() {
        val now = System.currentTimeMillis()
        if (now - lastAccEventTime > 25L) {
            lastAccEventTime = now
            viewModel.onRotaryScroll(-1f)
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

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL || event.actionMasked == MotionEvent.ACTION_SCROLL) {
            val axisScroll = event.getAxisValue(MotionEvent.AXIS_SCROLL)
            val axisVScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            val axisWheel = event.getAxisValue(MotionEvent.AXIS_WHEEL)
            val axisY = event.getAxisValue(MotionEvent.AXIS_Y)
            val vScroll = when {
                axisScroll != 0f -> axisScroll
                axisVScroll != 0f -> axisVScroll
                axisWheel != 0f -> axisWheel
                axisY != 0f -> axisY
                else -> 0f
            }
            if (vScroll != 0f) {
                viewModel.onRotaryScroll(vScroll)
                return true
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL || event.actionMasked == MotionEvent.ACTION_SCROLL) {
            val axisScroll = event.getAxisValue(MotionEvent.AXIS_SCROLL)
            val axisVScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            val axisWheel = event.getAxisValue(MotionEvent.AXIS_WHEEL)
            val axisY = event.getAxisValue(MotionEvent.AXIS_Y)
            val vScroll = when {
                axisScroll != 0f -> axisScroll
                axisVScroll != 0f -> axisVScroll
                axisWheel != 0f -> axisWheel
                axisY != 0f -> axisY
                else -> 0f
            }
            if (vScroll != 0f) {
                viewModel.onRotaryScroll(vScroll)
                return true
            }
        }
        return super.onGenericMotionEvent(event)
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

