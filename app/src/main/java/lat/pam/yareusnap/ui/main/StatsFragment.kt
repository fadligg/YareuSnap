package lat.pam.yareusnap.ui.main

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import lat.pam.yareusnap.R
import lat.pam.yareusnap.data.database.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class StatsFragment : Fragment(R.layout.fragment_stats) {

    private lateinit var tvTodayCalories: TextView
    private lateinit var progressCalories: ProgressBar

    // View Makronutrisi
    private lateinit var tvProtein: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFat: TextView
    private lateinit var tvFiber: TextView

    // View Stats Lain
    private lateinit var tvTotalScan: TextView
    private lateinit var tvLastCalories: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inisialisasi View
        tvTodayCalories = view.findViewById(R.id.tvTodayCalories)
        progressCalories = view.findViewById(R.id.progressCalories)

        tvProtein = view.findViewById(R.id.tvProtein)
        tvCarbs = view.findViewById(R.id.tvCarbs)
        tvFat = view.findViewById(R.id.tvFat)
        tvFiber = view.findViewById(R.id.tvFiber)

        tvTotalScan = view.findViewById(R.id.tvTotalScan)
        tvLastCalories = view.findViewById(R.id.tvLastCalories)

        // 2. Setup Database Observer
        val db = AppDatabase.getDatabase(requireContext())
        db.foodDao().getAllFood().observe(viewLifecycleOwner) { foodList ->

            if (foodList.isNotEmpty()) {

                var totalCaloriesToday = 0
                var totalProtein = 0
                var totalCarbs = 0
                var totalFat = 0
                var totalFiber = 0

                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                for (food in foodList) {
                    // Cek Tanggal Hari Ini
                    if (food.date.startsWith(todayDate)) {

                        // Hitung Kalori
                        val calValue = food.calories.replace(Regex("\\D+"), "").toIntOrNull() ?: 0
                        totalCaloriesToday += calValue

                        // Hitung Makronutrisi dari teks 'advice' menggunakan Regex
                        // Mencari pola seperti "Protein: 20g" atau "Lemak: 10 gr"
                        val adviceText = food.advice ?: ""

                        totalProtein += extractValue(adviceText, "Protein")
                        totalCarbs += extractValue(adviceText, "Karbohidrat")
                        totalFat += extractValue(adviceText, "Lemak")
                        totalFiber += extractValue(adviceText, "Serat")
                    }
                }

                // Update UI Kalori
                tvTodayCalories.text = "$totalCaloriesToday"
                progressCalories.progress = totalCaloriesToday

                // Update UI Makronutrisi
                tvProtein.text = "${totalProtein}g"
                tvCarbs.text = "${totalCarbs}g"
                tvFat.text = "${totalFat}g"
                tvFiber.text = "${totalFiber}g"

                // Update UI Total & Terakhir
                val totalItems = foodList.size
                val lastItemCal = foodList[0].calories.replace(Regex("\\D+"), "").toIntOrNull() ?: 0

                tvTotalScan.text = "$totalItems"
                tvLastCalories.text = "$lastItemCal"

            } else {
                resetViews()
            }
        }
    }

    // Fungsi bantuan untuk mencari angka dalam teks berdasarkan kata kunci
    private fun extractValue(text: String, keyword: String): Int {
        // Pola Regex: Cari Kata Kunci -> spasi/titik dua -> ANGKA
        // (?i) artinya ignore case (huruf besar/kecil dianggap sama)
        val pattern = Pattern.compile("(?i)$keyword\\s*[:\\-]?\\s*(\\d+)")
        val matcher = pattern.matcher(text)

        return if (matcher.find()) {
            matcher.group(1)?.toIntOrNull() ?: 0
        } else {
            0
        }
    }

    private fun resetViews() {
        tvTodayCalories.text = "0"
        progressCalories.progress = 0
        tvProtein.text = "0g"
        tvCarbs.text = "0g"
        tvFat.text = "0g"
        tvFiber.text = "0g"
        tvTotalScan.text = "0"
        tvLastCalories.text = "0"
    }
}