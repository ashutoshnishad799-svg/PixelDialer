package com.pixeldialer.app.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import android.Manifest

object DialerPermissions {

    val required: Array<String> = buildList {
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.READ_CALL_LOG)
        add(Manifest.permission.WRITE_CALL_LOG)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.ANSWER_PHONE_CALLS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    fun hasAll(context: Context): Boolean =
        required.all {
            ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

    fun isDefaultDialer(context: Context): Boolean {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return telecomManager.defaultDialerPackage == context.packageName
    }

    /** Launches the system flow to request default-dialer role for this app. */
    fun requestDefaultDialerIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        }
    }

    /**
     * Places a call the *correct* way for a default dialer app.
     *
     * If this app currently holds the default-dialer role, calls go straight
     * through TelecomManager.placeCall() — which routes directly into our
     * own PixelInCallService without asking the system "which app should
     * handle this?" That system chooser (the "Complete action using..."
     * dialog with Just once/Always) only appears when a plain ACTION_CALL
     * intent is broadcast instead — which is what forced it to show up
     * even though we were already the default dialer.
     *
     * If we're NOT the default dialer (shouldn't normally happen since the
     * UI blocks access until the role is granted), we fall back to
     * ACTION_CALL so the call can still go through via whatever is default.
     */
    fun placeCall(context: Context, number: String) {
        if (isDefaultDialer(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecomManager.placeCall(Uri.fromParts("tel", number, null), Bundle())
        } else {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
