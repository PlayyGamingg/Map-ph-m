package com.example.tvmouse

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.SharedPreferences
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import kotlin.math.abs

class MouseService : AccessibilityService() {

    companion object {
        private const val SPEED = 22f        // px mỗi khung hình (~60 khung/giây), tăng để chuột nhanh hơn
        private const val DEADZONE = 0.15f   // vùng chết của cần analog
    }

    private lateinit var wm: WindowManager
    private lateinit var prefs: SharedPreferences
    private lateinit var lp: WindowManager.LayoutParams
    private var cursor: CursorView? = null
    private val handler = Handler(Looper.getMainLooper())

    private var on = false
    private var x = 0f
    private var y = 0f
    private var sx = 0f; private var sy = 0f   // cần analog phải
    private var dx = 0; private var dy = 0     // D-pad (dự phòng cho Android < 14)
    private var w = 0; private var h = 0

    private val loop = object : Runnable {
        override fun run() {
            if (!on) return
            var mx = sx; var my = sy
            if (dx != 0 || dy != 0) { mx = dx.toFloat(); my = dy.toFloat() }
            if (abs(mx) > DEADZONE || abs(my) > DEADZONE) {
                x = (x + mx * abs(mx) * SPEED).coerceIn(0f, (w - 1).toFloat())
                y = (y + my * abs(my) * SPEED).coerceIn(0f, (h - 1).toFloat())
                moveCursor()
            }
            handler.postDelayed(this, 16)
        }
    }

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences(Shared.PREFS, MODE_PRIVATE)

        val info = serviceInfo
        if (Build.VERSION.SDK_INT >= 34) info.motionEventSources = InputDevice.SOURCE_JOYSTICK
        serviceInfo = info

        val dm = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(dm)
        w = dm.widthPixels; h = dm.heightPixels
        x = w / 2f; y = h / 2f

        lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        cursor = CursorView(this).also {
            it.visibility = View.GONE
            wm.addView(it, lp)
        }
    }

    private fun moveCursor() {
        lp.x = x.toInt(); lp.y = y.toInt()
        cursor?.let { wm.updateViewLayout(it, lp) }
    }

    private fun toggle() {
        on = !on
        sx = 0f; sy = 0f; dx = 0; dy = 0
        cursor?.visibility = if (on) View.VISIBLE else View.GONE
        if (on) { moveCursor(); handler.post(loop) } else handler.removeCallbacks(loop)
        Toast.makeText(this, if (on) "Chuột ảo: BẬT" else "Chuột ảo: TẮT", Toast.LENGTH_SHORT).show()
    }

    // Cần analog phải (Android 14+). Tay cầm Xbox/PS dùng Z/RZ; nếu tay cầm của bạn dùng RX/RY thì đổi ở đây.
    override fun onMotionEvent(event: MotionEvent) {
        if (!on) return
        if (event.source and InputDevice.SOURCE_JOYSTICK != 0 && event.action == MotionEvent.ACTION_MOVE) {
            sx = event.getAxisValue(MotionEvent.AXIS_Z)
            sy = event.getAxisValue(MotionEvent.AXIS_RZ)
        }
    }

    override fun onKeyEvent(e: KeyEvent): Boolean {
        val code = e.keyCode
        val down = e.action == KeyEvent.ACTION_DOWN

        // Nhấn cần phải (R3) để bật/tắt chuột ảo
        if (code == KeyEvent.KEYCODE_BUTTON_THUMBR) {
            if (down && e.repeatCount == 0) toggle()
            return true
        }
        if (!on) return false

        // Android < 14 không đọc được cần analog: dùng D-pad để di chuyển chuột
        if (Build.VERSION.SDK_INT < 34) {
            when (code) {
                KeyEvent.KEYCODE_DPAD_LEFT -> { dx = if (down) -1 else 0; return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { dx = if (down) 1 else 0; return true }
                KeyEvent.KEYCODE_DPAD_UP -> { dy = if (down) -1 else 0; return true }
                KeyEvent.KEYCODE_DPAD_DOWN -> { dy = if (down) 1 else 0; return true }
            }
        }

        val act = Shared.actFor(prefs, code) ?: return false
        if (act == Act.NONE) return false
        if (down && e.repeatCount == 0) run(act)
        return true
    }

    private fun run(act: Act) {
        when (act) {
            Act.CLICK -> gesture(x, y, x, y, 50)
            Act.LONG -> gesture(x, y, x, y, 700)
            Act.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            Act.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            Act.RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            Act.SCROLL_UP -> gesture(x, y, x, (y + 400f).coerceAtMost(h - 1f), 300)
            Act.SCROLL_DOWN -> gesture(x, y, x, (y - 400f).coerceAtLeast(0f), 300)
            Act.NONE -> {}
        }
    }

    private fun gesture(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long) {
        val p = Path().apply { moveTo(x1, y1); if (x1 != x2 || y1 != y2) lineTo(x2, y2) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, ms)).build()
        dispatchGesture(g, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(loop)
        cursor?.let { wm.removeView(it) }
        super.onDestroy()
    }
}
