package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ScrollView

/**
 * Custom AccessibilityDelegate for rotary / crown rotation mapping on Android smartwatches,
 * especially Chinese full Android watches (e.g. Kingwear, Lemfo, Lokmat, Kospet, Microwear).
 *
 * It fulfills:
 * // Line 1: Tell the system that this layout actively handles automated user scroll intents
 * myScrollView.accessibilityDelegate = View.AccessibilityDelegate()
 *
 * // Line 2: Route the standard framework container tracking to update your active item indices
 * override fun onAccessibilityEvent(event: AccessibilityEvent) { /* Update list index here */ }
 */
open class CrownAccessibilityDelegate(
    private val onEvent: (AccessibilityEvent) -> Unit,
    private val onScrollUp: () -> Unit,
    private val onScrollDown: () -> Unit
) : View.AccessibilityDelegate() {

    // Line 2: Route the standard framework container tracking to update your active item indices
    open fun onAccessibilityEvent(event: AccessibilityEvent) {
        onEvent(event)
    }

    override fun sendAccessibilityEvent(host: View, eventType: Int) {
        super.sendAccessibilityEvent(host, eventType)
    }

    override fun sendAccessibilityEventUnchecked(host: View, event: AccessibilityEvent) {
        super.sendAccessibilityEventUnchecked(host, event)
        onAccessibilityEvent(event)
    }

    override fun onPopulateAccessibilityEvent(host: View, event: AccessibilityEvent) {
        super.onPopulateAccessibilityEvent(host, event)
        onAccessibilityEvent(event)
    }

    override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(host, event)
        onAccessibilityEvent(event)
    }

    override fun onRequestSendAccessibilityEvent(
        host: ViewGroup,
        child: View,
        event: AccessibilityEvent
    ): Boolean {
        onAccessibilityEvent(event)
        return super.onRequestSendAccessibilityEvent(host, child, event)
    }

    override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
        when (action) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id,
            AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_UP.id,
            android.R.id.accessibilityActionScrollUp -> {
                onScrollUp()
                return true
            }
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id,
            AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_DOWN.id,
            android.R.id.accessibilityActionScrollDown -> {
                onScrollDown()
                return true
            }
        }
        return super.performAccessibilityAction(host, action, args)
    }
}

/**
 * ScrollView container that explicitly registers with the Android window manager
 * and accessibility system as handling scroll intents, routing crown rotation to active item indices.
 */
class CrownScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    var onCrownScrollUp: (() -> Unit)? = null
    var onCrownScrollDown: (() -> Unit)? = null

    private var lastScrollYPos = 5000
    private var lastScrollXPos = 5000
    private var lastFromIndex = -1
    private var lastEventTime = 0L

    init {
        // Line 1: Tell the system that this layout actively handles automated user scroll intents
        isScrollContainer = true
        isNestedScrollingEnabled = true
        isFocusable = false
        isFocusableInTouchMode = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        overScrollMode = OVER_SCROLL_NEVER

        // Create internal scroll canvas so canScrollVertically(1) and canScrollVertically(-1) are always true
        val dummyContent = View(context).apply {
            layoutParams = LayoutParams(10000, 10000)
            visibility = VISIBLE
        }
        addView(dummyContent)

        // Center scroll position so crown rotation can scroll both ways indefinitely
        post {
            scrollTo(5000, 5000)
            lastScrollYPos = 5000
            lastScrollXPos = 5000
        }

        // Line 1: Tell the system that this layout actively handles automated user scroll intents
        accessibilityDelegate = object : CrownAccessibilityDelegate(
            onEvent = { event -> onAccessibilityEvent(event) },
            onScrollUp = { triggerCrownUp() },
            onScrollDown = { triggerCrownDown() }
        ) {
            // Inherits accessibility delegate mapping
        }
    }

    // Line 2: Route the standard framework container tracking to update your active item indices
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        handleIncomingAccessibilityEvent(event)
    }

    override fun sendAccessibilityEventUnchecked(event: AccessibilityEvent) {
        super.sendAccessibilityEventUnchecked(event)
        onAccessibilityEvent(event)
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        onAccessibilityEvent(event)
    }

    override fun onPopulateAccessibilityEvent(event: AccessibilityEvent) {
        super.onPopulateAccessibilityEvent(event)
        onAccessibilityEvent(event)
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean {
        onAccessibilityEvent(event)
        return super.dispatchPopulateAccessibilityEvent(event)
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        when (action) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id,
            AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_UP.id,
            android.R.id.accessibilityActionScrollUp -> {
                triggerCrownUp()
                return true
            }
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id,
            AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_DOWN.id,
            android.R.id.accessibilityActionScrollDown -> {
                triggerCrownDown()
                return true
            }
        }
        return super.performAccessibilityAction(action, arguments)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        val dy = t - oldt
        val dx = l - oldl
        if (dy > 0 || dx > 0) {
            triggerCrownDown()
        } else if (dy < 0 || dx < 0) {
            triggerCrownUp()
        }
        // Keep scroll within safe bounds away from boundaries
        if (t < 1000 || t > 9000 || l < 1000 || l > 9000) {
            post {
                scrollTo(5000, 5000)
                lastScrollYPos = 5000
                lastScrollXPos = 5000
            }
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL || event.actionMasked == MotionEvent.ACTION_SCROLL) {
            val axisScroll = event.getAxisValue(MotionEvent.AXIS_SCROLL)
            val axisVScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            val axisWheel = event.getAxisValue(MotionEvent.AXIS_WHEEL)
            val vScroll = when {
                axisScroll != 0f -> axisScroll
                axisVScroll != 0f -> axisVScroll
                axisWheel != 0f -> axisWheel
                else -> 0f
            }
            if (vScroll > 0f) {
                triggerCrownUp()
                return true
            } else if (vScroll < 0f) {
                triggerCrownDown()
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return super.dispatchKeyEvent(event)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return false // Do not intercept touches - keep Jetpack Compose gestures fully responsive
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return false // Do not block touches
    }

    private fun triggerCrownUp() {
        val now = System.currentTimeMillis()
        if (now - lastEventTime > 25L) {
            lastEventTime = now
            onCrownScrollUp?.invoke()
        }
    }

    private fun triggerCrownDown() {
        val now = System.currentTimeMillis()
        if (now - lastEventTime > 25L) {
            lastEventTime = now
            onCrownScrollDown?.invoke()
        }
    }

    private fun handleIncomingAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                // Check Android P (API 28+) scrollDelta
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val dy = event.scrollDeltaY
                    val dx = event.scrollDeltaX
                    if (dy > 0 || dx > 0) {
                        triggerCrownDown()
                        return
                    } else if (dy < 0 || dx < 0) {
                        triggerCrownUp()
                        return
                    }
                }

                // Check event action
                if (event.action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
                    triggerCrownDown()
                    return
                } else if (event.action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
                    triggerCrownUp()
                    return
                }

                // Check scrollY position
                val curY = event.scrollY
                if (curY != -1 && curY != lastScrollYPos) {
                    if (curY > lastScrollYPos) {
                        triggerCrownDown()
                    } else {
                        triggerCrownUp()
                    }
                    lastScrollYPos = curY
                    return
                }

                // Check scrollX position
                val curX = event.scrollX
                if (curX != -1 && curX != lastScrollXPos) {
                    if (curX > lastScrollXPos) {
                        triggerCrownDown()
                    } else {
                        triggerCrownUp()
                    }
                    lastScrollXPos = curX
                    return
                }

                // Check fromIndex
                val fromIdx = event.fromIndex
                if (fromIdx != -1 && fromIdx != lastFromIndex) {
                    if (fromIdx > lastFromIndex) {
                        triggerCrownDown()
                    } else {
                        triggerCrownUp()
                    }
                    lastFromIndex = fromIdx
                    return
                }
            }
            AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                val fromIdx = event.fromIndex
                if (fromIdx != -1 && fromIdx != lastFromIndex) {
                    if (fromIdx > lastFromIndex) {
                        triggerCrownDown()
                    } else {
                        triggerCrownUp()
                    }
                    lastFromIndex = fromIdx
                }
            }
        }
    }
}
