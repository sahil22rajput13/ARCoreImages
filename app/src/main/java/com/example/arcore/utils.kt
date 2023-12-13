package com.example.arcore

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

fun Context.showPermissionDeniedSnackbar(view: View, customText: String, durationMillis: Long = 2000) {
    val snackbar = Snackbar.make(
        view,
        customText,
        Snackbar.LENGTH_INDEFINITE
    )

    snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.white))
    snackbar.setTextColor(ContextCompat.getColor(this, R.color.black))

    snackbar.setAction("Open Settings") {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", this.packageName, null)
        intent.data = uri
        startActivity(intent)
    }
    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed({
        snackbar.dismiss()
    }, durationMillis)

    snackbar.show()
}