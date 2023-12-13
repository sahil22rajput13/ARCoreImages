package com.example.arcore

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.arcore.databinding.ActivityMainBinding
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.ArCoreApk.getInstance
import com.google.ar.core.exceptions.UnavailableException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startARFunctionality()
            } else {
                showPermissionDeniedSnackbar(
                    findViewById(android.R.id.content),
                    "Camera permission is required for AR functionality",
                    8000
                )
            }
        }

    private fun startARFunctionality() {
        maybeEnableArButton()
        binding.mArButton.setOnClickListener {
            isARCoreSupportedAndUpToDate()
           startActivity(Intent(this@MainActivity, MainActivity2::class.java))
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission()
        }else{
            startARFunctionality()
        }
    }


    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun maybeEnableArButton() = with(binding) {
        val availability = getInstance().checkAvailability(this@MainActivity)
        if (availability.isSupported) {
            mArButton.visibility = View.VISIBLE
            mArButton.isEnabled = true
        } else {
            mArButton.visibility = View.INVISIBLE
            mArButton.isEnabled = false
        }
    }

    private fun isARCoreSupportedAndUpToDate(): Boolean {
        return when (getInstance().checkAvailability(this)) {
            Availability.SUPPORTED_INSTALLED -> true
            Availability.SUPPORTED_APK_TOO_OLD, Availability.SUPPORTED_NOT_INSTALLED -> {
                try {
                    // Request ARCore installation or update if needed.
                    when (getInstance().requestInstall(this, true)) {
                        InstallStatus.INSTALL_REQUESTED -> {
                            Log.i(TAG, "ARCore installation requested.")
                            false
                        }

                        InstallStatus.INSTALLED -> true
                    }
                } catch (e: UnavailableException) {
                    Log.e(TAG, "ARCore not installed", e)
                    false
                }
            }

            Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE ->
                // This device is not supported for AR.
                false

            Availability.UNKNOWN_CHECKING -> {
                return false
            }

            Availability.UNKNOWN_ERROR, Availability.UNKNOWN_TIMED_OUT -> {
                return false
            }
        }
    }
}