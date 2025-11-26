package lat.pam.yareusnap.api

import lat.pam.yareusnap.data.MistralRequest
import lat.pam.yareusnap.data.MistralResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MistralApiService {
    @POST("v1/chat/completions")
    fun chatWithMistral(
        @Header("Authorization") token: String, // Bearer API_KEY
        @Body request: MistralRequest
    ): Call<MistralResponse>
}