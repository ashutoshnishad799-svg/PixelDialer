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
import kotlinx.coroutines.launch

private enum class OverlayScreen { NONE, ACCOUNT, PRIVACY_POLICY, SETTINGS, BLOCKED_NUMBERS, HELP_FEEDBACK }

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
            val blockedNumbers by viewModel.blockedNumbers.collectAsState()

            var hasPermissions by remember { mutableStateOf(DialerPermissions.hasAll(context)) }
            var isDefaultDialer by remember { mutableStateOf(DialerPermissions.isDefaultDialer(context)) }
            val app = context.applicationContext as PixelDialerApp
            val onboardingComplete by app.onboardingPreference.isCompleteFlow.collectAsState(initial = null)
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            var selectedTab by remember { mutableStateOf(DialerTab.RECENT) }
            var showThemePicker by remember { mutableStateOf(false) }
            var showAddContactFromDialer by remember { mutableStateOf<String?>(null) }
            var overlay by remember { mutableStateOf(OverlayScreen.NONE) }

            // Without this, Android's system back gesture/button has no
            // idea an overlay (Settings/Account/etc) is "open" — it just
            // sees the Activity and finishes it, which is what made the
            // whole app close instead of only the overlay. BackHandler
            // intercepts system back specifically while an overlay is
            // showing and routes it through the same close logic the
            // in-app back arrow already used.
            androidx.activity.compose.BackHandler(enabled = overlay != OverlayScreen.NONE) {
                overlay = OverlayScreen.NONE
            }

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

                if (onboardingComplete == false) {
                    OnboardingScreen(
                        onFinished = { scope.launch { app.onboardingPreference.markComplete() } },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!hasPermissions || !isDefaultDialer) {
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
                                transitionSpec = {
                                    val forward = targetState.ordinal > initialState.ordinal
                                    // Smaller slide distance + shorter duration than the
                                    // original: the Dialer tab's 12-key grid (each key
                                    // tracking its own press-scale animateFloatAsState)
                                    // composing at the same moment as a larger slide
                                    // distance was heavy enough on slower devices to read
                                    // as the tab-switch animation stalling/jerking.
                                    val slideDistance = { fullWidth: Int -> fullWidth / 10 }
                                    (fadeIn(tween(160)) + slideInHorizontally(
                                        animationSpec = tween(160),
                                        initialOffsetX = { w -> if (forward) slideDistance(w) else -slideDistance(w) }
                                    )) togetherWith
                                        (fadeOut(tween(110)) + slideOutHorizontally(
                                            animationSpec = tween(110),
                                            targetOffsetX = { w -> if (forward) -slideDistance(w) else slideDistance(w) }
                                        ))
                                },
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
                                        onSaveNewContact = { input -> viewModel.saveNewContact(input) { } },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    DialerTab.DIALER -> DialerScreen(
                                        contacts = contacts,
                                        onCall = { number -> placeCall(number) },
                                        onAddContact = { number -> showAddContactFromDialer = number },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    DialerTab.PROTECT -> ProtectScreen(
                                        blockedNumbers = blockedNumbers,
                                        onOpenBlockedNumbers = { overlay = OverlayScreen.BLOCKED_NUMBERS },
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
                                                "Settings" -> overlay = OverlayScreen.SETTINGS
                                                "Blocked numbers" -> overlay = OverlayScreen.BLOCKED_NUMBERS
                                                "Help & feedback" -> overlay = OverlayScreen.HELP_FEEDBACK
                                                "Voicemail" -> DialerPermissions.callVoicemail(context)
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
                                        OverlayScreen.SETTINGS -> SettingsScreen(
                                            settings = settings,
                                            onBack = { overlay = OverlayScreen.NONE },
                                            onOpenAppearance = { showThemePicker = true },
                                            onToggleCallRecording = { enabled -> viewModel.setCallRecordingEnabled(enabled) },
                                            onToggleAutoRecordAll = { enabled -> viewModel.setAutoRecordAll(enabled) },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        OverlayScreen.BLOCKED_NUMBERS -> BlockedNumbersScreen(
                                            blockedNumbers = blockedNumbers,
                                            onBack = { overlay = OverlayScreen.NONE },
                                            onBlock = { number -> viewModel.blockNumber(number) },
                                            onUnblock = { entry -> viewModel.unblockNumber(entry) },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        OverlayScreen.HELP_FEEDBACK -> HelpFeedbackScreen(
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

                    // The "Add to Contacts" chip on the Dialer tab previously
                    // called an onAddContact that was never wired to
                    // anything at this level — it silently did nothing,
                    // which is why the dialog appeared to never open.
                    showAddContactFromDialer?.let { prefillNumber ->
                        com.pixeldialer.app.ui.components.AddContactDialog(
                            prefillNumber = prefillNumber,
                            onDismiss = { showAddContactFromDialer = null },
                            onSave = { input ->
                                viewModel.saveNewContact(input) { showAddContactFromDialer = null }
                            }
                        )
                    }
                }
            }
        }
    }
}
