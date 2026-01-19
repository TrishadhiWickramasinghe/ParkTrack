package com.example.car_park

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Add animation to the login card
        val loginCard = findViewById<MaterialCardView>(R.id.login_card)
        loginCard.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(300)
            .start()

        // Add click animations
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        btnLogin.setOnClickListener {
            // Add scale animation
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()

                    // Your login logic here
                    performLogin()
                }
                .start()
        }

        // Add focus listeners for text fields
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)

        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                animateViewScale(etEmail.parent as View, 1.02f)
            } else {
                animateViewScale(etEmail.parent as View, 1f)
            }
        }

        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                animateViewScale(etPassword.parent as View, 1.02f)
            } else {
                animateViewScale(etPassword.parent as View, 1f)
            }
        }

        // Add ripple effect programmatically
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        tvRegister.setBackgroundResource(R.drawable.green_ripple)

        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        tvForgotPassword.setBackgroundResource(R.drawable.green_ripple)
    }

    private fun animateViewScale(view: View, scale: Float) {
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(200)
            .start()
    }

    private fun performLogin() {
        // Your login implementation
    }
}