package com.example.tvmouse

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(Shared.PREFS, MODE_PRIVATE)
        val wrap = LinearLayout.LayoutParams.WRAP_CONTENT

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 48, 64, 48)
        }
        fun tv(s: String, size: Float) = TextView(this).apply {
            text = s; textSize = size; setTextColor(Color.WHITE); setPadding(0, 12, 0, 12)
        }

        root.addView(tv("TV Mouse", 28f))
        root.addView(tv(
            "1) Bật dịch vụ \"TV Mouse\" trong Trợ năng.\n" +
            "2) Nhấn cần phải (R3) để bật/tắt chuột ảo.\n" +
            "3) Cần phải di chuyển chuột (Android 14+). Android cũ hơn: dùng D-pad.\n" +
            "4) Khi chuột đang bật, các nút dưới đây thực hiện thao tác đã gán.", 16f))
        root.addView(Button(this).apply {
            text = "Mở cài đặt Trợ năng"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        val labels = Act.values().map { it.label }
        for ((code, name) in Shared.buttons) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(tv("Nút $name", 18f), LinearLayout.LayoutParams(0, wrap, 1f))
            val sp = Spinner(this)
            sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
            sp.setSelection((Shared.actFor(prefs, code) ?: Act.NONE).ordinal)
            sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    prefs.edit().putString("k$code", Act.values()[pos].name).apply()
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
            row.addView(sp, LinearLayout.LayoutParams(0, wrap, 1f))
            root.addView(row)
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
