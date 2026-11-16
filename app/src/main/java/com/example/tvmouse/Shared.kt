package com.example.tvmouse

import android.content.SharedPreferences
import android.view.KeyEvent

enum class Act(val label: String) {
    NONE("Không làm gì (giữ nguyên)"),
    CLICK("Nhấp chuột"),
    LONG("Nhấn giữ"),
    BACK("Quay lại"),
    HOME("Màn hình chính"),
    RECENTS("Ứng dụng gần đây"),
    SCROLL_UP("Cuộn lên"),
    SCROLL_DOWN("Cuộn xuống")
}

object Shared {
    const val PREFS = "map"

    val buttons = listOf(
        KeyEvent.KEYCODE_BUTTON_A to "A",
        KeyEvent.KEYCODE_BUTTON_B to "B",
        KeyEvent.KEYCODE_BUTTON_X to "X",
        KeyEvent.KEYCODE_BUTTON_Y to "Y",
        KeyEvent.KEYCODE_BUTTON_L1 to "L1",
        KeyEvent.KEYCODE_BUTTON_R1 to "R1",
        KeyEvent.KEYCODE_BUTTON_START to "START",
        KeyEvent.KEYCODE_BUTTON_SELECT to "SELECT"
    )

    private val defaults = mapOf(
        KeyEvent.KEYCODE_BUTTON_A to Act.CLICK,
        KeyEvent.KEYCODE_BUTTON_B to Act.BACK,
        KeyEvent.KEYCODE_BUTTON_X to Act.LONG,
        KeyEvent.KEYCODE_BUTTON_Y to Act.HOME,
        KeyEvent.KEYCODE_BUTTON_L1 to Act.SCROLL_UP,
        KeyEvent.KEYCODE_BUTTON_R1 to Act.SCROLL_DOWN,
        KeyEvent.KEYCODE_BUTTON_START to Act.RECENTS
    )

    /** Hành động đã gán cho nút; null nếu nút không nằm trong danh sách có thể gán. */
    fun actFor(p: SharedPreferences, code: Int): Act? {
        if (buttons.none { it.first == code }) return null
        val name = p.getString("k$code", null)
        return runCatching { Act.valueOf(name ?: "") }.getOrNull() ?: defaults[code] ?: Act.NONE
    }
}
