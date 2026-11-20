package com.agora.assistant

import android.Manifest
import android.app.Activity
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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var startButton: Button
    private val requestCode = 401

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(4, 8, 16)
        window.navigationBarColor = Color.rgb(4, 8, 16)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(4, 10, 22), Color.rgb(7, 18, 32), Color.rgb(17, 10, 22)))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)

        val brand = TextView(this).apply {
            text = "🔱  AGORA"
            textSize = 31f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(2))
        }
        content.addView(brand, matchWrap())
        val subtitle = TextView(this).apply {
            text = "INTELLIGENT VOICE ASSISTANT  •  SHIVA EDITION"
            textSize = 10f; letterSpacing = .12f; setTextColor(Color.rgb(32, 223, 255)); gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(18))
        }
        content.addView(subtitle, matchWrap())

        val orb = TextView(this).apply {
            text = "ॐ\n🔱"
            textSize = 38f; gravity = Gravity.CENTER; setTextColor(Color.rgb(255, 181, 46))
            background = glow(Color.rgb(9, 31, 52), Color.rgb(32, 223, 255))
            setPadding(0, dp(18), 0, dp(18))
        }
        content.addView(orb, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(150)).apply { bottomMargin = dp(14) })

        status = TextView(this).apply {
            text = "STATUS  •  STANDBY\nSay “Hey Agora” after enabling listening."
            textSize = 14f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = panel(); setPadding(dp(12), dp(14), dp(12), dp(14))
        }
        content.addView(status, matchWrap().apply { bottomMargin = dp(14) })

        startButton = actionButton("🎙  START HEY AGORA", Color.rgb(255, 153, 25)) {
            ensurePermissionsAndStart()
        }
        content.addView(startButton, matchWrap().apply { bottomMargin = dp(10) })
        content.addView(actionButton("■  STOP LISTENING", Color.rgb(32, 223, 255)) {
            stopService(Intent(this, WakeWordService::class.java))
            status.text = "STATUS  •  LISTENING OFF"
            startButton.isEnabled = true
        }, matchWrap().apply { bottomMargin = dp(18) })

        content.addView(sectionTitle("CORE SYSTEMS"), matchWrap())
        val cards = listOf(
            Triple("🧠  AI INTELLIGENCE", "Offline command brain + optional local model", 0xFF20DFFF.toInt()),
            Triple("📱  PHONE CONTROL", "Open apps, settings, dialer and safe intents", 0xFF39E879.toInt()),
            Triple("🌐  ONLINE MODE", "Web search and online information", 0xFF39C8FF.toInt()),
            Triple("🗣  MULTI-LANGUAGE", "Telugu • English • Hindi", 0xFFB56CFF.toInt()),
            Triple("🔊  VOICE RESPONSE", "Android Text-to-Speech", 0xFFFF4DA6.toInt()),
            Triple("🔄  SELF UPDATE", "Signed APK update flow (next phase)", 0xFFFFB52E.toInt())
        )
        cards.forEach { (title, desc, color) -> content.addView(infoCard(title, desc, color), matchWrap().apply { bottomMargin = dp(9) }) }

        content.addView(actionButton("⚙  APP SETTINGS", Color.rgb(160, 115, 255)) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(android.net.Uri.parse("package:$packageName")))
        }, matchWrap().apply { topMargin = dp(8) })

        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun ensurePermissionsAndStart() {
        val missing = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            missing += Manifest.permission.POST_NOTIFICATIONS
        }
        if (missing.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
            requestPermissions(missing.toTypedArray(), requestCode)
        } else startWakeService()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startWakeService()
        else Toast.makeText(this, "Microphone permission is required for Hey Agora.", Toast.LENGTH_LONG).show()
    }

    private fun startWakeService() {
        try {
            val i = Intent(this, WakeWordService::class.java).setAction(WakeWordService.ACTION_START)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            status.text = "STATUS  •  STARTING\nWake phrase: Hey Agora"
            startButton.isEnabled = false
        } catch (e: Exception) {
            status.text = "Could not start microphone service: ${e.message}"
            startButton.isEnabled = true
        }
    }

    private fun sectionTitle(s: String) = TextView(this).apply {
        text = s; textSize = 12f; letterSpacing = .16f; typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(255, 181, 46)); setPadding(dp(2), dp(8), 0, dp(9))
    }

    private fun infoCard(title: String, desc: String, color: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(13), dp(15), dp(13)); background = panel(color)
        addView(TextView(this@MainActivity).apply { text = title; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(color) }, matchWrap())
        addView(TextView(this@MainActivity).apply { text = desc; textSize = 12f; setTextColor(Color.LTGRAY); setPadding(0, dp(4), 0, 0) }, matchWrap())
    }

    private fun actionButton(label: String, color: Int, click: () -> Unit) = Button(this).apply {
        text = label; textSize = 14f; isAllCaps = false; setTextColor(Color.WHITE)
        background = glow(Color.rgb(11, 25, 40), color); setOnClickListener { click() }
    }

    private fun panel(stroke: Int = Color.rgb(32, 115, 150)) = GradientDrawable().apply {
        setColor(Color.rgb(8, 20, 34)); cornerRadius = dp(15).toFloat(); setStroke(dp(1), stroke)
    }
    private fun glow(fill: Int, stroke: Int) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(18).toFloat(); setStroke(dp(2), stroke)
    }
    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
