package com.pixeldialer.app.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

/**
 * Required system service for a default dialer app.
 * Android's Telecom framework binds to this service and hands us Call objects
 * for every incoming/outgoing call once this app is set as the default dialer.
 */
class PixelInCallService : InCallService() {

    companion object {
        private const val TAG = "PixelInCallService"

        // Holds the currently active call so the UI layer (InCallActivity) can read/control it.
        var currentCall: Call? = null
            private set

        private val listeners = mutableListOf<(Call?) -> Unit>()

        fun addCallListener(listener: (Call?) -> Unit) {
            listeners.add(listener)
            listener(currentCall)
        }

        fun removeCallListener(listener: (Call?) -> Unit) {
            listeners.remove(listener)
        }

        private fun notifyListeners() {
            listeners.forEach { it(currentCall) }
        }
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            Log.d(TAG, "Call state changed: $state")
            notifyListeners()
            if (state == Call.STATE_DISCONNECTED) {
                call.unregisterCallback(this)
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call added: ${call.details.handle}")
        currentCall = call
        call.registerCallback(callCallback)
        notifyListeners()
        launchInCallUi()
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed")
        call.unregisterCallback(callCallback)
        if (currentCall == call) {
            currentCall = null
        }
        notifyListeners()
    }

    private fun launchInCallUi() {
        val intent = Intent(this, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    /** Call action helpers, invoked from the Compose UI. */
    fun answer() {
        currentCall?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
    }

    fun reject() {
        currentCall?.reject(false, null)
    }

    fun hangup() {
        currentCall?.disconnect()
    }

    fun toggleHold() {
        currentCall?.let {
            if (it.state == Call.STATE_HOLDING) it.unhold() else it.hold()
        }
    }
}
