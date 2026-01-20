package com.example.car_park

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.car_park.databinding.ActivityRoleSelectionBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding
    private var isAnimating = false
    private val scope = CoroutineScope(Dispatchers.Main)
    private var lastRoleSelected: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.white)

        // Check if user is already logged in
        checkExistingSession()

        // Setup animations
        setupInitialAnimations()

        // Setup click listeners with animations
        setupRoleCards()

        // Setup button listeners
        setupButtonListeners()
    }

    override fun onResume() {
        super.onResume()
        // Reset animations when returning to this screen
        resetCardAnimations()
    }

    private fun checkExistingSession() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        val role = sharedPref.getString("user_role", "") ?: ""

        if (isLoggedIn && role.isNotEmpty()) {
            showWelcomeBackMessage(role)
            scope.launch {
                delay(1500) // Show welcome message for 1.5 seconds
                redirectToDashboard(role)
            }
        }
    }

    private fun showWelcomeBackMessage(role: String) {
        val roleName = when (role) {
            "admin" -> "Administrator"
            "driver" -> "Driver"
            else -> "User"
        }

        Snackbar.make(binding.root, "Welcome back, $roleName!", Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.green))
            .setTextColor(Color.WHITE)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }

    private fun setupInitialAnimations() {
        // Initially hide cards
        binding.cardDriver.alpha = 0f
        binding.cardDriver.translationY = 100f
        binding.cardAdmin.alpha = 0f
        binding.cardAdmin.translationY = 100f

        // Hide logo initially
        // binding.logoCard?.apply {
        //     scaleX = 0f
        //     scaleY = 0f
        //     alpha = 0f
        // }

        // Start entrance animations
        startEntranceAnimations()
    }

    private fun startEntranceAnimations() {
        // Animate logo
        // binding.logoCard?.apply {
        //     animate()
        //         .scaleX(1f)
        //         .scaleY(1f)
        //         .alpha(1f)
        //         .setDuration(800)
        //         .setInterpolator(OvershootInterpolator(1.2f))
        //         .setStartDelay(300)
        //         .start()
        // }

        // Animate title and subtitle
        binding.title.alpha = 0f
        binding.title.translationY = -50f
        binding.subtitle.alpha = 0f
        binding.subtitle.translationY = -30f

        binding.title.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(500)
            .start()

        binding.subtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(700)
            .start()

        // Animate role cards sequentially
        scope.launch {
            delay(900)
            animateCardEntrance(binding.cardDriver, 0)

            delay(200)
            animateCardEntrance(binding.cardAdmin, 1)
        }
    }

    private fun animateCardEntrance(card: View, delayMultiplier: Int) {
        card.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay((100 * delayMultiplier).toLong())
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun resetCardAnimations() {
        // Reset to initial state for re-animation
        binding.cardDriver.alpha = 1f
        binding.cardAdmin.alpha = 1f
        binding.cardDriver.scaleX = 1f
        binding.cardDriver.scaleY = 1f
        binding.cardAdmin.scaleX = 1f
        binding.cardAdmin.scaleY = 1f
    }

    private fun setupRoleCards() {
        binding.cardDriver.setOnClickListener {
            if (!isAnimating) {
                selectRole("driver", binding.cardDriver)
            }
        }

        binding.cardAdmin.setOnClickListener {
            if (!isAnimating) {
                selectRole("admin", binding.cardAdmin)
            }
        }

        // Add hover animations
        setupCardHoverEffects()
    }

    private fun setupCardHoverEffects() {
        listOf(binding.cardDriver, binding.cardAdmin).forEach { card ->
            card.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.animate()
                            .scaleX(0.98f)
                            .scaleY(0.98f)
                            .setDuration(100)
                            .start()
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                }
                false
            }
        }
    }

    private fun setupButtonListeners() {
        // Driver select button
        binding.cardDriver.setOnClickListener {
            if (!isAnimating) {
                selectRole("driver", binding.cardDriver)
            }
        }

        // Admin select button
        binding.cardAdmin.setOnClickListener {
            if (!isAnimating) {
                selectRole("admin", binding.cardAdmin)
            }
        }
    }

    private fun selectRole(role: String, selectedCard: View) {
        if (isAnimating) return

        isAnimating = true
        lastRoleSelected = role

        // Animate selection
        animateRoleSelection(selectedCard, role)

        // Animate deselection of other card
        val otherCard = if (role == "driver") binding.cardAdmin else binding.cardDriver
        animateCardDeselection(otherCard)

        // Navigate after animation
        scope.launch {
            delay(800) // Wait for animation to complete
            navigateToLogin(role)
        }
    }

    private fun animateRoleSelection(card: View, role: String) {
        // Scale up animation
        card.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator())
            .start()

        // Change card background color based on role
        val highlightColor = when (role) {
            "driver" -> ContextCompat.getColor(this, R.color.light_green)
            "admin" -> ContextCompat.getColor(this, R.color.light_orange)
            else -> Color.WHITE
        }

        // Animate background color change
        val originalColor = ContextCompat.getColor(this, R.color.white)
        animateBackgroundColor(card, originalColor, highlightColor)

        // Animate check icon
        // Check icon animation handled by card selection visual feedback
        card.apply {
            scaleX = 0.95f
            scaleY = 0.95f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(200)
                .setInterpolator(OvershootInterpolator())
                .start()
        }

        // Show selection confirmation
        showSelectionConfirmation(role)
    }

    private fun animateBackgroundColor(view: View, fromColor: Int, toColor: Int) {
        val animator = ValueAnimator.ofArgb(fromColor, toColor)
        animator.duration = 500
        animator.addUpdateListener { valueAnimator ->
            view.setBackgroundColor(valueAnimator.animatedValue as Int)
        }
        animator.start()
    }

    private fun animateCardDeselection(card: View) {
        card.animate()
            .alpha(0.6f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(300)
            .start()
    }

    private fun showSelectionConfirmation(role: String) {
        val roleName = when (role) {
            "driver" -> "Driver"
            "admin" -> "Administrator"
            else -> "User"
        }

        // Show animated text
        val confirmationText = "$roleName selected"
        val textView = android.widget.TextView(this).apply {
            text = confirmationText
            setTextColor(ContextCompat.getColor(this@RoleSelectionActivity, R.color.green))
            textSize = 14f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            alpha = 0f
            translationY = 20f
        }

        val layoutParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.CENTER
        }

        (binding.root as? android.view.ViewGroup)?.addView(textView, layoutParams)

        textView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .withEndAction {
                textView.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(300)
                    .setStartDelay(500)
                    .withEndAction {
                        (binding.root as? android.view.ViewGroup)?.removeView(textView)
                    }
                    .start()
            }
            .start()
    }

    private fun navigateToLogin(role: String) {
        isAnimating = false

        // Create intent with transition data
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra("role", role)
            putExtra("transition", "role_selection")
        }

        // Custom activity transition
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down)

        // Reset card states after delay
        scope.launch {
            delay(1000)
            resetCardStates()
        }
    }

    private fun resetCardStates() {
        binding.cardDriver.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(300)
            .start()

        binding.cardAdmin.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(300)
            .start()

        // Reset check icons
        listOf(binding.cardDriver, binding.cardAdmin).forEach { card ->
            card.scaleX = 1f
            card.scaleY = 1f
        }
    }

    private fun redirectToDashboard(role: String) {
        val intent = when (role) {
            "admin" -> Intent(this, AdminDashboardActivity::class.java)
            "driver" -> Intent(this, DriverDashboardActivity::class.java)
            else -> return
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() {
        // Enhanced back press with animation
        if (isAnimating) return

        if (lastRoleSelected != null) {
            // If a role was selected recently, reset it
            resetCardStates()
            lastRoleSelected = null
        } else {
            // Otherwise, exit with confirmation
            showExitConfirmation()
        }
    }

    private fun showExitConfirmation() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Exit") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.ic_exit)
            .show()
    }
}