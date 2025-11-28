package lat.pam.yareusnap.ui.main

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import lat.pam.yareusnap.R
import lat.pam.yareusnap.data.database.FoodEntity
import java.io.File

class HistoryAdapter(
    private var historyList: List<FoodEntity>,
    private val onItemClick: (FoodEntity) -> Unit // Callback untuk klik item
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgItem: ImageView = view.findViewById(R.id.imgHistoryItem)
        val tvName: TextView = view.findViewById(R.id.tvHistoryName)
        val tvInfo: TextView = view.findViewById(R.id.tvHistoryInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]

        // Set Data Text
        holder.tvName.text = item.foodName
        holder.tvInfo.text = "${item.calories} • ${item.date}"

        // Set Data Gambar
        val imgFile = File(item.imagePath)
        if (imgFile.exists()) {
            holder.imgItem.setImageURI(Uri.fromFile(imgFile))
        } else {
            holder.imgItem.setImageResource(R.drawable.ic_launcher_background)
        }

        // Saat item list ditekan, jalankan fungsi onItemClick
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = historyList.size

    fun updateData(newList: List<FoodEntity>) {
        historyList = newList
        notifyDataSetChanged()
    }
}