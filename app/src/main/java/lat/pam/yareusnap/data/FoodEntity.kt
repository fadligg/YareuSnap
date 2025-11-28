package lat.pam.yareusnap.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_history")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val foodName: String,
    val calories: String,
    val imagePath: String,
    val date: String,
    val advice: String = ""
)