package com.pixeldialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pixeldialer.app.data.SignedInUser
import com.pixeldialer.app.ui.theme.LocalDialerPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BackupState { IDLE, IN_PROGRESS, SUCCESS, FAILED }

@Composable
fun AccountScreen(
    user: SignedInUser?,
    cloudBackupEnabled: Boolean,
    lastBackedUpAtMillis: Long,
    backupState: BackupState,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onToggleCloudBackup: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDialerPalette.current

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Text("Account", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp)) {
            if (user == null) {
                SignedOutContent(onSignIn)
            } else {
                SignedInContent(
                    user = user,
                    cloudBackupEnabled = cloudBackupEnabled,
                    lastBackedUpAtMillis = lastBackedUpAtMillis,
                    backupState = backupState,
                    onSignOut = onSignOut,
                    onToggleCloudBackup = onToggleCloudBackup,
                    onBackupNow = onBackupNow,
                    onDeleteAccount = onDeleteAccount
                )
            }
        }
    }
}

@Composable
private fun SignedOutContent(onSignIn: () -> Unit) {
    val palette = LocalDialerPalette.current

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(88.dp).clip(CircleShape).background(palette.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = palette.accent, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Sign in to back up your data", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your call log, blocked numbers, and settings sync to your account so you can restore them on a new device.",
            fontSize = 13.5.sp, color = palette.textSecondary, textAlign = TextAlign.Center, lineHeight = 19.sp
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
        ) {
            Text("Sign in with Google", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SignedInContent(
    user: SignedInUser,
    cloudBackupEnabled: Boolean,
    lastBackedUpAtMillis: Long,
    backupState: BackupState,
    onSignOut: () -> Unit,
    onToggleCloudBackup: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val palette = LocalDialerPalette.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!user.photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = user.photoUrl, contentDescription = null,
                modifier = Modifier.size(52.dp).clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(palette.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = palette.accent)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.displayName ?: "Signed in", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
            if (!user.email.isNullOrBlank()) {
                Text(user.email, fontSize = 13.sp, color = palette.textSecondary)
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (cloudBackupEnabled) Icons.Filled.CloudDone else Icons.Filled.CloudUpload,
            contentDescription = null, tint = palette.accent
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Cloud Backup", fontWeight = FontWeight.SemiBold, color = palette.textPrimary, fontSize = 15.sp)
            Text(
                text = if (lastBackedUpAtMillis > 0) "Last backed up: ${formatBackupTime(lastBackedUpAtMillis)}" else "Not backed up yet",
                fontSize = 12.sp, color = palette.textSecondary
            )
        }
        Switch(checked = cloudBackupEnabled, onCheckedChange = onToggleCloudBackup)
    }

    if (cloudBackupEnabled) {
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBackupNow,
            enabled = backupState != BackupState.IN_PROGRESS,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                when (backupState) {
                    BackupState.IN_PROGRESS -> "Backing up…"
                    BackupState.SUCCESS -> "Backed up ✓"
                    BackupState.FAILED -> "Failed — tap to retry"
                    BackupState.IDLE -> "Back up now"
                }
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
    ) {
        Text(
            "Sign out",
            color = palette.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().clickable { onSignOut() }.padding(16.dp)
        )
        HorizontalDivider(color = palette.cardBorder, thickness = 1.dp)
        Text(
            "Delete account & cloud data",
            color = palette.danger,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().clickable { showDeleteConfirm = true }.padding(16.dp)
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete account?") },
            text = { Text("This permanently deletes your cloud backup and signs you out. Your data stays on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteAccount()
                }) { Text("Delete", color = palette.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

private fun formatBackupTime(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}
