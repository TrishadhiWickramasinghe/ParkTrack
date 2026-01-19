import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class ItemParkingHistory(private val records: List<ParkingRecord>) :
    RecyclerView.Adapter<ParkingRecordAdapter.ParkingRecordViewHolder>() {

    data class ParkingRecord(
        val id: String,
        val carNumber: String,
        val date: String,
        val time: String,
        val entryTime: String,
        val entryDate: String,
        val exitTime: String,
        val exitDate: String,
        val duration: String,
        val amount: String,
        val status: String,
        val parkingSlot: String,
        val vehicleType: String,
        val paymentMode: String,
        val statusColor: Int = R.color.green_500,
        val isExpanded: Boolean = false
    )

    inner class ParkingRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Header views
        private val iconParking: ImageView = itemView.findViewById(R.id.iconParking)
        private val tvCarNumber: TextView = itemView.findViewById(R.id.tvCarNumber)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
        private val cardStatus: MaterialCardView = itemView.findViewById(R.id.cardStatus)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        // Entry/Exit views
        private val layoutEntryExit: MaterialCardView = itemView.findViewById(R.id.layoutEntryExit)
        private val tvEntryTime: TextView = itemView.findViewById(R.id.tvEntryTime)
        private val tvEntryDate: TextView = itemView.findViewById(R.id.tvEntryDate)
        private val tvExitTime: TextView = itemView.findViewById(R.id.tvExitTime)
        private val tvExitDate: TextView = itemView.findViewById(R.id.tvExitDate)

        // Stats views
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        // Extra details views
        private val layoutExtraDetails: MaterialCardView = itemView.findViewById(R.id.layoutExtraDetails)
        private val tvParkingSlot: TextView = itemView.findViewById(R.id.tvParkingSlot)
        private val tvVehicleType: TextView = itemView.findViewById(R.id.tvVehicleType)
        private val tvPaymentMode: TextView = itemView.findViewById(R.id.tvPaymentMode)

        // Expand button
        private val btnExpand: MaterialCardView = itemView.findViewById(R.id.btnExpand)
        private val expandIcon: ImageView = btnExpand.getChildAt(0) as ImageView

        // Main card
        private val mainCard: MaterialCardView = itemView.findViewById(R.id.mainCard)

        init {
            // Set click listener for card
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Handle card click
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

            // Add ripple effect animation
            itemView.setOnTouchListener { v, event ->
                v.performClick()
                false
            }
        }

        fun bind(record: ParkingRecord) {
            // Set data
            tvCarNumber.text = record.carNumber
            tvDate.text = record.date
            tvTime.text = record.time
            tvEntryTime.text = record.entryTime
            tvEntryDate.text = record.entryDate
            tvExitTime.text = record.exitTime
            tvExitDate.text = record.exitDate
            tvDuration.text = record.duration
            tvAmount.text = record.amount
            tvStatus.text = record.status
            tvParkingSlot.text = record.parkingSlot
            tvVehicleType.text = record.vehicleType
            tvPaymentMode.text = record.paymentMode

            // Set status color
            val statusColor = ContextCompat.getColor(itemView.context, record.statusColor)
            statusIndicator.background.setTint(statusColor)
            cardStatus.strokeColor = statusColor
            tvStatus.setTextColor(statusColor)

            // Set status background based on status
            when (record.status.lowercase()) {
                "completed" -> {
                    cardStatus.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.green_50)
                    )
                }
                "active" -> {
                    cardStatus.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.blue_50)
                    )
                }
                "pending" -> {
                    cardStatus.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.orange_50)
                    )
                }
                "cancelled" -> {
                    cardStatus.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.red_50)
                    )
                }
            }

            // Set expand state
            if (record.isExpanded) {
                expandCard(true, false)
            } else {
                collapseCard(true, false)
            }

            // Add entrance animation
            itemView.startAnimation(
                AnimationUtils.loadAnimation(
                    itemView.context,
                    R.anim.slide_up
                )
            )
        }

        private fun toggleExpand(position: Int) {
            val isCurrentlyExpanded = records[position].isExpanded

            if (isCurrentlyExpanded) {
                collapseCard()
            } else {
                expandCard()
            }

            // Notify adapter
            onExpandToggle?.invoke(position, !isCurrentlyExpanded)
        }

        private fun expandCard(animate: Boolean = true, notify: Boolean = true) {
            if (animate) {
                // Rotate icon
                ObjectAnimator.ofFloat(expandIcon, "rotation", 0f, 180f)
                    .setDuration(300)
                    .start()

                // Expand details with animation
                layoutExtraDetails.visibility = View.VISIBLE
                layoutExtraDetails.alpha = 0f
                layoutExtraDetails.scaleY = 0.8f

                layoutExtraDetails.animate()
                    .alpha(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                // Elevate main card
                mainCard.animate()
                    .translationZ(12f)
                    .setDuration(300)
                    .start()
            } else {
                expandIcon.rotation = 180f
                layoutExtraDetails.visibility = View.VISIBLE
                layoutExtraDetails.alpha = 1f
                layoutExtraDetails.scaleY = 1f
            }
        }

        private fun collapseCard(animate: Boolean = true, notify: Boolean = true) {
            if (animate) {
                // Rotate icon
                ObjectAnimator.ofFloat(expandIcon, "rotation", 180f, 0f)
                    .setDuration(300)
                    .start()

                // Collapse details with animation
                layoutExtraDetails.animate()
                    .alpha(0f)
                    .scaleY(0.8f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        layoutExtraDetails.visibility = View.GONE
                    }
                    .start()

                // Lower main card
                mainCard.animate()
                    .translationZ(0f)
                    .setDuration(300)
                    .start()
            } else {
                expandIcon.rotation = 0f
                layoutExtraDetails.visibility = View.GONE
                layoutExtraDetails.alpha = 0f
                layoutExtraDetails.scaleY = 0.8f
            }
        }
    }

    var onItemClick: ((position: Int) -> Unit)? = null
    var onExpandToggle: ((position: Int, isExpanded: Boolean) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingRecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.attractive_parking_record_card, parent, false)
        return ParkingRecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParkingRecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size
}