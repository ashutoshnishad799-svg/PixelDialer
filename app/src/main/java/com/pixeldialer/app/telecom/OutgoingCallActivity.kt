package com.pixeldialer.app.telecom

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Handles android.intent.action.CALL intents (e.g. tapping a phone number
 * link in another app). Places the call through TelecomManager so it routes
 * through this app's InCallService, then finishes immediately — no UI of
 * its own, the InCallActivity takes over via PixelInCallService.onCallAdded.
 */
class OutgoingCallActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val number = intent?.data?.schemeSpecificPart

        if (number.isNullOrBlank()) {
            finish()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 101)
            finish()
            return
        }

        placeCall(number)
        finish()
    }

    private fun placeCall(number: String) {
        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        val uri = Uri.fromParts("tel", number, null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            telecomManager.placeCall(uri, Bundle())
        }
    }
}
