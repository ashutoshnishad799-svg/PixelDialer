package com.pixeldialer.app.telecom

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.pixeldialer.app.PixelDialerApp
import com.pixeldialer.app.data.db.PixelDialerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Required system service for a default dialer app.
 * Android's Telecom framework calls onScreenCall for every incoming call
 * before it rings, letting us silently reject numbers on the block list.
 */
class PixelCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: run {
            respondAllow(callDetails)
            return
        }

        scope.launch {
            val db = PixelDialerDatabase.getInstance(applicationContext)
            val isBlocked = db.blockedNumberDao().isBlocked(number)

            if (isBlocked) {
                Log.d("PixelCallScreening", "Rejecting blocked number: $number")
                respondReject(callDetails)
            } else {
                respondAllow(callDetails)
            }
        }
    }

    private fun respondAllow(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
    }

    private fun respondReject(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(true)
            .build()
        respondToCall(callDetails, response)
    }
}
