package lat.pam.yareusnap.data

import com.google.gson.annotations.SerializedName

data class FoodResponse(
    @SerializedName("meals") val meals: List<MealItem>?
)

data class MealItem(
    @SerializedName("strMeal") val name: String,
    @SerializedName("strMealThumb") val imageUrl: String,
    @SerializedName("idMeal") val id: String
)