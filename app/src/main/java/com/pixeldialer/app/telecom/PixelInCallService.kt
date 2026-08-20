package com.pixeldialer.app.telecom

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.pixeldialer.app.PixelDialerApp
import com.pixeldialer.app.data.db.CallDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Required system service for a default dialer app.
 * Android's Telecom framework binds to this service and hands us Call objects
 * for every incoming/outgoing call once this app is set as the default dialer.
 *
 * Tracks *all* simultaneous calls (not just one) so that "Add call" and
 * "Merge calls" (conference calling) work — a second call coming in while
 * one is active is a completely normal scenario a real dialer must handle.
 */
class PixelInCallService : InCallService() {

    companion object {
        private const val TAG = "PixelInCallService"

        private val _allCalls = mutableListOf<Call>()
        val allCalls: List<Call> get() = _allCalls.toList()

        /** The call the UI should currently render as "primary" — ringing takes priority, then active, then whatever's first. */
        val currentCall: Call?
            get() = _allCalls.firstOrNull { it.state == Call.STATE_RINGING }
                ?: _allCalls.firstOrNull { it.state == Call.STATE_ACTIVE }
                ?: _allCalls.firstOrNull()

        /** Any call other than the current primary one — surfaced in the UI as "the other call" for merge/swap. */
        val secondaryCall: Call?
            get() = _allCalls.firstOrNull { it != currentCall }

        val hasMultipleCalls: Boolean get() = _allCalls.size > 1

        private val listeners = mutableListOf<() -> Unit>()

        fun addCallListener(listener: () -> Unit) {
            listeners.add(listener)
            listener()
        }

        fun removeCallListener(listener: () -> Unit) {
            listeners.remove(listener)
        }

        private fun notifyListeners() {
            listeners.forEach { it() }
        }

        // Tracks call bookkeeping across the two disconnect paths a call
        // can take, so a call disconnecting after being answered doesn't
        // also get logged as missed.
        private val loggedAsMissed = mutableSetOf<String>()
        private val loggedAsAnswered = mutableSetOf<String>()
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            Log.d(TAG, "Call state changed: $state")
            notifyListeners()

            try {
                handleStateForNotification(call, state)
            } catch (e: Exception) {
                Log.e(TAG, "Notification handling failed on state change", e)
            }

            // Once a ringing call transitions to ACTIVE, remember that it
            // was actually answered — used below so a disconnect right
            // after doesn't get mistakenly logged as missed too.
            if (state == Call.STATE_ACTIVE) {
                callKey(call)?.let { loggedAsAnswered.add(it) }
            }

            if (state == Call.STATE_DISCONNECTED) {
                handleMissedCallLogging(call)
                call.unregisterCallback(this)
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call added: ${call.details.handle}, state=${call.state}, totalCalls=${_allCalls.size + 1}")
        _allCalls.add(call)
        call.registerCallback(callCallback)
        notifyListeners()

        try {
            handleStateForNotification(call, call.state)
        } catch (e: Exception) {
            Log.e(TAG, "Notification handling failed for new call", e)
        }

        val isIncoming = call.details?.callDirection == Call.Details.DIRECTION_INCOMING

        if (isIncoming) {
            if (isScreenInteractive()) {
                // Phone is actively being used: don't yank the full-screen
                // call UI over whatever the person is doing. The
                // notification (already shown above) plus a short vibrate
                // is the "someone's calling" signal here. The full-screen
                // intent is still attached to the notification itself, so
                // tapping it opens the call UI immediately if they want it.
                vibrateForIncomingCall()
            } else {
                // Screen is off / device is idle: nobody's looking at it,
                // so a notification alone could go unnoticed. This is
                // exactly the case the full-screen-intent notification
                // (and this direct launch as a foregrounded fast-path) is
                // designed for — wake the screen and show accept/decline.
                launchInCallUi()
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed, remaining=${_allCalls.size - 1}")
        call.unregisterCallback(callCallback)
        _allCalls.remove(call)
        notifyListeners()
        if (_allCalls.isEmpty()) {
            CallNotificationHelper.clear(applicationContext)
        }
        callKey(call)?.let {
            loggedAsMissed.remove(it)
            loggedAsAnswered.remove(it)
        }
    }

    private fun handleStateForNotification(call: Call, state: Int) {
        val number = call.details?.handle?.schemeSpecificPart ?: "Unknown"
        val name = call.details?.callerDisplayName?.takeIf { it.isNotBlank() } ?: number

        when (state) {
            Call.STATE_RINGING -> {
                CallNotificationHelper.showIncomingCallNotification(applicationContext, name, number)
            }
            Call.STATE_ACTIVE, Call.STATE_HOLDING -> {
                CallNotificationHelper.showOngoingCallNotification(applicationContext, name)
            }
            Call.STATE_DISCONNECTED -> {
                if (_allCalls.size <= 1) CallNotificationHelper.clear(applicationContext)
            }
        }
    }

    /**
     * Records a missed call at the SERVICE level rather than relying on
     * InCallActivity's onDecline handler — with the screen-on notification
     * path above, the activity may never open at all for a given ringing
     * call, which meant it never got logged as missed. This runs
     * regardless of whether any UI was ever shown, so "screen was on,
     * notification appeared, call rang out unanswered" still ends up in
     * Recents as a missed call, matching what a stock dialer does.
     */
    private fun handleMissedCallLogging(call: Call) {
        val key = callKey(call) ?: return
        if (key in loggedAsAnswered) return // was actually answered — not missed
        if (key in loggedAsMissed) return // already logged once for this call

        val isIncoming = call.details?.callDirection == Call.Details.DIRECTION_INCOMING
        if (!isIncoming) return // only incoming calls can be "missed"

        loggedAsMissed.add(key)

        val number = call.details?.handle?.schemeSpecificPart ?: "Unknown"
        val name = call.details?.callerDisplayName?.takeIf { it.isNotBlank() }

        val app = applicationContext as? PixelDialerApp ?: return
        serviceScope.launch {
            try {
                app.callLogRepository.logCall(
                    number = number,
                    name = name,
                    direction = CallDirection.MISSED
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log missed call", e)
            }
        }
    }

    private fun callKey(call: Call): String? {
        val number = call.details?.handle?.schemeSpecificPart ?: return null
        // Includes a coarse timestamp so a legitimate second call from the
        // same number shortly after doesn't collide with the key of the
        // previous one still being cleaned up.
        return "$number:${call.details?.creationTimeMillis ?: 0}"
    }

    private fun isScreenInteractive(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isInteractive ?: true
    }

    private fun vibrateForIncomingCall() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1))
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed", e)
        }
    }

    private fun launchInCallUi() {
        val intent = Intent(this, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Direct startActivity failed, relying on full-screen notification", e)
        }
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

    /** Swaps which of the two simultaneous calls is active/on-hold — the standard "swap calls" action. */
    fun swapCalls() {
        val primary = currentCall ?: return
        val secondary = secondaryCall ?: return
        primary.hold()
        secondary.unhold()
    }

    /**
     * Merges the two current calls into a conference — the standard
     * "Merge calls" action found in every stock dialer. Requires the
     * carrier/connection service to support conferencing; if it doesn't,
     * this is a no-op from the Telecom framework's side.
     */
    fun mergeCalls() {
        val primary = currentCall ?: return
        val secondary = secondaryCall ?: return
        primary.conference(secondary)
    }
}
