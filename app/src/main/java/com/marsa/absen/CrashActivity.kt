package com.marsa.absen

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Layar darurat: view biasa, tanpa Compose/Hilt, jalan di proses ":crash". */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra(EXTRA_TRACE) ?: "(tidak ada data)"
        val pad = (16 * resources.displayMetrics.density).toInt()

        val text = TextView(this).apply {
            this.text = trace
            textSize = 11f
            setTextIsSelectable(true)
        }
        val button = Button(this).apply {
            this.text = "Salin log crash"
            setOnClickListener {
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", trace))
                Toast.makeText(context, "Tersalin", Toast.LENGTH_SHORT).show()
            }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            addView(button)
            addView(ScrollView(context).apply { addView(text) })
        })
    }

    companion object {
        const val EXTRA_TRACE = "trace"
    }
}
