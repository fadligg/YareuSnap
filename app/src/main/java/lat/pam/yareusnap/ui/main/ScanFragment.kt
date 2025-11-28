package lat.pam.yareusnap.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button // Import Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope // Import LifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers // Import Coroutines
import kotlinx.coroutines.launch
import lat.pam.yareusnap.R
import lat.pam.yareusnap.data.database.AppDatabase // Import Database
import lat.pam.yareusnap.data.database.FoodEntity // Import Entity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class ScanFragment : Fragment() {

    // --- API KEY MISTRAL ---
    private val MISTRAL_API_KEY = "Bearer U4r3DVYoLxz3U8mwUID8EvVRg1bTnavc"

    // Variabel Kamera & UI
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var viewFinder: PreviewView
    private lateinit var btnCapture: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvFoodName: TextView
    private lateinit var tvRecommendation: TextView
    private lateinit var ivResultImage: ImageView
    private lateinit var tvCalories: TextView

    // --- TAMBAHAN BARU: Variabel Tombol & File ---
    private lateinit var btnSave: Button
    private lateinit var btnDiscard: Button
    private var currentPhotoFile: File? = null // Untuk menyimpan file sementara
    // ---------------------------------------------

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) startCamera() else Toast.makeText(requireContext(), "Izin ditolak", Toast.LENGTH_SHORT).show()
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewFinder = view.findViewById(R.id.viewFinder)
        btnCapture = view.findViewById(R.id.btnCapture)
        progressBar = view.findViewById(R.id.progressBar)
        tvFoodName = view.findViewById(R.id.tvFoodName)
        tvRecommendation = view.findViewById(R.id.tvRecommendation)
        ivResultImage = view.findViewById(R.id.ivResultImage)
        tvCalories = view.findViewById(R.id.tvCalories)

        // --- TAMBAHAN BARU: Inisialisasi Tombol ---
        btnSave = view.findViewById(R.id.btnSave)
        btnDiscard = view.findViewById(R.id.btnDiscard)
        // ------------------------------------------

        val bottomSheet = view.findViewById<NestedScrollView>(R.id.bottomSheetLayout)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isFitToContents = false
        bottomSheetBehavior.halfExpandedRatio = 0.6f
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) startCamera() else requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        btnCapture.setOnClickListener { takePhoto() }

        // --- TAMBAHAN BARU: Logic Click Listener Tombol ---
        btnDiscard.setOnClickListener {
            // Sembunyikan sheet dan reset kamera
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            startCamera()
        }

        btnSave.setOnClickListener {
            saveToHistory()
        }
        // --------------------------------------------------
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(viewFinder.display.rotation)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) { Log.e(TAG, "Gagal start kamera", exc) }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(requireContext().externalCacheDir, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) { Log.e(TAG, "Gagal foto", exc) }
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                showBottomSheetResult(photoFile)
            }
        })
    }

    private fun showBottomSheetResult(photoFile: File) {
        // --- TAMBAHAN BARU: Simpan referensi file ke variabel global ---
        currentPhotoFile = photoFile
        // ---------------------------------------------------------------

        viewFinder.post {
            val photoUri = android.net.Uri.fromFile(photoFile)
            ivResultImage.setImageURI(photoUri)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            progressBar.visibility = View.VISIBLE
            tvFoodName.text = "Menganalisis..."
            tvRecommendation.text = "Sedang mengidentifikasi makanan..."
            tvCalories.text = "..." // Reset teks kalori

            // Panggil API Render (Tahap 1)
            fetchFoodData(photoFile)
        }
    }

    // --- TAMBAHAN BARU: Fungsi Simpan ke Database ---
    private fun saveToHistory() {
        val file = currentPhotoFile
        if (file != null) {
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(System.currentTimeMillis())

            val foodData = FoodEntity(
                foodName = tvFoodName.text.toString(), // Ambil text hasil deteksi
                calories = tvCalories.text.toString(), // Ambil text hasil kalori
                imagePath = file.absolutePath,         // Simpan path gambar
                date = currentTime
            )

            // Jalankan Insert di Background Thread (IO)
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(requireContext())
                db.foodDao().insertFood(foodData)

                // Balik ke Main Thread untuk update UI
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Berhasil disimpan ke Riwayat!", Toast.LENGTH_SHORT).show()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    startCamera() // Restart kamera
                }
            }
        } else {
            Toast.makeText(context, "Gagal menyimpan: Foto tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }
    // ------------------------------------------------

    // --- LOGIC CHAINING (RENDER -> MISTRAL) ---

    private fun fetchFoodData(photoFile: File) {
        val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", photoFile.name, requestFile)

        val client = lat.pam.yareusnap.api.ApiConfig.getApiService().uploadImage(body)

        client.enqueue(object : retrofit2.Callback<lat.pam.yareusnap.data.ScanResponse> {
            override fun onResponse(call: retrofit2.Call<lat.pam.yareusnap.data.ScanResponse>, response: retrofit2.Response<lat.pam.yareusnap.data.ScanResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    val detectedFoods = result?.detectedFoods

                    if (!detectedFoods.isNullOrEmpty()) {
                        val foodNameRaw = detectedFoods[0]
                        val foodNameClean = foodNameRaw.replace("_", " ").split(" ").joinToString(" ") { it.capitalize() }

                        tvFoodName.text = foodNameClean
                        tvRecommendation.text = "Sedang bertanya ke Mistral AI..."

                        askMistral(foodNameClean)

                    } else {
                        progressBar.visibility = View.GONE
                        tvFoodName.text = "Tidak Terdeteksi"
                        tvRecommendation.text = "Coba ambil foto ulang dengan pencahayaan lebih baik."
                    }
                } else {
                    progressBar.visibility = View.GONE
                    tvFoodName.text = "Error Render"
                    tvRecommendation.text = "Gagal deteksi: ${response.code()}"
                }
            }

            override fun onFailure(call: retrofit2.Call<lat.pam.yareusnap.data.ScanResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                tvFoodName.text = "Gagal Koneksi"
                tvRecommendation.text = "Cek internet: ${t.message}"
            }
        })
    }

    private fun askMistral(foodName: String) {
        val prompt = """
            Saya punya makanan: $foodName.
            Berikan estimasi kalori (wajib tulis angka diikuti 'kkal', misal: 250 kkal).
            Lalu berikan rincian makronutrisi dan 3 saran kesehatan singkat.
            Gunakan emoji. Bahasa Indonesia.
        """.trimIndent()

        val requestData = lat.pam.yareusnap.data.MistralRequest(
            messages = listOf(
                lat.pam.yareusnap.data.MistralMessage(role = "user", content = prompt)
            )
        )

        val client = lat.pam.yareusnap.api.ApiConfig.getMistralService().chatWithMistral(MISTRAL_API_KEY, requestData)

        client.enqueue(object : retrofit2.Callback<lat.pam.yareusnap.data.MistralResponse> {
            override fun onResponse(call: retrofit2.Call<lat.pam.yareusnap.data.MistralResponse>, response: retrofit2.Response<lat.pam.yareusnap.data.MistralResponse>) {
                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val aiReply = response.body()?.choices?.firstOrNull()?.message?.content ?: "Tidak ada data."

                    val calorieRegex = Regex("(\\d+)\\s*(?:kkal|kcal|kalori)", RegexOption.IGNORE_CASE)
                    val matchResult = calorieRegex.find(aiReply)

                    if (matchResult != null) {
                        val calorieValue = matchResult.groupValues[1]
                        tvCalories.text = "$calorieValue kkal"
                    } else {
                        tvCalories.text = "Cek Kemasan"
                    }

                    tvRecommendation.text = aiReply

                } else {
                    tvRecommendation.text = "Gagal koneksi AI: ${response.code()}"
                }
            }

            override fun onFailure(call: retrofit2.Call<lat.pam.yareusnap.data.MistralResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                tvRecommendation.text = "Error: ${t.message}"
            }
        })
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    override fun onDestroyView() { super.onDestroyView(); cameraExecutor.shutdown() }
    companion object { private const val TAG = "YareuSnapFragment"; private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS" }
}