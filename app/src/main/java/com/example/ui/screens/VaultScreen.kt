package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DecryptedVaultSecret
import com.example.data.model.VaultItem
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.viewmodel.SecureVaultViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun VaultScreen(
    viewModel: SecureVaultViewModel,
    onAddNewSecret: () -> Unit,
    onRequestUnlock: (onUnlocked: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val vaultItemsState by viewModel.vaultItemsState.collectAsState()
    val decryptedSecrets by viewModel.decryptedSecrets.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val masterPin by viewModel.masterPin.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val categories = listOf("ALL", "CREDENTIAL", "API_KEY", "SECURE_NOTE", "FINANCIAL", "RECOVERY")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CyberCyan
                    )
                },
                placeholder = { Text("Search encrypted vault...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CyberSurfaceDark,
                    unfocusedContainerColor = CyberSurfaceDark,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vault_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = when (category) {
                                    "ALL" -> "All Secrets"
                                    "CREDENTIAL" -> "Logins"
                                    "API_KEY" -> "API Keys"
                                    "SECURE_NOTE" -> "Notes"
                                    "FINANCIAL" -> "Cards"
                                    "RECOVERY" -> "Recovery"
                                    else -> category
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberEmerald,
                            selectedLabelColor = CyberBgDark,
                            containerColor = CyberSurfaceElevated,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vault Items List
            when (val state = vaultItemsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CyberEmerald)
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${state.message}",
                            color = CyberCrimson,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is UiState.Success -> {
                    val filteredItems = state.data.filter { item ->
                        val matchesCategory = selectedCategory == "ALL" || item.category == selectedCategory
                        val matchesSearch = searchQuery.isBlank() ||
                                item.title.contains(searchQuery, ignoreCase = true) ||
                                (item.notesPreview?.contains(searchQuery, ignoreCase = true) == true)
                        matchesCategory && matchesSearch
                    }

                    if (filteredItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = CyberBorder,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank() || selectedCategory != "ALL")
                                        "No secrets match your filter"
                                    else "Vault is empty",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Secrets are AES-256 encrypted before cloud sync",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                VaultItemCard(
                                    item = item,
                                    decryptedSecret = decryptedSecrets[item.id],
                                    onDecryptClicked = {
                                        if (isVaultUnlocked) {
                                            viewModel.decryptItem(item, masterPin)
                                        } else {
                                            onRequestUnlock {
                                                viewModel.decryptItem(item, masterPin)
                                            }
                                        }
                                    },
                                    onHideClicked = {
                                        viewModel.hideDecryptedSecret(item.id)
                                    },
                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(item.id, item.favorite)
                                    },
                                    onDeleteClicked = {
                                        viewModel.deleteVaultItem(item.id, item.title)
                                    },
                                    onCopySecret = { secretText ->
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Vault Secret", secretText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied to clipboard (auto-clears soon)", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to add secret
        FloatingActionButton(
            onClick = onAddNewSecret,
            containerColor = CyberEmerald,
            contentColor = CyberBgDark,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("vault_fab_add")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Secret")
        }
    }
}

@Composable
fun VaultItemCard(
    item: VaultItem,
    decryptedSecret: DecryptedVaultSecret?,
    onDecryptClicked: () -> Unit,
    onHideClicked: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteClicked: () -> Unit,
    onCopySecret: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CyberSurfaceElevated,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = categoryIcon(item.category),
                            contentDescription = null,
                            tint = CyberEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CyberSurfaceElevated,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AES-256-GCM",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = CyberEmerald
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (item.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.favorite) CyberCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClicked,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secret Content Box (Encrypted vs Decrypted)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CyberSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (decryptedSecret == null) {
                    // Encrypted Preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ENCRYPTED PAYLOAD (CIPHERTEXT)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "••••••••••••••••••••••••",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = CyberEmerald
                            )
                        }

                        IconButton(
                            onClick = onDecryptClicked,
                            modifier = Modifier.testTag("decrypt_button_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Decrypt Secret",
                                tint = CyberCyan
                            )
                        }
                    }
                } else {
                    // Decrypted Secret Display
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DECRYPTED SECRET",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = CyberEmerald
                            )
                            IconButton(
                                onClick = onHideClicked,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Hide Secret",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (decryptedSecret.accountUsername.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Username: ${decryptedSecret.accountUsername}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = decryptedSecret.secretPasswordOrKey,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = CyberEmerald,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onCopySecret(decryptedSecret.secretPasswordOrKey) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("copy_secret_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Secret",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (decryptedSecret.websiteOrService.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "URL/Service: ${decryptedSecret.websiteOrService}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (decryptedSecret.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Notes: ${decryptedSecret.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun categoryIcon(category: String): ImageVector {
    return when (category) {
        "CREDENTIAL" -> Icons.Default.Key
        "API_KEY" -> Icons.Default.Code
        "SECURE_NOTE" -> Icons.Default.Description
        "FINANCIAL" -> Icons.Default.CreditCard
        "RECOVERY" -> Icons.Default.VpnKey
        else -> Icons.Default.Lock
    }
}
