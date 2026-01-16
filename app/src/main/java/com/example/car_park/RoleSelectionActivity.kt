// RoleSelectionActivity.kt
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_role_selection.*

class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        // Check if user is already logged in
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        val role = sharedPref.getString("user_role", "")

        if (isLoggedIn && role.isNotEmpty()) {
            redirectToDashboard(role)
            return
        }

        // Set up click listeners
        cardDriver.setOnClickListener {
            navigateToLogin("driver")
        }

        cardAdmin.setOnClickListener {
            navigateToLogin("admin")
        }
    }

    private fun navigateToLogin(role: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("role", role)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun redirectToDashboard(role: String) {
        when (role) {
            "admin" -> {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            }
            "driver" -> {
                startActivity(Intent(this, DriverDashboardActivity::class.java))
            }
        }
        finish()
    }
}