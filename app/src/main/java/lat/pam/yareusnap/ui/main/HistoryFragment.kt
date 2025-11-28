package lat.pam.yareusnap.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import lat.pam.yareusnap.R
import lat.pam.yareusnap.data.database.AppDatabase // Pastikan path ini sesuai
// Jika error di import AppDatabase, coba ganti jadi: import lat.pam.yareusnap.ui.main.AppDatabase

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: HistoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Hubungkan variabel dengan ID di XML
        rvHistory = view.findViewById(R.id.rvHistory)

        // 2. Setup RecyclerView Layout
        rvHistory.layoutManager = LinearLayoutManager(context)

        // 3. Inisialisasi adapter dengan list kosong dulu
        adapter = HistoryAdapter(emptyList())
        rvHistory.adapter = adapter

        // 4. Ambil data dari Room Database (Live Update)
        // Pastikan context tidak null dengan requireContext()
        val db = AppDatabase.getDatabase(requireContext())

        db.foodDao().getAllFood().observe(viewLifecycleOwner) { foodList ->
            // Saat data di database berubah (nambah/hapus), update tampilan otomatis
            adapter.updateData(foodList)
        }
    }
}