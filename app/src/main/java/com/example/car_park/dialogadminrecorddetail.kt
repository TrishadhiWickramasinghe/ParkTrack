import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class dialogadminrecorddetail : DialogFragment() {

    private lateinit var driverName: String
    private lateinit var phone: String
    private lateinit var details: String
    private var onContactClickListener: (() -> Unit)? = null

    companion object {
        fun newInstance(
            driverName: String,
            phone: String,
            details: String
        ): AttractiveRecordDialog {
            return AttractiveRecordDialog().apply {
                arguments = Bundle().apply {
                    putString("driverName", driverName)
                    putString("phone", phone)
                    putString("details", details)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            driverName = it.getString("driverName", "")
            phone = it.getString("phone", "")
            details = it.getString("details", "")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Set transparent background
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Add enter animation
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.attractive_dialog, null)
        dialog.setContentView(view)

        setupViews(view)
        setupAnimations(view)

        return dialog
    }

    private fun setupViews(view: View) {
        // Set text values
        view.findViewById<TextView>(R.id.tvDriverName).text = driverName
        view.findViewById<TextView>(R.id.tvPhone).text = phone
        view.findViewById<TextView>(R.id.tvDetailContent).text = details

        // Set click listeners
        view.findViewById<MaterialButton>(R.id.btnClose).setOnClickListener {
            dismissWithAnimation()
        }

        view.findViewById<MaterialButton>(R.id.btnContact).setOnClickListener {
            // Add button animation
            animateButtonClick(it)

            // Trigger contact callback
            onContactClickListener?.invoke()

            // Dismiss after contact action
            dismissWithAnimation()
        }

        // Add ripple effect
        view.findViewById<MaterialCardView>(R.id.detailsCard).setOnClickListener {
            animateCardClick(it)
        }
    }

    private fun setupAnimations(view: View) {
        // Initial hidden state
        view.alpha = 0f
        view.scaleX = 0.9f
        view.scaleY = 0.9f

        // Entrance animation
        view.post {
            // Background fade in
            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            // Card animations with delay
            val carCard = view.findViewById<MaterialCardView>(R.id.carIconCard)
            val detailsCard = view.findViewById<MaterialCardView>(R.id.detailsCard)

            carCard.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(200)
                .start()

            detailsCard.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(300)
                .start()

            // Button animations
            val btnClose = view.findViewById<MaterialButton>(R.id.btnClose)
            val btnContact = view.findViewById<MaterialButton>(R.id.btnContact)

            btnClose.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(400)
                .start()

            btnContact.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(450)
                .start()
        }
    }

    private fun dismissWithAnimation() {
        view?.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dismiss()
                }
            })
            .start()
    }

    private fun animateButtonClick(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun animateCardClick(view: View) {
        ObjectAnimator.ofFloat(view, "translationZ", 16f, 0f)
            .setDuration(300)
            .start()
    }

    fun setOnContactClickListener(listener: () -> Unit) {
        this.onContactClickListener = listener
    }

    // Usage in Activity/Fragment:
    fun showDialogExample(context: Context) {
        val dialog = AttractiveRecordDialog.newInstance(
            driverName = "John Smith",
            phone = "+1 234 567 8900",
            details = "Vehicle: Toyota Camry\nPlate: ABC-123\nParking Spot: A-12\nDuration: 2 hours"
        )

        dialog.setOnContactClickListener {
            // Handle contact action
            // Example: Make phone call or open contact
        }

        dialog.show((context as FragmentActivity).supportFragmentManager, "record_dialog")
    }
}