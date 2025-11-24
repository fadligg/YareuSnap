package lat.pam.yareusnap.api

import lat.pam.yareusnap.data.FoodResponse
import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    // Endpoint untuk mendapatkan 1 makanan acak (Cocok buat test scan)
    @GET("random.php")
    fun getRandomFood(): Call<FoodResponse>
}