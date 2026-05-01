package com.vxncius.authx
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.autofill.AutofillManager
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily.Companion.SansSerif
import androidx.lifecycle.lifecycleScope
import com.vxncius.authx.data.AppDatabase
import com.vxncius.authx.data.VaultItem
import com.vxncius.authx.logic.BiometricHelper
import com.vxncius.authx.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.graphics.Color as ColorCompose
import com.vxncius.authx.R
val Context.dataStore by preferencesDataStore(name = "settings")
val playfairDisplayFont = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.youngserif_regular, FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.youngserif_regular, FontWeight.Bold)
)
val Black = ColorCompose(0xFF000000)
val DarkBg = ColorCompose(0xFF000000)
val DarkSecondary = ColorCompose(0xFF212121)
val Gray900 = ColorCompose(0xFF121212)
val Gray800 = ColorCompose(0xFF1E1E1E)
val Gray700 = ColorCompose(0xFF2C2C2C)
val Gray500 = ColorCompose(0xFF9E9E9E)
val Gray200 = ColorCompose(0xFFEEEEEE)
val White = ColorCompose(0xFFFFFFFF)
val DarkGrayColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = Gray500,
    onSecondary = White,
    background = DarkBg,
    onBackground = White,
    surface = DarkBg,
    onSurface = White,
    surfaceVariant = DarkSecondary,
    onSurfaceVariant = White,
    primaryContainer = DarkSecondary,
    onPrimaryContainer = White,
    secondaryContainer = DarkSecondary,
    onSecondaryContainer = White
)
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)
val AppTypography = Typography().copy(
    displayLarge = Typography().displayLarge.copy(fontFamily = SansSerif),
    displayMedium = Typography().displayMedium.copy(fontFamily = SansSerif),
    displaySmall = Typography().displaySmall.copy(fontFamily = SansSerif),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = SansSerif),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = SansSerif),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = SansSerif),
    titleLarge = Typography().titleLarge.copy(fontFamily = SansSerif),
    titleMedium = Typography().titleMedium.copy(fontFamily = SansSerif),
    titleSmall = Typography().titleSmall.copy(fontFamily = SansSerif)
)
class MainActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private val dbPassphrase = "default_passphrase_for_demo".toByteArray()
    private val isLocked = mutableStateOf(true)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        db = AppDatabase.getDatabase(this, dbPassphrase)
        splashScreen.setKeepOnScreenCondition {
            isLocked.value
        }
        authenticateUser()
    }
    override fun onStart() {
        super.onStart()
        isLocked.value = true
        authenticateUser()
    }
    private fun authenticateUser() {
        if (BiometricHelper.canAuthenticate(this)) {
            BiometricHelper.showPrompt(this, 
                onSuccess = { 
                    isLocked.value = false
                    setupContent() 
                },
                onError = {
                    finish()
                }
            )
        } else {
            isLocked.value = false
            setupContent()
        }
    }
    private fun requestAutofillService() {
        val autofillManager = getSystemService(AutofillManager::class.java)
        if (autofillManager != null && !autofillManager.hasEnabledAutofillServices()) {
            val intent = Intent("android.settings.REQUEST_SET_AUTOFILL_SERVICE")
            intent.setData(android.net.Uri.parse("package:$packageName"))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent("android.settings.AUTOFILL_SETTINGS"))
            }
        }
    }
    private fun setupContent() {
        setContent {
            val scope = rememberCoroutineScope()
            val homeListState = androidx.compose.foundation.lazy.rememberLazyListState()
            var pressBackTime by remember { mutableStateOf(0L) }
            val context = androidx.compose.ui.platform.LocalContext.current
            var autofillPrompted by rememberSaveable { mutableStateOf(false) }
            MaterialTheme(
                colorScheme = DarkGrayColorScheme,
                shapes = AppShapes,
                typography = AppTypography
            ) {
                val colorScheme = MaterialTheme.colorScheme
                SideEffect {
                    window.statusBarColor = colorScheme.surface.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf("home") }
                    var selectedItemId by remember { mutableStateOf<Int?>(null) }
                    var searchQuery by rememberSaveable { mutableStateOf("") }
                    var webViewUrl by remember { mutableStateOf("") }
                    var webViewTitle by remember { mutableStateOf("") }
                    val items by db.vaultDao().getAllItems().collectAsState(initial = emptyList())
                    val selectedItem = items.find { it.id == selectedItemId }
                    LaunchedEffect(isLocked.value) {
                        if (!isLocked.value && !autofillPrompted) {
                            delay(3000)
                            requestAutofillService()
                            autofillPrompted = true
                        }
                    }
                    androidx.activity.compose.BackHandler(enabled = currentScreen == "home") {
                        if (System.currentTimeMillis() - pressBackTime < 2000) {
                            (context as android.app.Activity).finish()
                        } else {
                            pressBackTime = System.currentTimeMillis()
                            android.widget.Toast.makeText(context, "Pressione novamente para sair", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        when (currentScreen) {
                            "home" -> HomeScreen(
                                items = items,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onAddClick = { currentScreen = "add" },
                                onSettingsClick = { currentScreen = "settings" },
                                onItemClick = { item ->
                                    selectedItemId = item.id
                                    currentScreen = "detail"
                                },
                                onDeleteItems = { itemsToDelete ->
                                    scope.launch { itemsToDelete.forEach { db.vaultDao().deleteItem(it) } }
                                },
                                onAddCardClick = { currentScreen = "add_card" },
                                onAddAddressClick = { currentScreen = "add_address" },
                                onGeneratePasswordClick = { currentScreen = "generator" },
                                listState = homeListState
                            )
                            "add" -> AddItemScreen(
                                onBack = { currentScreen = "home" },
                                onSave = { item ->
                                    scope.launch {
                                        db.vaultDao().insertItem(item)
                                        currentScreen = "home"
                                    }
                                }
                            )
                            "add_card" -> AddCardScreen(
                                onBack = { currentScreen = "home" },
                                onSave = { item ->
                                    scope.launch {
                                        db.vaultDao().insertItem(item)
                                        currentScreen = "home"
                                    }
                                }
                            )
                            "add_address" -> AddAddressScreen(
                                onBack = { currentScreen = "home" },
                                onSave = { item ->
                                    scope.launch {
                                        db.vaultDao().insertItem(item)
                                        currentScreen = "home"
                                    }
                                }
                            )
                            "generator" -> PasswordGeneratorScreen(
                                onBack = { currentScreen = "home" }
                            )
                            "detail" -> {
                                if (selectedItem != null) {
                                    ItemDetailScreen(
                                        item = selectedItem,
                                        onBack = {
                                            selectedItemId = null
                                            currentScreen = "home"
                                        },
                                        onUpdate = { updatedItem ->
                                            scope.launch {
                                                db.vaultDao().updateItem(updatedItem)
                                            }
                                        },
                                        onDelete = { item ->
                                            scope.launch {
                                                db.vaultDao().deleteItem(item)
                                                selectedItemId = null
                                                currentScreen = "home"
                                            }
                                        }
                                    )
                                } else {
                                    currentScreen = "home"
                                }
                            }
                            "settings" -> SettingsScreen(
                                onBack = { currentScreen = "home" },
                                onImport = { importedItems ->
                                    scope.launch {
                                        importedItems.forEach { db.vaultDao().insertItem(it) }
                                        currentScreen = "home"
                                    }
                                },
                                itemsToExport = items,
                                onNavigateToWebView = { url, title ->
                                    webViewUrl = url
                                    webViewTitle = title
                                    currentScreen = "webview"
                                }
                            )
                            "webview" -> WebViewScreen(
                                url = webViewUrl,
                                title = webViewTitle,
                                onBack = { currentScreen = "settings" }
                            )
                        }
                    }
                    if (isLocked.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .blur(20.dp)
                        )
                    }
                }
            }
        }
    }
}

