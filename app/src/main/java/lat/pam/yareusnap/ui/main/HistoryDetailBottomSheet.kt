package lat.pam.yareusnap.ui.main

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope // PENTING
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers // PENTING
import kotlinx.coroutines.launch
import lat.pam.yareusnap.R
import lat.pam.yareusnap.data.database.AppDatabase
import lat.pam.yareusnap.data.database.FoodEntity
import java.io.File

class HistoryDetailBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_detail_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Ambil Data (Termasuk ID)
        val id = arguments?.getInt("id") ?: 0 // Default 0
        val name = arguments?.getString("name") ?: "-"
        val calories = arguments?.getString("calories") ?: "-"
        val date = arguments?.getString("date") ?: "-"
        val imagePath = arguments?.getString("imagePath") ?: ""
        val advice = arguments?.getString("advice") ?: "Tidak ada detail."

        // Hubungkan Views
        val tvName: TextView = view.findViewById(R.id.tvDetailName)
        val tvCalories: TextView = view.findViewById(R.id.tvDetailCalories)
        val tvDate: TextView = view.findViewById(R.id.tvDetailDate)
        val ivImage: ImageView = view.findViewById(R.id.ivDetailImage)
        val btnClose: Button = view.findViewById(R.id.btnClose)
        val btnDelete: Button = view.findViewById(R.id.btnDelete) // Tombol Hapus Baru
        val tvDetailAdvice: TextView = view.findViewById(R.id.tvDetailAdvice)

        // Set Tampilan
        tvName.text = name
        tvCalories.text = calories
        tvDate.text = date
        tvDetailAdvice.text = advice

        val imgFile = File(imagePath)
        if (imgFile.exists()) {
            ivImage.setImageURI(Uri.fromFile(imgFile))
        } else {
            ivImage.setImageResource(R.drawable.ic_launcher_background)
        }

        // Logic Tombol Tutup
        btnClose.setOnClickListener { dismiss() }

        // Logic Tombol Hapus (BARU)
        btnDelete.setOnClickListener {
            // Konfirmasi hapus (opsional, tapi disarankan langsung hapus saja biar cepat)
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(requireContext())

                // Buat objek dummy dengan ID yang sama untuk dihapus
                // Room menghapus berdasarkan Primary Key (ID), data lain boleh dummy
                val foodToDelete = FoodEntity(id = id, foodName = name, calories = calories, imagePath = imagePath, date = date, advice = advice)

                db.foodDao().deleteFood(foodToDelete)

                // Kembali ke UI Thread
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Data dihapus", Toast.LENGTH_SHORT).show()
                    dismiss() // Tutup modal
                    // HistoryFragment akan otomatis update karena pakai LiveData!
                }
            }
        }
    }

    // In file: D:/Hackaton IT FAIR XIV/app/src/main/java/lat/pam/yareusnap/ui/main/HistoryDetailBottomSheet.kt

    companion object {
        const val TAG = "HistoryDetailBottomSheet"

        // Update newInstance to accept ID and advice
        fun newInstance(id: Int, name: String, calories: String, date: String, imagePath: String, advice: String): HistoryDetailBottomSheet {
            val fragment = HistoryDetailBottomSheet()
            val args = Bundle()
            args.putInt("id", id) // Simpan ID
            args.putString("name", name)
            args.putString("calories", calories)
            args.putString("date", date)
            args.putString("imagePath", imagePath)
            args.putString("advice", advice)
            fragment.arguments = args
            return fragment
        }
    }

}