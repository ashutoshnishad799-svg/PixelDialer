package com.pixeldialer.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.pixeldialer.app.data.RecentCall
import com.pixeldialer.app.telecom.DialerPermissions
import com.pixeldialer.app.ui.components.DialerBottomNav
import com.pixeldialer.app.ui.components.DialerTab
import com.pixeldialer.app.ui.components.ThemePickerSheet
import com.pixeldialer.app.ui.screens.*
import com.pixeldialer.app.ui.theme.LocalDialerPalette
import com.pixeldialer.app.ui.theme.PixelDialerTheme
import com.pixeldialer.app.viewmodel.MainViewModel
import com.pixeldialer.app.viewmodel.ViewModelFactory

private enum class OverlayScreen { NONE, ACCOUNT, PRIVACY_POLICY }

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        ViewModelFactory(application as PixelDialerApp)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val themeId by viewModel.themeId.collectAsState()
            val recents by viewModel.recents.collectAsState()
            val contacts by viewModel.contacts.collectAsState()
            val currentUser by viewModel.currentUser.collectAsState()
            val settings by viewModel.settings.collectAsState()
            val backupState by viewModel.backupState.collectAsState()
            val lastBackedUpAt by viewModel.lastBackedUpAtMillis.collectAsState()

            var hasPermissions by remember { mutableStateOf(DialerPermissions.hasAll(context)) }
            var isDefaultDialer by remember { mutableStateOf(DialerPermissions.isDefaultDialer(context)) }
            var selectedTab by remember { mutableStateOf(DialerTab.RECENT) }
            var showThemePicker by remember { mutableStateOf(false) }
            var overlay by remember { mutableStateOf(OverlayScreen.NONE) }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                hasPermissions = results.values.all { it }
                if (hasPermissions) viewModel.loadContacts()
            }

            val defaultDialerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                isDefaultDialer = DialerPermissions.isDefaultDialer(context)
            }

            val signInLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                viewModel.handleSignInResult(result.data) { success, errorMessage ->
                    if (!success) {
                        Toast.makeText(context, errorMessage ?: "Sign-in failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            LaunchedEffect(hasPermissions) {
                if (hasPermissions) {
                    viewModel.loadContacts()
                    viewModel.syncCallHistory()
                }
            }

            fun placeCall(number: String) {
                DialerPermissions.placeCall(context, number)
            }

            fun sendMessage(number: String) {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }

            PixelDialerTheme(themeId = themeId) {
                val palette = LocalDialerPalette.current

                if (!hasPermissions || !isDefaultDialer) {
                    Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
                        PermissionsScreen(
                            isDefaultDialer = isDefaultDialer,
                            hasPermissions = hasPermissions,
                            onRequestPermissions = {
                                permissionLauncher.launch(DialerPermissions.required)
                            },
                            onSetDefaultDialer = {
                                defaultDialerLauncher.launch(
                                    DialerPermissions.requestDefaultDialerIntent(context)
                                )
                            }
                        )
                    }
                } else {
                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            if (overlay == OverlayScreen.NONE) {
                                DialerBottomNav(selected = selectedTab, onSelect = { selectedTab = it })
                            }
                        }
                    ) { padding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(palette.background)
                                .padding(padding)
                        ) {
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                                label = "tab-content"
                            ) { tab ->
                                when (tab) {
                                    DialerTab.RECENT -> RecentsScreen(
                                        recents = recents,
                                        currentThemeId = themeId,
                                        onOpenThemePicker = { showThemePicker = true },
                                        onCall = { call: RecentCall -> placeCall(call.phoneNumber) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    DialerTab.CONTACTS -> ContactsScreen(
                                        contacts = contacts,
                                        onCall = { contact -> placeCall(contact.phoneNumber) },
                                        onMessage = { contact -> sendMessage(contact.phoneNumber) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    DialerTab.DIALER -> DialerScreen(
                                        contacts = contacts,
                                        onCall = { number -> placeCall(number) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    DialerTab.PROTECT -> ProtectScreen(
                                        blockedNumbers = emptyList(),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    DialerTab.MORE -> MoreScreen(
                                        onItemClick = { item ->
                                            when (item) {
                                                "Set as default dialer" -> defaultDialerLauncher.launch(
                                                    DialerPermissions.requestDefaultDialerIntent(context)
                                                )
                                                "Appearance" -> showThemePicker = true
                                                "Account" -> overlay = OverlayScreen.ACCOUNT
                                                "Privacy Policy" -> overlay = OverlayScreen.PRIVACY_POLICY
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            // Full-screen overlays (Account, Privacy Policy) slide in over the
                            // tab content. AnimatedVisibility (not a plain `if`) is used here
                            // specifically so the slide-out plays fully before the overlay is
                            // actually removed from composition — a bare `if` would yank it
                            // out instantly on close, skipping the exit transition.
                            //
                            // `renderedOverlay` intentionally lags one step behind `overlay`:
                            // when overlay flips to NONE to trigger the close animation, this
                            // keeps rendering whatever screen was showing (rather than blanking
                            // to nothing) for the duration of the slide-out.
                            var renderedOverlay by remember { mutableStateOf(overlay) }
                            if (overlay != OverlayScreen.NONE) renderedOverlay = overlay

                            AnimatedVisibility(
                                visible = overlay != OverlayScreen.NONE,
                                enter = slideInHorizontally { it } + fadeIn(),
                                exit = slideOutHorizontally { it } + fadeOut()
                            ) {
                                Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
                                    when (renderedOverlay) {
                                        OverlayScreen.ACCOUNT -> AccountScreen(
                                            user = currentUser,
                                            cloudBackupEnabled = settings.cloudBackupEnabled,
                                            lastBackedUpAtMillis = lastBackedUpAt,
                                            backupState = backupState,
                                            onBack = { overlay = OverlayScreen.NONE },
                                            onSignIn = {
                                                val intent = viewModel.signInIntent()
                                                if (intent != null) {
                                                    signInLauncher.launch(intent)
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Sign-in isn't set up yet",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            onSignOut = { viewModel.signOut() },
                                            onToggleCloudBackup = { enabled -> viewModel.setCloudBackupEnabled(enabled) },
                                            onBackupNow = { viewModel.backupNow() },
                                            onDeleteAccount = {
                                                viewModel.deleteAccount { success ->
                                                    val msg = if (success) "Account deleted" else "Couldn't delete account"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    if (success) overlay = OverlayScreen.NONE
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        OverlayScreen.PRIVACY_POLICY -> PrivacyPolicyScreen(
                                            onBack = { overlay = OverlayScreen.NONE },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        OverlayScreen.NONE -> {}
                                    }
                                }
                            }
                        }
                    }

                    if (showThemePicker) {
                        ThemePickerSheet(
                            currentThemeId = themeId,
                            onSelect = { id -> viewModel.setTheme(id) },
                            onDismiss = { showThemePicker = false }
                        )
                    }
                }
            }
        }
    }
}
