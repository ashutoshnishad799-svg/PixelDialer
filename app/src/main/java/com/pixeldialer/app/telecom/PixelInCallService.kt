package com.pixeldialer.app.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

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
    }

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
            if (state == Call.STATE_DISCONNECTED) {
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

        // Fire the notification FIRST — it's the guaranteed-to-work path
        // (full-screen intent reliably wakes the screen even when this
        // service is backgrounded). The direct startActivity() below is
        // a same-process fast-path that works when we're already
        // foregrounded, layered on top rather than relied on alone.
        // Both are wrapped so a rendering/launch failure here can never
        // bring down the whole service (and with it, calling ability).
        try {
            handleStateForNotification(call, call.state)
        } catch (e: Exception) {
            Log.e(TAG, "Notification handling failed for new call", e)
        }
        launchInCallUi()
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

    private fun launchInCallUi() {
        val intent = Intent(this, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Expected to fail in some background states — the full-screen
            // notification above is the reliable path in those cases.
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
     * this is a no-op from the Telecom framework's side (nothing to catch
     * here — Call.conference() itself doesn't throw, it just won't merge).
     */
    fun mergeCalls() {
        val primary = currentCall ?: return
        val secondary = secondaryCall ?: return
        primary.conference(secondary)
    }
}
