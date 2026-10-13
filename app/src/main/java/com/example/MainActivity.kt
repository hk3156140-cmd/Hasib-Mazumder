package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.model.UserRole
import com.example.data.repository.AuthAndUserRepository
import com.example.data.repository.BusinessRepository
import com.example.data.repository.ProductAndCategoryRepository
import com.example.data.repository.SaleAndOrderRepository
import com.example.ui.AuthUiState
import com.example.ui.AuthViewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CustomersSuppliersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpenseLedgerScreen
import com.example.ui.screens.OnlineOrdersScreen
import com.example.ui.screens.OnlineShopScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ProductManagementScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StaffPermissionsScreen
import com.example.ui.theme.IlamBlack
import com.example.ui.theme.IlamDarkCard
import com.example.ui.theme.IlamGold
import com.example.ui.theme.IlamGoldLight
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage

class MainActivity : ComponentActivity() {

    private val authUserRepo by lazy { AuthAndUserRepository(applicationContext) }
    private val productRepo by lazy { ProductAndCategoryRepository(applicationContext) }
    private val saleOrderRepo by lazy { SaleAndOrderRepository(applicationContext) }
    private val businessRepo by lazy { BusinessRepository(applicationContext) }

    private val authViewModel: AuthViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(authUserRepo) as T
            }
        }
    }

    private val mainViewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(productRepo, saleOrderRepo, businessRepo, authUserRepo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentLanguage by remember { mutableStateOf(AppLanguage.BANGLA) }

                CompositionLocalProvider(LocalAppLanguage provides currentLanguage) {
                    IlamStoreApp(
                        authViewModel = authViewModel,
                        mainViewModel = mainViewModel,
                        currentLanguage = currentLanguage,
                        onToggleLanguage = {
                            currentLanguage = if (currentLanguage == AppLanguage.BANGLA) {
                                AppLanguage.ENGLISH
                            } else {
                                AppLanguage.BANGLA
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun IlamStoreApp(
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel,
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsState()
    val isGuestMode by authViewModel.isGuestMode.collectAsState()
    val userMessage by mainViewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Toast/Snackbar for system messages
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearUserMessage()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = IlamBlack
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = IlamBlack
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    authState is AuthUiState.Loading -> {
                        LuxurySplashScreen()
                    }

                    isGuestMode -> {
                        // Guest mode: customer online store browsing
                        OnlineShopScreen(
                            userProfile = null,
                            mainViewModel = mainViewModel,
                            onBack = { authViewModel.exitGuestCustomerMode() }
                        )
                    }

                    authState is AuthUiState.Authenticated -> {
                        val profile = (authState as AuthUiState.Authenticated).userProfile
                        MainAppContent(
                            userProfile = profile,
                            authViewModel = authViewModel,
                            mainViewModel = mainViewModel,
                            onToggleLanguage = onToggleLanguage
                        )
                    }

                    else -> {
                        // Unauthenticated or Error: show Sign-In
                        AuthScreen(
                            authViewModel = authViewModel,
                            onLanguageToggle = onToggleLanguage
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    userProfile: com.example.data.model.UserProfile,
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel,
    onToggleLanguage: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val credentialManager = remember { androidx.credentials.CredentialManager.create(context) }
    val isCustomer = userProfile.role == UserRole.CUSTOMER.name
    val defaultScreen = if (isCustomer) "shop" else "dashboard"

    var currentScreen by remember { mutableStateOf(defaultScreen) }
    var screenStack by remember { mutableStateOf(listOf(defaultScreen)) }

    fun navigateTo(screen: String) {
        if (currentScreen != screen) {
            screenStack = screenStack + screen
            currentScreen = screen
        }
    }

    fun navigateBack() {
        if (screenStack.size > 1) {
            val newStack = screenStack.dropLast(1)
            screenStack = newStack
            currentScreen = newStack.last()
        } else if (currentScreen != defaultScreen) {
            currentScreen = defaultScreen
            screenStack = listOf(defaultScreen)
        }
    }

    // Hardware and gesture Back button handling
    BackHandler(enabled = screenStack.size > 1 || currentScreen != defaultScreen) {
        navigateBack()
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            "dashboard" -> {
                DashboardScreen(
                    userProfile = userProfile,
                    mainViewModel = mainViewModel,
                    onNavigate = { route -> navigateTo(route) },
                    onSignOut = { authViewModel.signOut(context, credentialManager) },
                    onLanguageToggle = onToggleLanguage
                )
            }

            "pos" -> {
                PosScreen(
                    userProfile = userProfile,
                    mainViewModel = mainViewModel,
                    onBack = { navigateBack() }
                )
            }

            "scan" -> {
                ScannerScreen(
                    userProfile = userProfile,
                    mainViewModel = mainViewModel,
                    onBack = { navigateBack() },
                    onNavigateToPos = {
                        navigateTo("pos")
                    }
                )
            }

            "products" -> {
                ProductManagementScreen(
                    userProfile = userProfile,
                    mainViewModel = mainViewModel,
                    onBack = { navigateBack() }
                )
            }

            "orders" -> {
                OnlineOrdersScreen(
                    userProfile = userProfile,
                    mainViewModel = mainViewModel,
                    onBack = { navigateBack() }
                )
            }

            "shop" -> {
                OnlineShopScreen(
                    userProfile = userProfile,
                    mainViewModel = mainViewModel,
                    onBack = {
                        if (isCustomer) {
                            authViewModel.signOut(context, credentialManager)
                        } else {
                            navigateBack()
                        }
                    }
                )
            }

            "customers" -> {
                CustomersSuppliersScreen(
                    userProfile = userProfile,
                    mainViewModel = mainViewModel,
                    onBack = { navigateBack() }
                )
            }

            "expenses" -> {
                ExpenseLedgerScreen(
                    userProfile = userProfile,
                    mainViewModel = mainViewModel,
                    onBack = { navigateBack() }
                )
            }

            "reports" -> {
                ReportsScreen(
                    userProfile = userProfile,
                    mainViewModel = mainViewModel,
                    onBack = { navigateBack() }
                )
            }

            "staff_permissions" -> {
                StaffPermissionsScreen(
                    userProfile = userProfile,
                    mainViewModel = mainViewModel,
                    onBack = { navigateBack() }
                )
            }

            "settings" -> {
                SettingsScreen(
                    userProfile = userProfile,
                    mainViewModel = mainViewModel,
                    onBack = { navigateBack() },
                    onSignOut = { authViewModel.signOut(context, credentialManager) },
                    onLanguageToggle = onToggleLanguage
                )
            }

            else -> {
                DashboardScreen(
                    userProfile = userProfile,
                    mainViewModel = mainViewModel,
                    onNavigate = { route -> navigateTo(route) },
                    onSignOut = { authViewModel.signOut(context, credentialManager) },
                    onLanguageToggle = onToggleLanguage
                )
            }
        }
    }
}

@Composable
fun LuxurySplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(IlamBlack, Color(0xFF181818), IlamBlack)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(IlamDarkCard),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_ilam_logo),
                    contentDescription = "ILAM Logo",
                    modifier = Modifier.size(80.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "ILAM",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = IlamGoldLight,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = "STORE MANAGER",
                fontSize = 12.sp,
                letterSpacing = 3.sp,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(
                color = IlamGold,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
