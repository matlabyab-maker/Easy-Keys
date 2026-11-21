package com.easykeyboard.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
        }
        layout.addView(TextView(this).apply {
            text = "Easy Keys\n\nبرای استفاده، ابتدا صفحه‌کلید را فعال و سپس انتخاب کنید."
            textSize = 21f
        })
        layout.addView(Button(this).apply {
            text = "فعال‌سازی صفحه‌کلید"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        layout.addView(Button(this).apply {
            text = "انتخاب صفحه‌کلید"
            setOnClickListener {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        })
        setContentView(layout)
    }
}
