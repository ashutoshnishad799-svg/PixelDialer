package com.pixeldialer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pixeldialer.app.ui.theme.DialerPalette
import com.pixeldialer.app.ui.theme.LocalDialerPalette

data class NewContactInput(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val phoneLabel: String,
    val email: String
)

private val phoneLabels = listOf("Mobile", "Home", "Work", "Other")

/**
 * Add-contact dialog. Prefills the phone number when opened from the
 * dialer with a dialed-but-unsaved number, otherwise opens blank from the
 * Contacts tab's + button.
 */
@Composable
fun AddContactDialog(
    prefillNumber: String = "",
    onDismiss: () -> Unit,
    onSave: (NewContactInput) -> Unit
) {
    val palette = LocalDialerPalette.current
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf(prefillNumber) }
    var phoneLabel by remember { mutableStateOf("Mobile") }
    var email by remember { mutableStateOf("") }

    val canSave = firstName.isNotBlank() && phoneNumber.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.cardBackground)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New Contact", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            LabeledField(label = "First name", value = firstName, onChange = { firstName = it }, palette = palette)
            Spacer(Modifier.height(10.dp))
            LabeledField(label = "Last name", value = lastName, onChange = { lastName = it }, palette = palette)
            Spacer(Modifier.height(10.dp))
            LabeledField(
                label = "Phone number", value = phoneNumber, onChange = { phoneNumber = it },
                palette = palette, keyboardType = KeyboardType.Phone
            )

            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(phoneLabels) { label ->
                    val selected = phoneLabel == label
                    Surface(
                        onClick = { phoneLabel = label },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) palette.accent else palette.searchBackground,
                        contentColor = if (selected) Color.White else palette.textSecondary
                    ) {
                        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            LabeledField(
                label = "Email (optional)", value = email, onChange = { email = it },
                palette = palette, keyboardType = KeyboardType.Email
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    onSave(NewContactInput(firstName.trim(), lastName.trim(), phoneNumber.trim(), phoneLabel, email.trim()))
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
            ) {
                Text("Save Contact", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    palette: DialerPalette,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(label, fontSize = 12.sp, color = palette.textSecondary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.searchBackground)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = palette.textPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
