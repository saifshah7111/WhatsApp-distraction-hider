package com.example.myfocus

import android.content.Context
import android.content.SharedPreferences

/** One line of the rules list: which app, how to find the element, what to look for. */
data class Rule(val pkg: String, val type: String, val value: String, val up: Int)

object Prefs {
    const val KEY_RULES = "rules"
    const val KEY_MODE = "mode"    // "color" or "blur"
    const val KEY_COLOR = "color"  // hex, for example #000000

    const val DEFAULT_RULES = """# package | id or text | value | optional: parents to climb
# Examples (replace with a real app and element):
# com.example.app | id | com.example.app:id/feed_container
# com.example.app | text | Suggested for you | 2
"""

    fun prefs(c: Context): SharedPreferences =
        c.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)

    fun rulesText(c: Context): String =
        prefs(c).getString(KEY_RULES, DEFAULT_RULES) ?: DEFAULT_RULES

    fun parseRules(text: String): List<Rule> =
        text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val p = line.split("|").map { it.trim() }
                val type = p.getOrNull(1)?.lowercase()
                if (p.size < 3 || (type != "id" && type != "text")) null
                else Rule(p[0], type, p[2], p.getOrNull(3)?.toIntOrNull() ?: 0)
            }
}

/** Used by the "ID finder": the service fills it while scanning, the app screen shows it. */
object ScanState {
    @Volatile var scanning = false
    var pkg = "com.whatsapp"
    val maxSeen = HashMap<String, Int>() // "viewId [ClassName]" -> most seen on one screen at once
}
