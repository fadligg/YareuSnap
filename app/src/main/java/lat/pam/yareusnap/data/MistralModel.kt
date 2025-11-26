package lat.pam.yareusnap.data

import com.google.gson.annotations.SerializedName

// 1. Yang kita kirim ke Mistral
data class MistralRequest(
    val model: String = "mistral-tiny", // Model yang murah & cepat
    val messages: List<MistralMessage>
)

data class MistralMessage(
    val role: String,
    val content: String
)

// 2. Yang Mistral balas ke kita
data class MistralResponse(
    val choices: List<MistralChoice>?
)

data class MistralChoice(
    val message: MistralMessage?
)