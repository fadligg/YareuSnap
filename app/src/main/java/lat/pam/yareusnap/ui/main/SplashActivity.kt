package lat.pam.yareusnap.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import lat.pam.yareusnap.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Menyembunyikan Action Bar agar full screen seperti desain LifeFit
        supportActionBar?.hide()

        // Handler untuk menunda selama 3 detik (3000ms) sebelum pindah
        Handler(Looper.getMainLooper()).postDelayed({
            // Pindah dari SplashActivity ke MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Menutup SplashActivity agar tidak bisa kembali ke sini saat tombol back ditekan
            finish()
        }, 3000) // 3000 artinya 3 detik
    }
}