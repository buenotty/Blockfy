package com.buenotty.blockfy.feature_accessibility.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.buenotty.blockfy.R

/**
 * The funny interruption shown when a block fires.
 *
 * It is a window owned by the Accessibility service (TYPE_ACCESSIBILITY_OVERLAY), so it needs no
 * "draw over other apps" permission. It never reads the screen: it only draws its own views.
 * Both modes are one-tap to leave and close themselves, so nobody is ever trapped.
 */
class ChallengeOverlay(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private var root: FrameLayout? = null
    private val autoClose = Runnable { dismiss() }

    val isShowing: Boolean get() = root != null

    /** Set before each show so the window matches the app (light or dark). */
    var dark: Boolean = false

    private val scrim get() = Color.parseColor(if (dark) "#F20B0E1B" else "#E60B0E1B")
    private val card get() = Color.parseColor(if (dark) "#1C1F33" else "#F7F7FC")
    private val ink get() = Color.parseColor(if (dark) "#F1F2FA" else "#0B0E1B")
    private val mutedColor get() = Color.parseColor(if (dark) "#A3A8C3" else "#5B6078")
    private val violet get() = Color.parseColor(if (dark) "#8F9BFF" else "#4B5BFF")
    private val suggestionBg get() = Color.parseColor(if (dark) "#2A2E4A" else "#E8EAFF")
    private val choiceBg get() = Color.parseColor(if (dark) "#2A2E4A" else "#E8EAFF")
    private val onViolet get() = Color.parseColor(if (dark) "#0B0E1B" else "#FFFFFF")

    /** Trick question for short-video blocks. Returns false when the window could not be added. */
    fun showChallenge(challenge: Challenge, contextLine: String?, suggestion: String): Boolean {
        dismiss()
        val card = newCard()
        card.addView(tag(R.string.overlay_tag_challenge))
        contextLine?.takeIf { it.isNotBlank() }?.let { card.addView(body(it, muted = true, topDp = 6)) }
        card.addView(title(service.getString(challenge.question), topDp = 10))

        val showResult = {
            card.removeAllViews()
            card.addView(tag(R.string.overlay_tag_result))
            card.addView(title(service.getString(challenge.resultTitle), topDp = 8, accent = true))
            card.addView(body(service.getString(challenge.resultBody), topDp = 10))
            card.addView(suggestionBlock(suggestion))
            card.addView(primaryButton(R.string.overlay_btn_ok) { dismiss() })
            scheduleAutoClose()
        }
        val options = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL }
        if (challenge.a.art != null && challenge.b.art != null) {
            val row = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(artCell(challenge.a, showResult), cellParams(rightDp = 6))
            row.addView(artCell(challenge.b, showResult), cellParams(leftDp = 6))
            options.addView(row, matchWidth(topDp = 16))
        } else {
            options.addView(choiceButton(challenge.a.label, showResult), matchWidth(topDp = 16))
            options.addView(choiceButton(challenge.b.label, showResult), matchWidth(topDp = 10))
        }
        card.addView(options)
        return attach(card)
    }

    /** Calmer, no-quiz variant for the adult shield: a short pause and somewhere useful to go. */
    fun showCheckIn(
        titleText: String,
        bodyText: String,
        suggestion: String,
        helpLine: String?,
        onOpenNotifications: () -> Unit
    ): Boolean {
        dismiss()
        val card = newCard()
        card.addView(tag(R.string.overlay_tag_checkin))
        card.addView(title(titleText, topDp = 8))
        card.addView(body(bodyText, topDp = 10))
        card.addView(suggestionBlock(suggestion))
        card.addView(incognitoBlock(onOpenNotifications))
        card.addView(primaryButton(R.string.overlay_btn_ok) { dismiss() })
        helpLine?.takeIf { it.isNotBlank() }?.let { card.addView(body(it, muted = true, topDp = 14, sizeSp = 13f)) }
        return attach(card)
    }

    fun dismiss() {
        main.removeCallbacks(autoClose)
        val view = root ?: return
        root = null
        try {
            windowManager.removeViewImmediate(view)
        } catch (e: Exception) {
            Log.w(TAG, "Overlay already gone", e)
        }
    }

    private fun attach(card: LinearLayout): Boolean {
        val frame = FrameLayout(service).apply {
            setBackgroundColor(scrim)
            isClickable = true
        }
        frame.addView(
            card,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                .apply { setMargins(dp(20), dp(24), dp(20), dp(24)) }
        )
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        return try {
            windowManager.addView(frame, params)
            root = frame
            scheduleAutoClose()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not show overlay", e)
            false
        }
    }

    private fun scheduleAutoClose() {
        main.removeCallbacks(autoClose)
        main.postDelayed(autoClose, AUTO_CLOSE_MILLIS)
    }

    private fun newCard() = LinearLayout(service).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(22), dp(22), dp(20))
        background = rounded(card, 28)
    }

    private fun tag(textRes: Int) = TextView(service).apply {
        text = service.getString(textRes).uppercase()
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(violet)
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.12f
    }

    private fun title(text: String, topDp: Int, accent: Boolean = false) = TextView(service).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        setTextColor(if (accent) violet else ink)
        typeface = Typeface.DEFAULT_BOLD
        setLineSpacing(0f, 1.05f)
        layoutParams = matchWidth(topDp)
    }

    private fun body(text: String, muted: Boolean = false, topDp: Int = 0, sizeSp: Float = 16f) = TextView(service).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(if (muted) mutedColor else ink)
        setLineSpacing(0f, 1.15f)
        layoutParams = matchWidth(topDp)
    }

    private fun suggestionBlock(suggestion: String) = LinearLayout(service).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(suggestionBg, 18)
        layoutParams = matchWidth(18)
        addView(TextView(service).apply {
            text = service.getString(R.string.overlay_suggestion_label)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(violet)
            typeface = Typeface.DEFAULT_BOLD
        })
        addView(TextView(service).apply {
            text = suggestion
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(4), 0, 0)
        })
    }

    /** The browser keeps an "incognito tabs open" notification: point at it so the loop really ends. */
    private fun incognitoBlock(onOpenNotifications: () -> Unit) = LinearLayout(service).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(choiceBg, 18)
        layoutParams = matchWidth(12)
        addView(TextView(service).apply {
            text = service.getString(R.string.overlay_incognito_title)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
        })
        addView(TextView(service).apply {
            text = service.getString(R.string.overlay_incognito_body)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(mutedColor)
            setPadding(0, dp(4), 0, dp(10))
        })
        addView(TextView(service).apply {
            text = service.getString(R.string.overlay_btn_notifications)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(violet)
            typeface = Typeface.DEFAULT_BOLD
            isClickable = true
            setOnClickListener {
                dismiss()
                onOpenNotifications()
            }
        })
    }

    private fun primaryButton(textRes: Int, onClick: () -> Unit) = TextView(service).apply {
        text = service.getString(textRes)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTextColor(onViolet)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(violet, 18)
        layoutParams = matchWidth(18)
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun choiceButton(labelRes: Int, onClick: () -> Unit) = TextView(service).apply {
        text = service.getString(labelRes)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        setTextColor(ink)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(choiceBg, 18)
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun artCell(option: ChallengeOption, onClick: () -> Unit) = LinearLayout(service).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        isClickable = true
        contentDescription = service.getString(option.label)
        setOnClickListener { onClick() }
        addView(
            ImageView(service).apply {
                setImageResource(option.art!!)
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(138))
        )
        addView(TextView(service).apply {
            text = service.getString(option.label)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(2))
        })
    }

    private fun matchWidth(topDp: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(topDp) }

    private fun cellParams(leftDp: Int = 0, rightDp: Int = 0) =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            leftMargin = dp(leftDp)
            rightMargin = dp(rightDp)
        }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), service.resources.displayMetrics).toInt()

    private companion object {
        const val TAG = "ChallengeOverlay"
        const val AUTO_CLOSE_MILLIS = 30_000L
    }
}
