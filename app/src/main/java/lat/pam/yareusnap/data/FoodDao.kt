package lat.pam.yareusnap.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FoodDao {
    @Insert
    suspend fun insertFood(food: FoodEntity)

    @Query("SELECT * FROM food_history ORDER BY id DESC")
    fun getAllFood(): LiveData<List<FoodEntity>>
}