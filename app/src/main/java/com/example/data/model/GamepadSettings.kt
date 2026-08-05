package com.example.data.model

import android.view.KeyEvent

data class GamepadSettings(
    val keyPageUp: Int = KeyEvent.KEYCODE_BUTTON_L1,
    val keyPageDown: Int = KeyEvent.KEYCODE_BUTTON_R1,
    val keyGoToTop: Int = KeyEvent.KEYCODE_BUTTON_L2,
    val keyGoToBottom: Int = KeyEvent.KEYCODE_BUTTON_R2,
    val keySystemSettings: Int = KeyEvent.KEYCODE_BUTTON_START,
    val keyRomListSettings: Int = KeyEvent.KEYCODE_BUTTON_SELECT,
    val keySelectAction: Int = KeyEvent.KEYCODE_BUTTON_A,
    val keyBackAction: Int = KeyEvent.KEYCODE_BUTTON_B,
    val keyFavoriteAction: Int = KeyEvent.KEYCODE_BUTTON_Y,
    val keyInfoAction: Int = KeyEvent.KEYCODE_BUTTON_X,
    val keyOpenSearch: Int = 0,
    val keySystemManagerAction: Int = KeyEvent.KEYCODE_BUTTON_START,
    val keyToggleTopBarKey1: Int = KeyEvent.KEYCODE_BUTTON_L1,
    val keyToggleTopBarKey2: Int = KeyEvent.KEYCODE_BUTTON_R1
)
