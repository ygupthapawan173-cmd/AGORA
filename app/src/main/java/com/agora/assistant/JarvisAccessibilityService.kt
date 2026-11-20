package com.agora.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.content.ComponentName
import android.content.Intent

/** User-controlled local screen reader/actions. No screen text is sent to a network service. */
class JarvisAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: JarvisAccessibilityService? = null
            private set

        fun isEnabledByUser(context: android.content.Context): Boolean {
            val expected = ComponentName(context, JarvisAccessibilityService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }
    }

    @Volatile private var lastPackage: String = ""
    @Volatile private var lastScreenText: String = ""
    @Volatile private var lastSnapshotAt: Long = 0L
    @Volatile private var pendingApprovedAction: String? = null
    @Volatile private var pendingTargetPackage: String = ""
    @Volatile private var pendingActionAt: Long = 0L
    @Volatile private var pendingSnapshotHash: Int = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        refreshPackageScope()
    }

    fun refreshPackageScope() {
        val chosen = AccessibilityAppRepository.selected(this)
        if (lastPackage.isNotBlank() && !chosen.contains(lastPackage)) {
            lastPackage = ""
            lastScreenText = ""
            lastSnapshotAt = 0L
            pendingApprovedAction = null
            pendingTargetPackage = ""
            pendingSnapshotHash = 0
        }
        if (AccessibilityAppRepository.isPaused(this)) clearSnapshot()
        val scope = if (chosen.isEmpty()) arrayOf(packageName) else chosen.toTypedArray()
        try {
            serviceInfo = serviceInfo.apply {
                packageNames = scope
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                notificationTimeout = 120
            }
        } catch (_: Exception) { }
    }

    fun clearSnapshot() {
        lastPackage = ""
        lastScreenText = ""
        lastSnapshotAt = 0L
        pendingApprovedAction = null
        pendingTargetPackage = ""
        pendingActionAt = 0L
        pendingSnapshotHash = 0
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (!AccessibilityAppRepository.isAllowed(this, pkg)) return
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return
        val freshSnapshot = readTree(root).take(5000)
        // An approved action is bound to the same selected package and same visible screen snapshot.
        val queued = pendingApprovedAction
        if (queued != null) {
            if (pkg == pendingTargetPackage &&
                System.currentTimeMillis() - pendingActionAt <= 30000L &&
                freshSnapshot.hashCode() == pendingSnapshotHash &&
                !AccessibilityAppRepository.isSensitivePackage(pkg) && !containsSensitiveContent(root)) {
                pendingApprovedAction = null
                pendingSnapshotHash = 0
                val result = executeActionOnRoot(root, queued)
                AccessibilityAuditLog.record(this, pkg, queued, result)
            } else if (pkg == pendingTargetPackage) {
                pendingApprovedAction = null
                pendingSnapshotHash = 0
                AccessibilityAuditLog.record(this, pkg, queued, "Cancelled: target screen changed, expired, or privacy guard blocked it")
            }
        }
        lastPackage = pkg
        // Refresh in-memory only. Nothing is persisted or sent to any AI provider.
        lastScreenText = freshSnapshot
        lastSnapshotAt = System.currentTimeMillis()
    }

    fun currentScreenSummary(): String {
        if (lastPackage.isBlank() || System.currentTimeMillis() - lastSnapshotAt > 60000L) {
            return "No recent screen snapshot. Open a selected app, then return to JARVIS and try again."
        }
        return "LOCAL SCREEN SNAPSHOT · ${lastPackage.substringAfterLast('.')}\n\n$lastScreenText"
    }

    /** Queues one action against the last selected app. It runs only after the user approves and returns to that app. */
    fun approveActionAndReturn(rawAction: String): String {
        val action = rawAction.trim()
        if (lastPackage.isBlank() || System.currentTimeMillis() - lastSnapshotAt > 60000L) {
            return "No recent selected-app screen. Open the app first, then return to JARVIS."
        }
        if (!isSupportedAction(action)) return "Supported: Back, Scroll down, Scroll up, or Click [exact visible label]."
        if (action.startsWith("click ", true)) {
            val label = action.substringAfter(' ').trim()
            val unsafe = listOf("pay", "transfer", "send money", "delete", "remove account", "password", "otp", "pin", "confirm purchase", "buy now", "submit payment")
            if (unsafe.any { label.contains(it, true) }) return "Action blocked: this label may trigger a sensitive or irreversible action."
        }
        val targetPackage = lastPackage
        val targetSnapshot = lastScreenText.hashCode()
        val launch = packageManager.getLaunchIntentForPackage(targetPackage)
            ?: return "Could not reopen the selected app. Action cancelled."
        pendingApprovedAction = action
        pendingTargetPackage = targetPackage
        pendingActionAt = System.currentTimeMillis()
        pendingSnapshotHash = targetSnapshot
        try {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(launch)
        } catch (_: Exception) {
            pendingApprovedAction = null
            pendingSnapshotHash = 0
            return "Could not reopen the selected app. Action cancelled."
        }
        return "Approved. Reopening ${targetPackage.substringAfterLast('.')}; the one-time action runs only if the selected app returns to the same screen within 30 seconds and Privacy Guard allows it."
    }

    private fun isSupportedAction(action: String): Boolean =
        action.equals("back", true) || action.equals("scroll down", true) || action.equals("scroll up", true) ||
            action.equals("swipe left", true) || action.equals("swipe right", true) ||
            action.equals("swipe up", true) || action.equals("swipe down", true) ||
            (action.startsWith("click ", true) && action.substringAfter(' ').trim().isNotBlank() && action.substringAfter(' ').trim().length <= 80)

    private fun executeActionOnRoot(root: AccessibilityNodeInfo, action: String): String = when {
        action.equals("back", true) -> if (performGlobalAction(GLOBAL_ACTION_BACK)) "Back action performed." else "Back action was not available."
        action.equals("scroll down", true) -> scroll(root, forward = true)
        action.equals("scroll up", true) -> scroll(root, forward = false)
        action.startsWith("swipe ", true) -> swipe(action.substringAfter(' ').trim().lowercase())
        action.startsWith("click ", true) -> clickVisibleText(root, action.substringAfter(' ').trim())
        else -> "Unsupported action."
    }


    private fun swipe(direction: String): String {
        val dm = resources.displayMetrics
        val w = dm.widthPixels.toFloat()
        val h = dm.heightPixels.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val distanceX = w * 0.34f
        val distanceY = h * 0.28f
        val (sx, sy, ex, ey) = when (direction) {
            "left" -> listOf(cx + distanceX / 2f, cy, cx - distanceX / 2f, cy)
            "right" -> listOf(cx - distanceX / 2f, cy, cx + distanceX / 2f, cy)
            "up" -> listOf(cx, cy + distanceY / 2f, cx, cy - distanceY / 2f)
            "down" -> listOf(cx, cy - distanceY / 2f, cx, cy + distanceY / 2f)
            else -> return "Unsupported swipe direction."
        }
        val path = Path().apply { moveTo(sx, sy); lineTo(ex, ey) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 320L))
            .build()
        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                AccessibilityAuditLog.record(this@JarvisAccessibilityService, lastPackage, "swipe $direction", "Gesture completed")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                AccessibilityAuditLog.record(this@JarvisAccessibilityService, lastPackage, "swipe $direction", "Gesture cancelled by Android or app")
            }
        }, Handler(Looper.getMainLooper()))
        return if (dispatched) "Swipe $direction dispatched to the selected app." else "Android did not accept the swipe gesture."
    }

    private fun scroll(root: AccessibilityNodeInfo, forward: Boolean): String {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectScrollable(root, nodes)
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        val done = nodes.firstOrNull { it.isScrollable && it.performAction(action) } != null
        return if (done) if (forward) "Scrolled down." else "Scrolled up." else "No scrollable area accepted the action."
    }

    private fun collectScrollable(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        if (node.isScrollable) out.add(node)
        for (i in 0 until node.childCount) node.getChild(i)?.let { collectScrollable(it, out) }
    }

    private fun clickVisibleText(root: AccessibilityNodeInfo, label: String): String {
        if (label.isBlank() || label.length > 80) return "Enter a short visible button label."
        val unsafe = listOf("pay", "transfer", "send money", "delete", "remove account", "password", "otp", "pin", "confirm purchase", "buy now", "submit payment")
        if (unsafe.any { label.contains(it, true) }) return "Action blocked: this label may trigger a sensitive or irreversible action."
        val matches = try { root.findAccessibilityNodeInfosByText(label) } catch (_: Exception) { emptyList() }
        val exact = matches.filter { n ->
            val text = (n.text?.toString() ?: n.contentDescription?.toString()).orEmpty()
            text.equals(label, ignoreCase = true)
        }
        if (exact.size != 1) return if (exact.isEmpty()) "Could not find an exact visible match for '$label'." else "More than one match found. Use a more specific label."
        var target: AccessibilityNodeInfo? = exact.first()
        while (target != null) {
            if (target.isClickable) return if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) "Clicked '$label'." else "The app declined the click."
            target = target.parent
        }
        return "The matching item is not clickable."
    }

    private fun containsSensitiveContent(root: AccessibilityNodeInfo): Boolean {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        val patterns = listOf(
            "enter otp", "one time password", "verification code", "upi pin", "enter pin", "cvv", "card number", "password", "passcode", "bank account", "payment details",
            "otp enter", "పాస్‌వర్డ్", "పాస్వర్డ్", "ఓటీపీ", "ఓటిపి", "పిన్ నంబర్", "యూపీఐ పిన్", "సివివి", "కార్డు నంబర్", "చెల్లింపు వివరాలు", "బ్యాంక్ ఖాతా"
        )
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            if (node.isPassword) return true
            val text = (node.text?.toString().orEmpty() + " " + node.contentDescription?.toString().orEmpty() + " " + node.hintText?.toString().orEmpty()).lowercase()
            if (patterns.any { text.contains(it) }) return true
            for (i in 0 until node.childCount) node.getChild(i)?.let { stack.add(it) }
        }
        return false
    }

    private fun readTree(root: AccessibilityNodeInfo): String {
        if (containsSensitiveContent(root)) return "Privacy Guard: this screen appears to contain sensitive information."
        val lines = LinkedHashSet<String>()
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        while (stack.isNotEmpty() && lines.size < 120) {
            val node = stack.removeLast()
            val text = node.text?.toString()?.trim().orEmpty()
            val desc = node.contentDescription?.toString()?.trim().orEmpty()
            if (text.isNotBlank() && !node.isPassword) lines.add(text)
            else if (desc.isNotBlank() && !node.isPassword) lines.add(desc)
            for (i in 0 until node.childCount) node.getChild(i)?.let { stack.add(it) }
        }
        return lines.joinToString("\n")
    }

    override fun onInterrupt() { clearSnapshot() }

    override fun onDestroy() {
        if (instance === this) instance = null
        clearSnapshot()
        super.onDestroy()
    }
}
