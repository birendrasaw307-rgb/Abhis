package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DecryptedVaultSecret
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.viewmodel.SecureVaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSecretDialog(
    viewModel: SecureVaultViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("CREDENTIAL") }
    var username by remember { mutableStateOf("") }
    var secretValue by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categories = listOf("CREDENTIAL", "API_KEY", "SECURE_NOTE", "FINANCIAL", "RECOVERY")
    val strength = remember(secretValue) { viewModel.evaluatePassword(secretValue) }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CyberSurfaceDark,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberSurfaceElevated,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = CyberEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "New Encrypted Secret",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "AES-256-GCM Zero-Knowledge",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberEmerald
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Label / Title (e.g. AWS Production Key)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CyberSurfaceElevated,
                        unfocusedContainerColor = CyberSurfaceElevated,
                        focusedBorderColor = CyberEmerald,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("secret_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (category) {
                            "CREDENTIAL" -> "Login / Account"
                            "API_KEY" -> "API Key / Token"
                            "SECURE_NOTE" -> "Encrypted Note"
                            "FINANCIAL" -> "Credit Card / Bank"
                            "RECOVERY" -> "Recovery Passphrase"
                            else -> category
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CyberSurfaceElevated,
                            unfocusedContainerColor = CyberSurfaceElevated,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(CyberSurfaceDark)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Username / Account
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username / Account ID (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CyberSurfaceElevated,
                        unfocusedContainerColor = CyberSurfaceElevated,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("secret_username_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Secret / Password input
                OutlinedTextField(
                    value = secretValue,
                    onValueChange = { secretValue = it },
                    label = { Text("Secret Value / Password *") },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Secret Visibility",
                                tint = CyberCyan
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CyberSurfaceElevated,
                        unfocusedContainerColor = CyberSurfaceElevated,
                        focusedBorderColor = CyberEmerald,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("secret_value_input")
                )

                // Password Strength Indicator
                if (secretValue.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val progressColor = when (strength.rating) {
                        "ENTERPRISE" -> CyberEmerald
                        "STRONG" -> CyberEmerald
                        "GOOD" -> CyberCyan
                        "FAIR" -> CyberAmber
                        else -> CyberCrimson
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Strength: ${strength.rating} (${strength.score}/100)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = progressColor
                        )
                        Text(
                            text = "${strength.entropyBits.toInt()} bits entropy",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { strength.score / 100f },
                        color = progressColor,
                        trackColor = CyberSurfaceElevated,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Generator button
                OutlinedButton(
                    onClick = {
                        secretValue = viewModel.generatePassword(22)
                        isPasswordVisible = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_password_button")
                ) {
                    Icon(imageVector = Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate 22-char High-Entropy Secret", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // URL / Service
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Service URL / Host (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CyberSurfaceElevated,
                        unfocusedContainerColor = CyberSurfaceElevated,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Confidential Notes (optional)") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CyberSurfaceElevated,
                        unfocusedContainerColor = CyberSurfaceElevated,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = CyberCrimson,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save button
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            errorMessage = "Please enter a title"
                            return@Button
                        }
                        if (secretValue.isBlank()) {
                            errorMessage = "Secret value cannot be empty"
                            return@Button
                        }

                        isSaving = true
                        errorMessage = null
                        val secretPayload = DecryptedVaultSecret(
                            accountUsername = username.trim(),
                            secretPasswordOrKey = secretValue,
                            websiteOrService = website.trim(),
                            notes = notes.trim()
                        )

                        val securityLevel = if (strength.score >= 80) "MAXIMUM"
                        else if (strength.score >= 50) "HIGH"
                        else "STANDARD"

                        viewModel.saveVaultItem(
                            title = title.trim(),
                            category = category,
                            securityLevel = securityLevel,
                            secret = secretPayload,
                            notesPreview = if (notes.isNotBlank()) notes.take(50) else null,
                            onSuccess = {
                                isSaving = false
                                onDismiss()
                            },
                            onError = { err ->
                                isSaving = false
                                errorMessage = err
                            }
                        )
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberEmerald,
                        contentColor = CyberBgDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_secret_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CyberBgDark, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Encrypting & Syncing...")
                    } else {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Encrypt & Save to Cloud", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
