package com.example.myfocus

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class FocusService : AccessibilityService() {

    private lateinit var wm: WindowManager
    private var rules: List<Rule> = emptyList()
    private var mode = "color"
    private var color = Color.BLACK
    private val overlays = mutableListOf<View>()

    // Kept as a field on purpose: SharedPreferences holds listeners weakly.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        loadSettings()
        clearOverlays() // recreate with the new look on the next event
    }

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        loadSettings()
        Prefs.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun loadSettings() {
        val p = Prefs.prefs(this)
        rules = Prefs.parseRules(Prefs.rulesText(this))
        mode = p.getString(Prefs.KEY_MODE, "color") ?: "color"
        color = try {
            Color.parseColor(p.getString(Prefs.KEY_COLOR, "#000000"))
        } catch (e: IllegalArgumentException) {
            Color.BLACK
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val pkgName = root.packageName?.toString()
        if (ScanState.scanning && pkgName == ScanState.pkg) scanTree(root)
        val appRules = rules.filter { it.pkg == pkgName }
        if (appRules.isEmpty()) {
            clearOverlays()
            return
        }

        val rects = mutableListOf<Rect>()
        for (rule in appRules) {
            val nodes = if (rule.type == "id") {
                root.findAccessibilityNodeInfosByViewId(rule.value)
            } else {
                root.findAccessibilityNodeInfosByText(rule.value) // matches text and content description
            }
            for (node in nodes.orEmpty()) {
                val target = climb(node, rule.up)
                if (!target.isVisibleToUser) continue
                val r = Rect()
                target.getBoundsInScreen(r)
                if (r.width() > 0 && r.height() > 0) rects.add(r)
            }
        }
        render(rects)
    }

    /** ID finder: remember which view IDs appear, and how many at once (avatars in a list repeat a lot). */
    private fun scanTree(root: AccessibilityNodeInfo) {
        val seen = HashMap<String, Int>()
        fun walk(n: AccessibilityNodeInfo) {
            val id = n.viewIdResourceName
            if (id != null) {
                val key = id + " [" + (n.className?.toString()?.substringAfterLast('.') ?: "?") + "]"
                seen[key] = (seen[key] ?: 0) + 1
            }
            for (i in 0 until n.childCount) n.getChild(i)?.let { walk(it) }
        }
        walk(root)
        for ((k, v) in seen) if (v > (ScanState.maxSeen[k] ?: 0)) ScanState.maxSeen[k] = v
    }

    private fun climb(node: AccessibilityNodeInfo, up: Int): AccessibilityNodeInfo {
        var n = node
        repeat(up) { n = n.parent ?: return n }
        return n
    }

    /** Reuse existing overlay views, add missing ones, remove extras (avoids flicker). */
    private fun render(rects: List<Rect>) {
        while (overlays.size > rects.size) {
            wm.removeView(overlays.removeAt(overlays.size - 1))
        }
        rects.forEachIndexed { i, r ->
            val lp = layoutParams(r)
            if (i < overlays.size) {
                wm.updateViewLayout(overlays[i], lp)
            } else {
                val v = View(this)
                v.setBackgroundColor(overlayColor())
                wm.addView(v, lp)
                overlays.add(v)
            }
        }
    }

    private fun blurEnabled() = mode == "blur" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    private fun overlayColor(): Int =
        if (blurEnabled()) (color and 0x00FFFFFF) or (0x99 shl 24) // light tint over the blur
        else (color and 0x00FFFFFF) or (0xFF shl 24)               // fully solid

    private fun layoutParams(r: Rect): WindowManager.LayoutParams {
        val lp = WindowManager.LayoutParams(
            r.width(), r.height(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or // taps and scrolls pass through
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = r.left
        lp.y = r.top
        if (blurEnabled()) {
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            lp.blurBehindRadius = 40
        }
        return lp
    }

    private fun clearOverlays() {
        if (!::wm.isInitialized) return
        overlays.forEach { runCatching { wm.removeView(it) } }
        overlays.clear()
    }

    override fun onInterrupt() = clearOverlays()

    override fun onDestroy() {
        Prefs.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        clearOverlays()
        super.onDestroy()
    }
}
