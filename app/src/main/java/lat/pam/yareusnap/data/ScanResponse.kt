package lat.pam.yareusnap.data

import com.google.gson.annotations.SerializedName

// Level 1: Wadah Utama
data class ScanResponse(
    @SerializedName("detected_foods")
    val detectedFoods: List<String>?, // GANTI JADI LIST (Karena outputnya ["macarons"])

    @SerializedName("nutrition_analysis")
    val nutritionAnalysis: NutritionAnalysis? // GANTI JADI OBJECT (Karena isinya { ... })
)

// Level 2: Isi dari nutrition_analysis
data class NutritionAnalysis(
    @SerializedName("food_type")
    val foodType: String?,

    @SerializedName("recommendations")
    val recommendations: List<String>? // GANTI JADI LIST (Karena outputnya ["Saran 1", "Saran 2"])
)