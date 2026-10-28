package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthManager
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.AddEditSecretDialog
import com.example.ui.screens.AuditLogScreen
import com.example.ui.screens.BackendMonitorScreen
import com.example.ui.screens.MasterPinDialog
import com.example.ui.screens.SecurityHubScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SecureVaultViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        MainAppScaffold(currentUser = currentUser!!)
    }
}

enum class NavTab {
    HUB, VAULT, BACKEND, AUDIT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    currentUser: FirebaseUser,
    viewModel: SecureVaultViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(NavTab.HUB) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingUnlockCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    val userMessage by viewModel.userMessage.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()

    // Handle back button when on sub-tabs
    BackHandler(enabled = currentTab != NavTab.HUB) {
        currentTab = NavTab.HUB
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CyberSurfaceElevated,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "SecurePlus",
                                    tint = CyberEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SecurePlus",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "REST API Client • Dynamic Link",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = CyberCyan
                            )
                        }
                    }
                },
                actions = {
                    // Backend Ping / Status Chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CyberSurfaceElevated,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { currentTab = NavTab.BACKEND }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "API Link Active",
                                tint = CyberEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "API • ${telemetry.pingMs}ms",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CyberEmerald
                            )
                        }
                    }

                    // Master Vault Lock / Unlock Toggle
                    IconButton(
                        onClick = {
                            if (isVaultUnlocked) {
                                viewModel.lockVault()
                            } else {
                                showPinDialog = true
                            }
                        },
                        modifier = Modifier.testTag("appbar_lock_toggle")
                    ) {
                        Icon(
                            imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = if (isVaultUnlocked) "Vault Unlocked (Tap to Lock)" else "Vault Locked (Tap to Unlock)",
                            tint = if (isVaultUnlocked) CyberEmerald else CyberCyan
                        )
                    }

                    // Sign Out button
                    IconButton(
                        onClick = {
                            AuthManager.signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {},
                                scope = scope
                            )
                        },
                        modifier = Modifier.testTag("appbar_signout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberBgDark,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurfaceDark,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = currentTab == NavTab.HUB,
                    onClick = { currentTab = NavTab.HUB },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Security Hub") },
                    label = { Text("Hub") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberEmerald,
                        selectedTextColor = CyberEmerald,
                        indicatorColor = CyberSurfaceElevated,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_hub")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.VAULT,
                    onClick = { currentTab = NavTab.VAULT },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Cloud Vault") },
                    label = { Text("Vault") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberEmerald,
                        selectedTextColor = CyberEmerald,
                        indicatorColor = CyberSurfaceElevated,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_vault")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.BACKEND,
                    onClick = { currentTab = NavTab.BACKEND },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = "Backend Monitor") },
                    label = { Text("Backend") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberSurfaceElevated,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_backend")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.AUDIT,
                    onClick = { currentTab = NavTab.AUDIT },
                    icon = { Icon(Icons.Default.History, contentDescription = "Audit Trail") },
                    label = { Text("Audit") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberSurfaceElevated,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_audit")
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = CyberBgDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.HUB -> SecurityHubScreen(
                    viewModel = viewModel,
                    onNavigateToVault = { currentTab = NavTab.VAULT },
                    onNavigateToBackend = { currentTab = NavTab.BACKEND },
                    onNavigateToAudit = { currentTab = NavTab.AUDIT },
                    onAddNewSecret = { showAddDialog = true }
                )
                NavTab.VAULT -> VaultScreen(
                    viewModel = viewModel,
                    onAddNewSecret = { showAddDialog = true },
                    onRequestUnlock = { onUnlocked ->
                        pendingUnlockCallback = onUnlocked
                        showPinDialog = true
                    }
                )
                NavTab.BACKEND -> BackendMonitorScreen(viewModel = viewModel)
                NavTab.AUDIT -> AuditLogScreen(viewModel = viewModel)
            }
        }
    }

    if (showAddDialog) {
        AddEditSecretDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }

    if (showPinDialog) {
        MasterPinDialog(
            viewModel = viewModel,
            onDismiss = {
                showPinDialog = false
                pendingUnlockCallback = null
            },
            onSuccess = {
                pendingUnlockCallback?.invoke()
                pendingUnlockCallback = null
            }
        )
    }
}
