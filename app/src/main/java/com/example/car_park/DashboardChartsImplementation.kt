// DashboardChartsHelper.kt
package com.example.car_park

import android.content.Context
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.*

class DashboardChartsHelper(private val context: Context) {

    fun setupDailyIncomeBarChart(chart: BarChart, dailyData: Map<String, Double>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        var index = 0f
        dailyData.entries.sortedBy { it.key }.forEach { (date, amount) ->
            entries.add(BarEntry(index, amount.toFloat()))
            labels.add(formatDateLabel(date))
            index++
        }

        val dataSet = BarDataSet(entries, "Daily Income")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 10f

        val data = BarData(dataSet)
        chart.data = data

        // Configure chart
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.setDrawGridBackground(false)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value < labels.size) labels[value.toInt()] else ""
            }
        }
        xAxis.labelCount = labels.size.coerceAtMost(7)
        xAxis.granularity = 1f

        chart.axisLeft.setDrawGridLines(true)
        chart.axisRight.isEnabled = false

        chart.animateY(1000)
        chart.invalidate()
    }

    fun setupMonthlyIncomeLineChart(chart: LineChart, monthlyData: Map<String, Double>) {
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        var index = 0f
        monthlyData.entries.sortedBy { it.key }.forEach { (month, amount) ->
            entries.add(Entry(index, amount.toFloat()))
            labels.add(month)
            index++
        }

        val dataSet = LineDataSet(entries, "Monthly Income")
        dataSet.color = context.resources.getColor(R.color.primary_color)
        dataSet.valueTextSize = 10f
        dataSet.lineWidth = 2f
        dataSet.setDrawCircles(true)
        dataSet.setCircleColor(context.resources.getColor(R.color.primary_color))

        val data = LineData(dataSet)
        chart.data = data

        // Configure chart
        chart.description.isEnabled = false
        chart.legend.isEnabled = true

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value < labels.size) labels[value.toInt()] else ""
            }
        }
        xAxis.labelCount = labels.size

        chart.animateX(1000)
        chart.invalidate()
    }

    fun setupVehicleTypePieChart(chart: PieChart, typeData: Map<String, Int>) {
        val entries = ArrayList<PieEntry>()

        typeData.forEach { (type, count) ->
            if (count > 0) {
                entries.add(PieEntry(count.toFloat(), type))
            }
        }

        val dataSet = PieDataSet(entries, "Vehicle Types")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
        dataSet.valueTextSize = 12f

        val data = PieData(dataSet)
        chart.data = data

        // Configure chart
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.setHoleColor(android.graphics.Color.TRANSPARENT)
        chart.setTransparentCircleAlpha(0)

        chart.animateY(1000)
        chart.invalidate()
    }

    fun setupParkingHoursChart(chart: BarChart, hourlyData: Map<Int, Int>) {
        val entries = ArrayList<BarEntry>()

        (0..23).forEach { hour ->
            val count = hourlyData[hour] ?: 0
            entries.add(BarEntry(hour.toFloat(), count.toFloat()))
        }

        val dataSet = BarDataSet(entries, "Parking Hours Distribution")
        dataSet.color = context.resources.getColor(R.color.accent_color)

        val data = BarData(dataSet)
        chart.data = data

        // Configure chart
        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}:00"
            }
        }
        xAxis.labelCount = 24
        xAxis.granularity = 1f

        chart.animateY(1000)
        chart.invalidate()
    }

    private fun formatDateLabel(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun getDailyIncomeData(dbHelper: DatabaseHelper): Map<String, Double> {
        return dbHelper.getDailyIncomeForLast7Days()
    }

    fun getMonthlyIncomeData(dbHelper: DatabaseHelper): Map<String, Double> {
        return dbHelper.getMonthlyIncomeForLast6Months()
    }

    fun getVehicleTypeData(dbHelper: DatabaseHelper): Map<String, Int> {
        val data = mutableMapOf<String, Int>()

        // Example vehicle types - you should get this from your database
        data["Car"] = 45
        data["Bike"] = 30
        data["Scooter"] = 20
        data["Cycle"] = 5

        return data
    }

    fun getHourlyParkingData(dbHelper: DatabaseHelper): Map<Int, Int> {
        return dbHelper.getParkingCountByHour()
    }
}