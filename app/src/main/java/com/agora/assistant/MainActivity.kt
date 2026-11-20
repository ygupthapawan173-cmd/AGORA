package com.agora.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.text.InputType
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var startButton: Button
    private val requestCode = 401
    private val red = Color.rgb(255, 35, 48)
    private val white = Color.WHITE
    private val muted = Color.rgb(178, 178, 188)
    private val black = Color.rgb(7, 7, 10)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = black
        window.navigationBarColor = black
        window.decorView.systemUiVisibility = 0
        showHome()
    }

    private fun baseScreen(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(5, 5, 8), Color.rgb(19, 5, 8), Color.rgb(5, 5, 8)))
        }
    }

    private fun showHome() {
        root = baseScreen()
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(8), dp(2), dp(18))
        }
        header.addView(text("J", 30, red, true), LinearLayout.LayoutParams(dp(42), dp(52)))
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("JARVIS AI", 23, white, true), matchWrap())
            addView(text("PERSONAL AI ASSISTANT", 9, muted, true).apply { letterSpacing = .12f }, matchWrap())
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(actionButton("⚙", Color.rgb(70, 15, 24)) { showApiSettings() }.apply {
            textSize = 19f; minWidth = dp(48)
        }, LinearLayout.LayoutParams(dp(52), dp(48)))
        body.addView(header, matchWrap())

        val apiReady = GeminiApiClient.hasKey(this)
        body.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = panel(if (apiReady) Color.rgb(45, 190, 110) else red)
            addView(text(if (apiReady) "●  GEMINI CONNECTED" else "●  API KEY REQUIRED", 10, if (apiReady) Color.rgb(90, 235, 150) else Color.rgb(255, 130, 140), true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(text("SETUP  ›", 10, white, true).apply { setOnClickListener { showApiSettings() } }, matchWrap())
            setOnClickListener { showApiSettings() }
        }, matchWrap().apply { bottomMargin = dp(16) })

        body.addView(text("YOUR AI. YOUR WORLD.", 10, muted, true).apply {
            letterSpacing = .2f; gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(14))
        }, matchWrap())

        val welcome = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(20), dp(18), dp(20))
            background = gradientPanel(red)
        }
        val core = TextView(this).apply {
            text = "◈"; textSize = 64f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(white); gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(red, Color.rgb(75, 8, 20), black)).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(3), Color.rgb(255, 120, 130))
            }
        }
        welcome.addView(core, LinearLayout.LayoutParams(dp(138), dp(138)).apply { gravity = Gravity.CENTER })
        pulseOrb(core)
        welcome.addView(text("JARVIS CORE  •  ONLINE", 11, Color.rgb(255, 120, 130), true).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(2))
        }, matchWrap())
        welcome.addView(text("నమస్కారం! 👋", 23, white, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(4)) }, matchWrap())
        welcome.addView(text("How can I help you today?", 14, muted, false).apply { gravity = Gravity.CENTER }, matchWrap())
        body.addView(welcome, matchWrap().apply { bottomMargin = dp(16) })

        body.addView(text("QUICK ACCESS", 12, red, true).apply { letterSpacing = .12f; setPadding(dp(2), dp(8), 0, dp(10)) }, matchWrap())
        body.addView(row(
            feature("▤", "AI Chat") { showChat() },
            feature("🎙", "Voice") { showVoice() }
        ), matchWrap().apply { bottomMargin = dp(10) })
        body.addView(row(
            feature("▧", "Vision & OCR") { showInfo("Vision & OCR", "Image and screenshot analysis will be added in the next build.") },
            feature("</>", "Code") { showInfo("Code Assistant", "Code help screen is coming next.") }
        ), matchWrap().apply { bottomMargin = dp(10) })
        body.addView(row(
            feature("⌕", "Web Search") { openWebSearch() },
            feature("⚡", "Automation") { showInfo("Automation", "Actions will ask for your confirmation before running.") }
        ), matchWrap().apply { bottomMargin = dp(18) })

        body.addView(text(if (GeminiApiClient.hasKey(this)) "GEMINI AI • API KEY SAVED" else "GEMINI AI • API KEY NOT SET", 12, red, true)
            .apply { setPadding(dp(2), dp(4), 0, dp(8)) }, matchWrap())
        body.addView(text("JARVIS SYSTEM STATUS", 12, red, true).apply { setPadding(dp(2), dp(4), 0, dp(8)) }, matchWrap())
        status = text("STANDBY  •  Voice assistant is off", 13, white, false).apply {
            gravity = Gravity.CENTER; setPadding(dp(12), dp(15), dp(12), dp(15)); background = panel()
        }
        body.addView(status, matchWrap().apply { bottomMargin = dp(10) })

        startButton = actionButton("🎙  START HEY AGORA", red) { ensurePermissionsAndStart() }
        body.addView(startButton, matchWrap().apply { bottomMargin = dp(9) })
        body.addView(actionButton("■  STOP LISTENING", Color.rgb(95, 95, 105)) {
            stopService(Intent(this, WakeWordService::class.java))
            status.text = "LISTENING OFF"
            startButton.isEnabled = true
        }, matchWrap().apply { bottomMargin = dp(16) })

        body.addView(text("RECENT ACTIVITY", 12, red, true).apply { setPadding(dp(2), dp(4), 0, dp(8)) }, matchWrap())
        body.addView(infoCard("Welcome to JARVIS AI", "Telugu + English assistant", red), matchWrap())
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(bottomNav("Home"), matchWrap())
        setContentView(root)
        animateScreen(root)
    }

    private fun showChat() {
        root = baseScreen()
        root.addView(row(actionButton("‹", red) { showHome() }, text("JARVIS AI  •  CHAT", 18, white, true)), matchWrap())
        val scroll = ScrollView(this)
        val messages = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(18), 0, dp(12)) }
        messages.addView(bubble("నమస్కారం! నేను JARVIS AI. Gemini ద్వారా తెలుగులోనూ English లోనూ మాట్లాడవచ్చు.", false), matchWrap().apply { bottomMargin = dp(12) })
        scroll.addView(messages, ViewGroup.LayoutParams(-1, -1))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val inputRow = row()
        val input = EditText(this).apply {
            hint = "Ask JARVIS anything..."
            setHintTextColor(muted); setTextColor(white); textSize = 14f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 3
            setPadding(dp(14), dp(8), dp(14), dp(8)); background = panel()
        }
        inputRow.addView(input, LinearLayout.LayoutParams(0, dp(56), 1f).apply { rightMargin = dp(8) })
        inputRow.addView(actionButton("➤", red) {
            val q = input.text.toString().trim()
            if (q.isNotEmpty()) {
                if (!GeminiApiClient.hasKey(this)) {
                    showApiSettings()
                } else {
                    messages.addView(bubble(q, true), matchWrap().apply { bottomMargin = dp(10) })
                    val loading = text("JARVIS is thinking  •••", 13, Color.rgb(255, 120, 130), true)
                    messages.addView(loading, matchWrap().apply { bottomMargin = dp(12) })
                    input.text.clear()
                    scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                    Thread {
                        val answer = GeminiApiClient.ask(this, q)
                        runOnUiThread {
                            messages.removeView(loading)
                            messages.addView(bubble(answer, false), matchWrap().apply { bottomMargin = dp(12) })
                            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                        }
                    }.start()
                }
            }
        }, LinearLayout.LayoutParams(dp(58), dp(56)))
        root.addView(inputRow, matchWrap().apply { topMargin = dp(10) })
        root.addView(text("Powered by Gemini • Telugu + English", 10, muted, false).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(7), 0, 0)
        }, matchWrap())
        setContentView(root)
        animateScreen(root)
    }

    private fun showApiSettings() {
        val field = EditText(this).apply {
            hint = "Paste Google Gemini API key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(field, matchWrap())
            addView(text("Key is encrypted on this device with Android Keystore. Never share it in chat or commit it to GitHub.", 12, muted, false)
                .apply { setPadding(0, dp(10), 0, 0) }, matchWrap())
        }
        AlertDialog.Builder(this)
            .setTitle("Gemini API Settings")
            .setView(container)
            .setPositiveButton("SAVE KEY") { _, _ ->
                try {
                    GeminiApiClient.saveKey(this, field.text.toString())
                    Toast.makeText(this, "Gemini API key saved securely on this device.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, e.message ?: "Could not save API key", Toast.LENGTH_LONG).show()
                }
            }
            .setNeutralButton("REMOVE KEY") { _, _ ->
                GeminiApiClient.clearKey(this)
                Toast.makeText(this, "Gemini key removed from this device.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showVoice() {
        root = baseScreen()
        root.addView(actionButton("‹  BACK", red) { showHome() }, matchWrap())
        root.addView(text("HEY AGORA", 22, white, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(28), 0, dp(8)) }, matchWrap())
        root.addView(text("Voice Assistant", 13, muted, false).apply { gravity = Gravity.CENTER }, matchWrap())
        val orb = TextView(this).apply {
            text = "A"; textSize = 66f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(white); gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(red, Color.DKGRAY, black)).apply {
                shape = GradientDrawable.OVAL; setStroke(dp(3), white)
            }
        }
        root.addView(orb, LinearLayout.LayoutParams(dp(210), dp(210)).apply { gravity = Gravity.CENTER; topMargin = dp(48); bottomMargin = dp(28) })
        pulseOrb(orb)
        root.addView(text("చెప్పండి... నేను వింటున్నాను", 18, white, true).apply { gravity = Gravity.CENTER }, matchWrap())
        root.addView(text("Telugu  •  English", 13, muted, false).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(24)) }, matchWrap())
        root.addView(voiceWaveform(), LinearLayout.LayoutParams(-1, dp(54)).apply { bottomMargin = dp(18) })
        root.addView(actionButton("🎙  START HEY AGORA", red) { ensurePermissionsAndStart() }, matchWrap())
        root.addView(actionButton("■  STOP", Color.DKGRAY) {
            stopService(Intent(this, WakeWordService::class.java))
            if (::startButton.isInitialized) startButton.isEnabled = true
            Toast.makeText(this, "Voice listening stopped", Toast.LENGTH_SHORT).show()
        }, matchWrap().apply { topMargin = dp(10) })
        root.addView(View(this), LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        animateScreen(root)
    }

    private fun showInfo(title: String, detail: String) {
        root = baseScreen()
        root.addView(actionButton("‹  BACK", red) { showHome() }, matchWrap())
        root.addView(text(title, 24, white, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(40), 0, dp(18)) }, matchWrap())
        root.addView(infoCard(title, detail, red), matchWrap())
        setContentView(root)
        animateScreen(root)
    }


    private fun animateScreen(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                child.alpha = 0f
                child.translationY = dp(12).toFloat()
                child.animate().alpha(1f).translationY(0f).setStartDelay(i * 45L)
                    .setDuration(320L).setInterpolator(DecelerateInterpolator()).start()
            }
        }
    }

    private fun pulseOrb(view: View) {
        ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.055f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.055f, 1f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.88f, 1f, 0.88f)
        ).apply {
            duration = 1900L
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun voiceWaveform(): View {
        val wave = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val heights = intArrayOf(12, 22, 36, 18, 44, 25, 14, 34, 20, 40, 16, 28, 12)
        heights.forEachIndexed { index, height ->
            val bar = View(this).apply {
                background = GradientDrawable().apply {
                    setColor(if (index % 3 == 0) white else red)
                    cornerRadius = dp(5).toFloat()
                }
            }
            wave.addView(bar, LinearLayout.LayoutParams(dp(4), dp(height)).apply {
                leftMargin = dp(3); rightMargin = dp(3)
            })
            ObjectAnimator.ofFloat(bar, View.SCALE_Y, 0.45f, 1.25f, 0.55f).apply {
                duration = 420L + (index % 5) * 110L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                startDelay = index * 55L
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                start()
            }
        }
        return wave
    }

    private fun bubble(message: String, user: Boolean): TextView = text(message, 15, white, false).apply {
        setPadding(dp(15), dp(13), dp(15), dp(13))
        background = panel(if (user) red else Color.rgb(75, 75, 82))
        gravity = if (user) Gravity.END else Gravity.START
    }

    private fun feature(icon: String, label: String, click: () -> Unit): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), dp(12))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(55, 15, 23), Color.rgb(22, 20, 25))).apply {
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), Color.rgb(115, 55, 66))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { click() }
        }
        card.addView(text(icon, 25, Color.rgb(255, 105, 120), true).apply { gravity = Gravity.CENTER }, matchWrap())
        card.addView(text(label, 13, white, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(7), 0, 0) }, matchWrap())
        return card
    }

    private fun bottomNav(selected: String): View = row(
        navItem("⌂", "Home", selected == "Home") { showHome() },
        navItem("▤", "Chat", selected == "Chat") { showChat() },
        navItem("🎙", "Voice", selected == "Voice") { showVoice() }
    ).apply { setPadding(0, dp(8), 0, dp(4)); background = panel() }

    private fun navItem(icon: String, label: String, active: Boolean, click: () -> Unit): View =
        TextView(this).apply {
            text = "$icon\n$label"; textSize = 12f; gravity = Gravity.CENTER
            setTextColor(if (active) red else white); setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener { click() }
            layoutParams = LinearLayout.LayoutParams(0, dp(58), 1f)
        }

    private fun ensurePermissionsAndStart() {
        val missing = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            missing += Manifest.permission.POST_NOTIFICATIONS
        }
        if (missing.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) requestPermissions(missing.toTypedArray(), requestCode)
        else startWakeService()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startWakeService()
        else Toast.makeText(this, "Microphone permission is required for voice assistant.", Toast.LENGTH_LONG).show()
    }

    private fun startWakeService() {
        try {
            val i = Intent(this, WakeWordService::class.java).setAction(WakeWordService.ACTION_START)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            if (::status.isInitialized) status.text = "LISTENING STARTED  •  Wake phrase: Hey Agora"
            if (::startButton.isInitialized) startButton.isEnabled = false
        } catch (e: Exception) {
            if (::status.isInitialized) status.text = "Could not start voice service: ${e.message}"
            if (::startButton.isInitialized) startButton.isEnabled = true
        }
    }

    private fun openWebSearch() {
        val i = Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, "AI assistant")
        try { startActivity(i) } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com")))
        }
    }

    private fun infoCard(title: String, desc: String, stroke: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(14), dp(15), dp(14)); background = panel(stroke)
        addView(text(title, 15, white, true), matchWrap())
        addView(text(desc, 12, muted, false).apply { setPadding(0, dp(5), 0, 0) }, matchWrap())
    }

    private fun actionButton(label: String, color: Int, click: () -> Unit) = Button(this).apply {
        text = label; textSize = 14f; isAllCaps = false; setTextColor(white)
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(color, Color.rgb(75, 8, 14))).apply {
            cornerRadius = dp(17).toFloat(); setStroke(dp(1), Color.rgb(230, 230, 235))
        }
        setOnClickListener { click() }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun row(vararg views: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        views.forEachIndexed { index, view ->
            addView(view, LinearLayout.LayoutParams(0, dp(76), 1f).apply {
                if (index > 0) leftMargin = dp(8)
            })
        }
    }

    private fun panel(stroke: Int = Color.rgb(105, 105, 115)) = GradientDrawable().apply {
        setColor(Color.rgb(20, 20, 24)); cornerRadius = dp(17).toFloat(); setStroke(dp(1), stroke)
    }

    private fun gradientPanel(accent: Int) = GradientDrawable(GradientDrawable.Orientation.TL_BR,
        intArrayOf(Color.rgb(45, 8, 13), Color.rgb(22, 18, 20), Color.rgb(8, 8, 10))).apply {
        cornerRadius = dp(22).toFloat(); setStroke(dp(1), accent)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
