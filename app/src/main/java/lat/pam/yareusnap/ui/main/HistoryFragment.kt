package lat.pam.yareusnap.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import lat.pam.yareusnap.R
import lat.pam.yareusnap.data.database.AppDatabase
import lat.pam.yareusnap.data.database.FoodEntity

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: HistoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Hubungkan variabel dengan ID di XML
        rvHistory = view.findViewById(R.id.rvHistory)

        // 2. Setup RecyclerView Layout
        rvHistory.layoutManager = LinearLayoutManager(context)

        // 3. Inisialisasi adapter (DITAMBAHKAN Listener Klik di sini)
        // Adapter sekarang butuh 2 parameter: list kosong & fungsi ketika diklik
        adapter = HistoryAdapter(emptyList()) { selectedFood ->
            // Aksi saat item diklik: Tampilkan Modal Detail
            showDetailModal(selectedFood)
        }

        rvHistory.adapter = adapter

        // 4. Ambil data dari Room Database (Live Update)
        val db = AppDatabase.getDatabase(requireContext())
        db.foodDao().getAllFood().observe(viewLifecycleOwner) { foodList ->
            // Saat data di database berubah, update tampilan
            adapter.updateData(foodList)
        }
    }

    // Fungsi untuk memunculkan Bottom Sheet Detail
    private fun showDetailModal(food: FoodEntity) {
        val bottomSheet = HistoryDetailBottomSheet.newInstance(
            id = food.id,
            name = food.foodName,
            calories = food.calories,
            date = food.date,
            imagePath = food.imagePath,
            advice = food.advice

        )
        bottomSheet.show(parentFragmentManager, HistoryDetailBottomSheet.TAG)
    }
}