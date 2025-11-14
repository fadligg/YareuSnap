package lat.pam.yareusnap.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import lat.pam.yareusnap.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : Fragment() {

    // Variabel Kamera
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    // Variabel UI
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var viewFinder: PreviewView
    private lateinit var btnCapture: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvFoodName: TextView
    private lateinit var tvRecommendation: TextView
    private lateinit var ivResultImage: ImageView



    // Permission Launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Izin kamera wajib diterima!", Toast.LENGTH_SHORT).show()
            }
        }

    // 1. Setup Layout Fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    // 2. Setup Logic setelah Layout jadi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi View
        viewFinder = view.findViewById(R.id.viewFinder)
        btnCapture = view.findViewById(R.id.btnCapture)
        progressBar = view.findViewById(R.id.progressBar)
        tvFoodName = view.findViewById(R.id.tvFoodName)
        tvRecommendation = view.findViewById(R.id.tvRecommendation)
        ivResultImage = view.findViewById(R.id.ivResultImage)

        // Setup Bottom Sheet
        val bottomSheet = view.findViewById<NestedScrollView>(R.id.bottomSheetLayout)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN // Awalnya sembunyi

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Cek Izin Kamera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Tombol Shutter logic
        btnCapture.setOnClickListener {
            takePhoto()
        }
    }

    // --- LOGIC KAMERA ---
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                // PENTING: Pakai viewLifecycleOwner di Fragment
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Gagal start kamera", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Simpan di Cache (Sementara)
        val photoFile = File(
            requireContext().externalCacheDir,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Gagal ambil foto: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Foto Tersimpan: ${photoFile.absolutePath}"
                    Log.d(TAG, msg)

                    // Tampilkan UI Hasil (Bottom Sheet Slide Up)
                    showBottomSheetResult(photoFile)
                }
            }
        )
    }

    // --- LOGIC GOOGLE LENS EFFECT ---
    private fun showBottomSheetResult(photoFile: File) {
        viewFinder.post {
            // 1. Naikkan Bottom Sheet
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

            // 2. TAMPILKAN GAMBAR YANG BARU DIFOTO
            val photoUri = android.net.Uri.fromFile(photoFile)
            ivResultImage.setImageURI(photoUri) // <--- MAGIC-NYA DISINI

            // 3. Set UI Loading
            progressBar.visibility = View.VISIBLE
            tvFoodName.text = "YareuSnap AI..."
            tvRecommendation.text = "Sedang menganalisis nutrisi..."

            // 4. Simulasi Delay AI
            viewFinder.postDelayed({
                progressBar.visibility = View.GONE
                tvFoodName.text = "Nasi Goreng"
                tvRecommendation.text = "Kalori: 300 kkal. \nSaran: Kurangi porsi nasi, tambah timun."
            }, 2000)
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "YareuSnapFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}