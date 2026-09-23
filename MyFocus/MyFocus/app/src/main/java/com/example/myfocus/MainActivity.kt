package com.example.myfocus

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val p = Prefs.prefs(this)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(pad, pad, pad, pad)

        fun label(t: String) = TextView(this).also { it.text = t; it.setPadding(0, pad, 0, pad / 2) }

        col.addView(label("1. Turn on \"My Focus\" in the accessibility settings."))
        col.addView(Button(this).also {
            it.text = "Open accessibility settings"
            it.setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        col.addView(label("2. Rules: package | id or text | value | optional parents to climb"))
        val rulesBox = EditText(this).also {
            it.setText(Prefs.rulesText(this))
            it.minLines = 8
            it.gravity = Gravity.TOP
            it.typeface = Typeface.MONOSPACE
        }
        col.addView(rulesBox)

        col.addView(label("3. How hidden elements look"))
        val solid = RadioButton(this).also { it.text = "Solid color"; it.id = 1 }
        val blur = RadioButton(this).also { it.text = "Blur (Android 12+, device dependent)"; it.id = 2 }
        val group = RadioGroup(this)
        group.addView(solid)
        group.addView(blur)
        group.check(if (p.getString(Prefs.KEY_MODE, "color") == "blur") 2 else 1)
        col.addView(group)

        col.addView(label("Color (hex, for example #000000 for black, #FFFFFF for white)"))
        val colorBox = EditText(this).also { it.setText(p.getString(Prefs.KEY_COLOR, "#000000")) }
        col.addView(colorBox)

        col.addView(Button(this).also {
            it.text = "Save"
            it.setOnClickListener {
                val hex = colorBox.text.toString().trim()
                try {
                    Color.parseColor(hex)
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(this, "Invalid color", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                p.edit()
                    .putString(Prefs.KEY_RULES, rulesBox.text.toString())
                    .putString(Prefs.KEY_MODE, if (blur.isChecked) "blur" else "color")
                    .putString(Prefs.KEY_COLOR, hex)
                    .apply()
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            }
        })

        col.addView(label("4. ID finder: start a scan, open the app and visit its screens (Chats, Groups...), then come back and stop"))
        val scanPkg = EditText(this).also { it.setText(ScanState.pkg) }
        col.addView(scanPkg)
        val result = TextView(this).also { it.setTextIsSelectable(true); it.typeface = Typeface.MONOSPACE }
        col.addView(Button(this).also {
            it.text = "Start scan"
            it.setOnClickListener {
                ScanState.pkg = scanPkg.text.toString().trim()
                ScanState.maxSeen.clear()
                ScanState.scanning = true
                result.text = "Scanning... now open ${ScanState.pkg} and browse its screens."
            }
        })
        col.addView(Button(this).also {
            it.text = "Stop and show IDs"
            it.setOnClickListener {
                ScanState.scanning = false
                val filter = Regex("Image|photo|avatar|pic|icon", RegexOption.IGNORE_CASE)
                val lines = ScanState.maxSeen.entries
                    .filter { e -> filter.containsMatchIn(e.key) }
                    .sortedByDescending { e -> e.value }
                    .take(40)
                    .joinToString("\n") { e -> "${e.value}x  ${e.key}" }
                result.text = if (lines.isEmpty()) "Nothing found. Is the service turned on?" else lines
            }
        })
        col.addView(result)

        val scroll = ScrollView(this)
        scroll.addView(col)
        setContentView(scroll)
    }
}
