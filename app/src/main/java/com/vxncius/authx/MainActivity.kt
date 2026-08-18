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
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.lifecycleScope
import com.vxncius.authx.BuildConfig
import com.vxncius.authx.data.AppDatabase
import com.vxncius.authx.data.VaultItem
import com.vxncius.authx.logic.BiometricHelper
import com.vxncius.authx.ui.*
import com.vxncius.authx.ui.theme.AuthXColors
import com.vxncius.authx.ui.theme.AuthXMaterialTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
val Context.dataStore by preferencesDataStore(name = "settings")
class MainActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private val dbPassphrase = "default_passphrase_for_demo".toByteArray()
    private val isLocked = mutableStateOf(true)
    private val showSplash = mutableStateOf(true)
    private var splashHandled = false
    private var autofillPromptShownForThisLaunch = false
    private val debugSkipBiometricPrompt = true
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        db = AppDatabase.getDatabase(this, dbPassphrase)
        splashScreen.setKeepOnScreenCondition { false }
        setupContent()
    }
    override fun onStart() {
        super.onStart()
        isLocked.value = true
        if (splashHandled) {
            authenticateUser()
        }
    }
    private fun onSplashFinished() {
        if (splashHandled) return
        splashHandled = true
        authenticateUser()
    }
    private fun authenticateUser() {
        val skipBiometric = BuildConfig.DEBUG && debugSkipBiometricPrompt
        if (!skipBiometric && BiometricHelper.canAuthenticate(this)) {
            BiometricHelper.showPrompt(this, 
                onSuccess = { 
                    unlockApp() 
                },
                onError = {
                    finish()
                }
            )
        } else {
            unlockApp()
        }
    }
    private fun unlockApp() {
        isLocked.value = false
        showSplash.value = false
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
            AuthXMaterialTheme {
                SideEffect {
                    window.statusBarColor = AuthXColors.BgBase.toArgb()
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
                        if (!isLocked.value && !autofillPromptShownForThisLaunch) {
                            delay(3000)
                            requestAutofillService()
                            autofillPromptShownForThisLaunch = true
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
                    if (showSplash.value) {
                        SplashScreen(onFinished = ::onSplashFinished)
                    }
                }
            }
        }
    }
}

