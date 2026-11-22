package com.example.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.ClaudeClassicApplication
import com.example.MainActivity
import com.example.R
import com.example.data.launcher.ClaudeLauncher
import com.example.notifications.ClaudeNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var rootView: FrameLayout? = null
    private var bubbleContainer: FrameLayout? = null
    private var panelContainer: LinearLayout? = null
    private var statusTextView: TextView? = null
    private var statusDotView: View? = null
    private var quickInput: EditText? = null
    private var newChatBtn: TextView? = null
    private var continueChatBtn: TextView? = null
    private var params: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var isExpanded = false
    private var continueLatestConversation = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val app = application as ClaudeClassicApplication
        app.container.notificationManager.ensureChannels()
        startForegroundSafe()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        serviceScope.launch {
            val savedSettings = app.container.settingsDataStore.settingsFlow.first()
            createOverlayViews(savedSettings.bubbleX, savedSettings.bubbleY)
            observeStatusUpdates(app)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_HIDE) {
            hideAndStop()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startForegroundSafe() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            404,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(
            this,
            ClaudeNotificationManager.CHANNEL_OVERLAY
        )
            .setSmallIcon(R.drawable.ic_stat_claude)
            .setContentTitle("כפתור צף פעיל · Claude")
            .setContentText("לחץ על הבועה לגישה מהירה מכל אפליקציה")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pending)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                FOREGROUND_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(FOREGROUND_ID, notification)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayViews(initialX: Int, initialY: Int) {
        if (rootView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val wmParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            val (clampedX, clampedY) = clampPosition(initialX, initialY, expanded = false)
            x = clampedX
            y = clampedY
        }
        params = wmParams

        val root = FrameLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // 1. Collapsed Studio Bubble
        val bubbleSize = dp(58)
        val bubble = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#101217"))
                setStroke(dp(2), Color.parseColor("#C8A86B"))
            }
            elevation = dp(8).toFloat()
        }

        val logoImage = ImageView(this).apply {
            setImageResource(R.drawable.ic_stat_claude)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val pad = dp(13)
            setPadding(pad, pad, pad, pad)
            setColorFilter(Color.parseColor("#D47A5A"))
        }
        bubble.addView(logoImage, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        val dotSize = dp(12)
        val dot = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#64A685"))
                setStroke(dp(2), Color.parseColor("#08090C"))
            }
        }
        statusDotView = dot
        bubble.addView(
            dot,
            FrameLayout.LayoutParams(dotSize, dotSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        )

        // 2. Expanded Quick Panel
        val panelWidth = dp(304)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            visibility = View.GONE
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#101217"))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.parseColor("#C8A86B"))
            }
            elevation = dp(12).toFloat()
        }

        // Panel Header
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val headerTitle = TextView(this).apply {
            text = "Claude · חלונית מהירה"
            setTextColor(Color.parseColor("#E3C993"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val collapseBtn = TextView(this).apply {
            text = "מזער"
            setTextColor(Color.parseColor("#9EA5B3"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { togglePanel(false) }
        }
        headerRow.addView(headerTitle)
        headerRow.addView(collapseBtn)
        panel.addView(headerRow)

        val statusTv = TextView(this).apply {
            text = "בודק מצב חיבור…"
            setTextColor(Color.parseColor("#9EA5B3"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(4), 0, dp(8))
        }
        statusTextView = statusTv
        panel.addView(statusTv)

        // Target Conversation Toggle: New Chat vs Continue Latest
        val targetRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val newBtn = TextView(this).apply {
            text = "שיחה חדשה"
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        val contBtn = TextView(this).apply {
            text = "המשך שיחה אחרונה"
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        newChatBtn = newBtn
        continueChatBtn = contBtn

        fun refreshTargetButtons() {
            val activeBg = GradientDrawable().apply {
                setColor(Color.parseColor("#2A2316"))
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), Color.parseColor("#C8A86B"))
            }
            val idleBg = GradientDrawable().apply {
                setColor(Color.parseColor("#161920"))
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), Color.parseColor("#252934"))
            }
            newBtn.background = if (!continueLatestConversation) activeBg else idleBg
            newBtn.setTextColor(Color.parseColor(if (!continueLatestConversation) "#E3C993" else "#9EA5B3"))
            contBtn.background = if (continueLatestConversation) activeBg else idleBg
            contBtn.setTextColor(Color.parseColor(if (continueLatestConversation) "#E3C993" else "#9EA5B3"))
        }
        newBtn.setOnClickListener {
            continueLatestConversation = false
            refreshTargetButtons()
        }
        contBtn.setOnClickListener {
            continueLatestConversation = true
            refreshTargetButtons()
        }
        refreshTargetButtons()
        targetRow.addView(newBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(4)
        })
        targetRow.addView(contBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(4)
        })
        panel.addView(targetRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })

        // Quick Prompt Template Chips
        val templatesRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val templates = listOf("סכם בקצרה", "תרגם לעברית", "נסח מקצועית")
        templates.forEachIndexed { idx, label ->
            val chip = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#E3C993"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                setPadding(dp(6), dp(5), dp(6), dp(5))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1A1D26"))
                    cornerRadius = dp(8).toFloat()
                    setStroke(dp(1), Color.parseColor("#55C8A86B"))
                }
                setOnClickListener {
                    val current = quickInput?.text?.toString()?.trim().orEmpty()
                    val updated = if (current.isEmpty()) "$label: " else "$label:\n$current"
                    quickInput?.setText(updated)
                    quickInput?.setSelection(updated.length)
                }
            }
            templatesRow.addView(chip, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (idx < templates.lastIndex) marginEnd = dp(4)
            })
        }
        panel.addView(templatesRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })

        val input = EditText(this).apply {
            hint = "כתוב הודעה בעברית או באנגלית…"
            setHintTextColor(Color.parseColor("#9EA5B3"))
            setTextColor(Color.parseColor("#F4F2EE"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            minLines = 2
            maxLines = 4
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#161920"))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), Color.parseColor("#252934"))
            }
        }
        quickInput = input
        panel.addView(input, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // Primary Action Button: Prepare / Send
        val sendBtn = Button(this).apply {
            text = "הכן ופתח ב-Claude"
            setTextColor(Color.parseColor("#08090C"))
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#C8A86B"))
                cornerRadius = dp(10).toFloat()
            }
            setOnClickListener {
                val prompt = quickInput?.text?.toString()?.trim().orEmpty()
                if (prompt.isEmpty()) {
                    Toast.makeText(this@FloatingBubbleService, "כתוב הודעה תחילה", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                handleQuickSend(prompt)
            }
        }
        panel.addView(sendBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(46)
        ).apply { topMargin = dp(10) })

        // Secondary Row: Open Full App & Hide Bubble
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val openAppBtn = Button(this).apply {
            text = "פתח אפליקציה"
            setTextColor(Color.parseColor("#E3C993"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#161920"))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), Color.parseColor("#55C8A86B"))
            }
            setOnClickListener {
                togglePanel(false)
                val openIntent = Intent(this@FloatingBubbleService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(openIntent)
            }
        }

        val hideBtn = Button(this).apply {
            text = "הסתר בועה"
            setTextColor(Color.parseColor("#D66A72"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#281518"))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), Color.parseColor("#66D66A72"))
            }
            setOnClickListener { hideAndStop() }
        }

        bottomRow.addView(openAppBtn, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
            marginEnd = dp(6)
        })
        bottomRow.addView(hideBtn, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
            marginStart = dp(6)
        })
        panel.addView(bottomRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        root.addView(bubble)
        root.addView(panel, FrameLayout.LayoutParams(panelWidth, FrameLayout.LayoutParams.WRAP_CONTENT))

        // Drag & Tap Listener on Bubble
        var startX = 0
        var startY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var isDragging = false

        bubble.setOnTouchListener { _, event ->
            val lp = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x
                    startY = lp.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    isDragging = false
                    bubble.animate().scaleX(0.92f).scaleY(0.92f).setDuration(120).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        lp.x = startX + dx
                        lp.y = startY + dy
                        windowManager.updateViewLayout(root, lp)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    bubble.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
                    if (!isDragging) {
                        togglePanel(true)
                    } else {
                        snapAndSavePosition(lp, root)
                    }
                    true
                }
                else -> false
            }
        }

        bubbleContainer = bubble
        panelContainer = panel
        rootView = root

        windowManager.addView(root, wmParams)
        root.alpha = 0f
        root.scaleX = 0.8f
        root.scaleY = 0.8f
        root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(240).start()
    }

    private fun snapAndSavePosition(lp: WindowManager.LayoutParams, root: View) {
        val displayWidth = resources.displayMetrics.widthPixels
        val targetX = if (lp.x + dp(30) < displayWidth / 2) dp(12) else (displayWidth - dp(72)).coerceAtLeast(dp(12))
        val (_, clampedY) = clampPosition(targetX, lp.y, expanded = false)
        lp.y = clampedY
        val animator = ValueAnimator.ofInt(lp.x, targetX).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                lp.x = anim.animatedValue as Int
                if (root.isAttachedToWindow) {
                    windowManager.updateViewLayout(root, lp)
                }
            }
        }
        animator.start()

        val app = application as ClaudeClassicApplication
        serviceScope.launch(Dispatchers.IO) {
            app.container.settingsDataStore.saveBubblePosition(targetX, clampedY)
        }
    }

    private fun togglePanel(expand: Boolean) {
        val root = rootView ?: return
        val lp = params ?: return
        val bubble = bubbleContainer ?: return
        val panel = panelContainer ?: return

        isExpanded = expand
        if (expand) {
            bubble.visibility = View.GONE
            panel.visibility = View.VISIBLE
            panel.alpha = 0f
            panel.scaleX = 0.9f
            panel.scaleY = 0.9f
            panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
            val (clampedX, clampedY) = clampPosition(lp.x, lp.y, expanded = true)
            lp.x = clampedX
            lp.y = clampedY
            lp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            windowManager.updateViewLayout(root, lp)
        } else {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            quickInput?.windowToken?.let { imm.hideSoftInputFromWindow(it, 0) }
            panel.visibility = View.GONE
            bubble.visibility = View.VISIBLE
            val (clampedX, clampedY) = clampPosition(lp.x, lp.y, expanded = false)
            lp.x = clampedX
            lp.y = clampedY
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            windowManager.updateViewLayout(root, lp)
        }
    }

    private fun clampPosition(rawX: Int, rawY: Int, expanded: Boolean): Pair<Int, Int> {
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val overlayWidth = if (expanded) dp(304) else dp(58)
        val overlayHeight = if (expanded) dp(330) else dp(58)
        val margin = dp(12)
        val maxX = (width - overlayWidth - margin).coerceAtLeast(margin)
        val maxY = (height - overlayHeight - dp(24)).coerceAtLeast(dp(36))
        return rawX.coerceIn(margin, maxX) to rawY.coerceIn(dp(36), maxY)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val root = rootView ?: return
        val lp = params ?: return
        if (root.isAttachedToWindow) {
            val (clampedX, clampedY) = clampPosition(lp.x, lp.y, expanded = isExpanded)
            lp.x = clampedX
            lp.y = clampedY
            windowManager.updateViewLayout(root, lp)
        }
    }

    private fun handleQuickSend(prompt: String) {
        val app = application as ClaudeClassicApplication
        serviceScope.launch {
            val settings = app.container.settingsDataStore.getCurrentSettings()
            val hasKey = app.container.apiKeyStore.hasApiKey()
            val isOnline = app.container.networkMonitor.checkCurrentConnectivity()
            val targetConvoId = if (continueLatestConversation) {
                app.container.repository.getLatestConversationId()
            } else {
                null
            }

            if (settings.isPaidApiReady) {
                if (!hasKey) {
                    Toast.makeText(
                        this@FloatingBubbleService,
                        "לא הוגדר מפתח API — פותח במצב חינמי",
                        Toast.LENGTH_SHORT
                    ).show()
                    app.container.repository.prepareMessageForFreeClaude(
                        conversationId = targetConvoId,
                        promptText = prompt,
                        isScheduled = false
                    )
                    quickInput?.setText("")
                    togglePanel(false)
                    val outcome = ClaudeLauncher.openClaudeWebWithPrompt(this@FloatingBubbleService, prompt)
                    Toast.makeText(this@FloatingBubbleService, outcome.message, Toast.LENGTH_LONG).show()
                    return@launch
                }
                if (!isOnline) {
                    Toast.makeText(
                        this@FloatingBubbleService,
                        "אין חיבור לאינטרנט — בדוק את הרשת ונסה שוב",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                quickInput?.setText("")
                togglePanel(false)
                Toast.makeText(this@FloatingBubbleService, "שולח ל-Claude דרך API…", Toast.LENGTH_SHORT).show()
                app.container.repository.sendMessage(
                    conversationId = targetConvoId,
                    promptText = prompt,
                    isScheduled = false,
                    notifyEvenIfForeground = true
                )
            } else {
                app.container.repository.prepareMessageForFreeClaude(
                    conversationId = targetConvoId,
                    promptText = prompt,
                    isScheduled = false
                )
                quickInput?.setText("")
                togglePanel(false)
                val outcome = ClaudeLauncher.openClaudeWebWithPrompt(this@FloatingBubbleService, prompt)
                Toast.makeText(this@FloatingBubbleService, outcome.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun observeStatusUpdates(app: ClaudeClassicApplication) {
        serviceScope.launch {
            app.container.networkMonitor.isOnline.collect { online ->
                updateStatusDisplay(app, online)
            }
        }
        serviceScope.launch {
            app.container.settingsDataStore.settingsFlow.collect {
                updateStatusDisplay(app, app.container.networkMonitor.checkCurrentConnectivity())
            }
        }
    }

    private suspend fun updateStatusDisplay(app: ClaudeClassicApplication, online: Boolean) {
        val settings = app.container.settingsDataStore.getCurrentSettings()
        val hasKey = app.container.apiKeyStore.hasApiKey()

        val statusLabel = when {
            !online -> "אין חיבור לאינטרנט"
            settings.isPaidApiReady && hasKey -> "מחובר · API פעיל (${settings.selectedModel})"
            else -> "מחובר · מצב חשבון Claude חינמי"
        }
        val dotColor = when {
            !online -> Color.parseColor("#D66A72")
            settings.isPaidApiReady && hasKey -> Color.parseColor("#64A685")
            else -> Color.parseColor("#D47A5A")
        }

        statusTextView?.text = statusLabel
        (statusDotView?.background as? GradientDrawable)?.setColor(dotColor)
    }

    private fun hideAndStop() {
        val app = application as ClaudeClassicApplication
        serviceScope.launch(Dispatchers.IO) {
            app.container.settingsDataStore.setFloatingBubbleEnabled(false)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        rootView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
        }
        rootView = null
        serviceScope.cancel()
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    companion object {
        const val ACTION_HIDE = "com.example.overlay.ACTION_HIDE"
        private const val FOREGROUND_ID = 7701

        fun start(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingBubbleService::class.java))
        }
    }
}
