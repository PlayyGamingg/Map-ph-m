package com.example.tvmouse

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

class CursorView(c: Context) : View(c) {
    private val d = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2.5f
    }
    private val path = Path().apply {
        moveTo(1f, 1f); lineTo(1f, 40f); lineTo(10f, 31f); lineTo(18f, 48f)
        lineTo(25f, 45f); lineTo(17f, 28f); lineTo(30f, 28f); close()
    }

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension((34 * d).toInt(), (52 * d).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        canvas.scale(d, d)
        canvas.drawPath(path, fill)
        canvas.drawPath(path, line)
    }
}
