package com.pixeldialer.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

            var hasPermissions by remember { mutableStateOf(DialerPermissions.hasAll(context)) }
            var isDefaultDialer by remember { mutableStateOf(DialerPermissions.isDefaultDialer(context)) }
            var selectedTab by remember { mutableStateOf(DialerTab.RECENT) }
            var showThemePicker by remember { mutableStateOf(false) }

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

            LaunchedEffect(hasPermissions) {
                if (hasPermissions) viewModel.loadContacts()
            }

            fun placeCall(number: String) {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
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
                            DialerBottomNav(selected = selectedTab, onSelect = { selectedTab = it })
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
                                        onOpenThemePicker = { showThemePicker = true },
                                        onCall = { call: RecentCall -> placeCall(call.phoneNumber) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    DialerTab.CONTACTS -> ContactsScreen(
                                        contacts = contacts,
                                        onCall = { contact -> placeCall(contact.phoneNumber) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    DialerTab.DIALER -> DialerScreen(
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
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
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
