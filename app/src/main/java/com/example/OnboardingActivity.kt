package com.example

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.data.PrefManager

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefManager = PrefManager(this)
        prefManager.applySavedTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)

        val isDark = prefManager.isDarkMode()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        val root = findViewById<View>(R.id.onboardingRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val fromDashboard = intent.getBooleanExtra(EXTRA_FROM_DASHBOARD, false)
        val btnStart = findViewById<View>(R.id.btnStartOnboarding)
        val onboardingScroll = findViewById<View>(R.id.onboardingScroll)
        val containerBrandBadge = findViewById<View>(R.id.containerBrandBadge)
        val circleOrange = findViewById<View>(R.id.circle_orange_onboarding)
        val containerLogoCircle = findViewById<View>(R.id.container_logo_circle_onboarding)

        btnStart.setOnClickListener {
            btnStart.isClickable = false

            prefManager.setOnboardingDone(true)

            if (fromDashboard) {
                finish()
                return@setOnClickListener
            }

            // Animate out text content, brand badge, and start button
            onboardingScroll.animate()
                .alpha(0f)
                .translationY(30f)
                .setDuration(220)
                .start()

            containerBrandBadge.animate()
                .alpha(0f)
                .setDuration(180)
                .start()

            btnStart.animate()
                .alpha(0f)
                .translationY(40f)
                .setDuration(220)
                .start()

            // Measure actual runtime screen coordinates
            val dpToPx = resources.displayMetrics.density

            val orangeLoc = IntArray(2)
            val logoLoc = IntArray(2)
            circleOrange.getLocationOnScreen(orangeLoc)
            containerLogoCircle.getLocationOnScreen(logoLoc)

            // Center of circle_orange_onboarding on screen:
            val orangeCenterX = orangeLoc[0] + (circleOrange.width / 2f)
            val orangeCenterY = orangeLoc[1] + (circleOrange.height / 2f)

            // In LoginActivity, container_logo_circle (180dp x 180dp) is centered in circle_orange with translationX=10dp, translationY=-10dp:
            val targetLogoSizePx = 180f * dpToPx
            val targetScreenX = orangeCenterX - (targetLogoSizePx / 2f) + (10f * dpToPx)
            val targetScreenY = orangeCenterY - (targetLogoSizePx / 2f) - (10f * dpToPx)

            val deltaX = targetScreenX - logoLoc[0]
            val deltaY = targetScreenY - logoLoc[1]
            val targetScale = targetLogoSizePx / containerLogoCircle.width.toFloat()

            val animDuration = 550L

            containerLogoCircle.animate()
                .translationX(deltaX)
                .translationY(deltaY)
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(animDuration)
                .setInterpolator(DecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        val target = if (prefManager.isLoggedIn()) {
                            MainActivity::class.java
                        } else {
                            LoginActivity::class.java
                        }
                        startActivity(Intent(this@OnboardingActivity, target))
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                        finish()
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    }
                })
                .start()
        }
    }

    companion object {
        const val EXTRA_FROM_DASHBOARD = "extra_from_dashboard"
    }
}
