package lat.pam.yareusnap.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import lat.pam.yareusnap.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // 1. Definisi Variabel Kamera & UI
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>

    // Referensi ke View (sesuai ID di XML)
    private lateinit var viewFinder: PreviewView
    private lateinit var btnCapture: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvFoodName: TextView
    private lateinit var tvRecommendation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 2. Inisialisasi View
        viewFinder = findViewById(R.id.viewFinder)
        btnCapture = findViewById(R.id.btnCapture)
        progressBar = findViewById(R.id.progressBar)
        tvFoodName = findViewById(R.id.tvFoodName)
        tvRecommendation = findViewById(R.id.tvRecommendation)

        val bottomSheet = findViewById<NestedScrollView>(R.id.bottomSheetLayout)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        // Set state awal Bottom Sheet: TERSEMBUNYI (HIDDEN)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // 3. Cek Izin Kamera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // 4. Listener Tombol Capture
        btnCapture.setOnClickListener {
            takePhoto()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    // --- LOGIC KAMERA ---
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Mengikat siklus hidup kamera ke Activity
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview (Tampilan kamera di layar)
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // ImageCapture (Untuk ambil gambar)
            imageCapture = ImageCapture.Builder().build()

            // Pilih Kamera Belakang
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use case sebelumnya (biar fresh)
                cameraProvider.unbindAll()

                // Bind use cases ke camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Gagal memunculkan kamera", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Pastikan use case imageCapture sudah ada
        val imageCapture = imageCapture ?: return

        // Buat file output sementara untuk menyimpan hasil foto
        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Ambil Gambar
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Gagal ambil foto: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Foto berhasil: ${photoFile.absolutePath}"
                    Toast.makeText(baseContext, "Memproses AI...", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                    // --- DISINI MAGIC-NYA GOOGLE LENS ---
                    // Setelah foto diambil, kita munculkan Bottom Sheet
                    showBottomSheetResult(photoFile)
                }
            }
        )
    }

    // --- UI LOGIC ---
    private fun showBottomSheetResult(photoFile: File) {
        // 1. Munculkan Bottom Sheet
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        // 2. Tampilkan Loading, Sembunyikan Teks dulu
        progressBar.visibility = View.VISIBLE
        tvFoodName.text = "Menganalisis..."
        tvRecommendation.text = ""

        // TODO: Nanti di sini kita panggil Retrofit ke Python API
        // Simulasi delay seolah-olah sedang mikir (mocking)
        viewFinder.postDelayed({
            progressBar.visibility = View.GONE
            tvFoodName.text = "Nasi Goreng Spesial" // Dummy Data dulu
            tvRecommendation.text = "Kalori tinggi! Saran: Minum Jus Jeruk murni untuk vitamin C & serat tambahan."
        }, 2000) // Delay 2 detik
    }

    // --- PERMISSION LOGIC ---
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Izin kamera ditolak, aplikasi tidak bisa jalan.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "NutriScanCamera"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}
