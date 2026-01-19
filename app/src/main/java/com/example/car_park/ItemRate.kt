import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

class RateAdapter(private val rates: List<ParkingRate>) :
    RecyclerView.Adapter<RateAdapter.RateViewHolder>() {

    data class ParkingRate(
        val id: String,
        val name: String,
        val value: String,
        val description: String,
        val timeRange: String = "08:00 - 20:00",
        val vehicleTypes: String = "All Vehicles",
        val applicableDays: String = "Mon - Sun",
        val isActive: Boolean = true,
        val isExpanded: Boolean = false
    )

    inner class RateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Main views (original IDs)
        private val cardRate: MaterialCardView = itemView.findViewById(R.id.cardRate)
        private val tvRateName: TextView = itemView.findViewById(R.id.tvRateName)
        private val tvRateValue: TextView = itemView.findViewById(R.id.tvRateValue)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvActive: TextView = itemView.findViewById(R.id.tvActive)
        private val switchActive: SwitchMaterial = itemView.findViewById(R.id.switchActive)

        // New views for enhanced UI
        private val statusBadge: MaterialCardView = itemView.findViewById(R.id.statusBadge)
        private val tvTimeRange: TextView = itemView.findViewById(R.id.tvTimeRange)
        private val layoutAdditionalInfo: View = itemView.findViewById(R.id.layoutAdditionalInfo)
        private val tvVehicleTypes: TextView = itemView.findViewById(R.id.tvVehicleTypes)
        private val tvApplicableDays: TextView = itemView.findViewById(R.id.tvApplicableDays)
        private val btnExpand: MaterialCardView = itemView.findViewById(R.id.btnExpand)
        private val expandIcon = btnExpand.getChildAt(0) as android.widget.ImageView

        init {
            // Card click listener
            cardRate.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(position)
                }
            }

            // Expand/collapse listener
            btnExpand.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    toggleExpand(position)
                }
            }

            // Switch listener
            switchActive.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSwitchToggle?.invoke(position, isChecked)

                    // Animate status badge
                    if (isChecked) {
                        animateStatusBadge(true)
                    } else {
                        animateStatusBadge(false)
                    }
                }
            }

            // Add press animation
            cardRate.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        animateCardPress(true)
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        animateCardPress(false)
                    }
                }
                false
            }
        }

        fun bind(rate: ParkingRate) {
            // Set data
            tvRateName.text = rate.name
            tvRateValue.text = rate.value
            tvDescription.text = rate.description
            tvTimeRange.text = rate.timeRange
            tvVehicleTypes.text = rate.vehicleTypes
            tvApplicableDays.text = rate.applicableDays
            switchActive.isChecked = rate.isActive

            // Update active text color
            if (rate.isActive) {
                tvActive.setTextColor(ContextCompat.getColor(itemView.context, R.color.green_500))
            } else {
                tvActive.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray_600))
            }

            // Show/hide status badge
            if (rate.isActive) {
                statusBadge.visibility = View.VISIBLE
            } else {
                statusBadge.visibility = View.GONE
            }

            // Set expand state
            if (rate.isExpanded) {
                expandCard(true, false)
            } else {
                collapseCard(true, false)
            }

            // Show/hide expand button (only if there's additional info)
            val hasAdditionalInfo = rate.vehicleTypes.isNotEmpty() || rate.applicableDays.isNotEmpty()
            btnExpand.parent.visibility = if (hasAdditionalInfo) View.VISIBLE else View.GONE

            // Add entrance animation
            itemView.startAnimation(
                AnimationUtils.loadAnimation(
                    itemView.context,
                    R.anim.slide_up
                )
            )
        }

        private fun toggleExpand(position: Int) {
            val isCurrentlyExpanded = rates[position].isExpanded

            if (isCurrentlyExpanded) {
                collapseCard()
            } else {
                expandCard()
            }

            onExpandToggle?.invoke(position, !isCurrentlyExpanded)
        }

        private fun expandCard(animate: Boolean = true, notify: Boolean = true) {
            if (animate) {
                // Rotate icon
                ObjectAnimator.ofFloat(expandIcon, "rotation", 0f, 180f)
                    .setDuration(300)
                    .start()

                // Expand details with animation
                layoutAdditionalInfo.visibility = View.VISIBLE
                layoutAdditionalInfo.alpha = 0f
                layoutAdditionalInfo.scaleY = 0.8f

                layoutAdditionalInfo.animate()
                    .alpha(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                // Elevate card
                cardRate.animate()
                    .translationZ(12f)
                    .setDuration(300)
                    .start()
            } else {
                expandIcon.rotation = 180f
                layoutAdditionalInfo.visibility = View.VISIBLE
                layoutAdditionalInfo.alpha = 1f
                layoutAdditionalInfo.scaleY = 1f
            }
        }

        private fun collapseCard(animate: Boolean = true, notify: Boolean = true) {
            if (animate) {
                // Rotate icon
                ObjectAnimator.ofFloat(expandIcon, "rotation", 180f, 0f)
                    .setDuration(300)
                    .start()

                // Collapse details with animation
                layoutAdditionalInfo.animate()
                    .alpha(0f)
                    .scaleY(0.8f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        layoutAdditionalInfo.visibility = View.GONE
                    }
                    .start()

                // Lower card
                cardRate.animate()
                    .translationZ(0f)
                    .setDuration(300)
                    .start()
            } else {
                expandIcon.rotation = 0f
                layoutAdditionalInfo.visibility = View.GONE
                layoutAdditionalInfo.alpha = 0f
                layoutAdditionalInfo.scaleY = 0.8f
            }
        }

        private fun animateCardPress(isPressed: Boolean) {
            val scale = if (isPressed) 0.98f else 1f
            cardRate.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(150)
                .start()
        }

        private fun animateStatusBadge(isActive: Boolean) {
            if (isActive) {
                statusBadge.visibility = View.VISIBLE
                statusBadge.scaleX = 0.8f
                statusBadge.scaleY = 0.8f
                statusBadge.alpha = 0f

                statusBadge.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            } else {
                statusBadge.animate()
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        statusBadge.visibility = View.GONE
                    }
                    .start()
            }
        }
    }

    var onItemClick: ((position: Int) -> Unit)? = null
    var onSwitchToggle: ((position: Int, isActive: Boolean) -> Unit)? = null
    var onExpandToggle: ((position: Int, isExpanded: Boolean) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.attractive_rate_item, parent, false)
        return RateViewHolder(view)
    }

    override fun onBindViewHolder(holder: RateViewHolder, position: Int) {
        holder.bind(rates[position])
    }

    override fun getItemCount(): Int = rates.size
}