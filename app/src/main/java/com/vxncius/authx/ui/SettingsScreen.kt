package com.vxncius.authx.ui
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vxncius.authx.data.VaultItem
import com.vxncius.authx.logic.CsvHandler
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onImport: (List<VaultItem>) -> Unit,
    itemsToExport: List<VaultItem>,
    onNavigateToWebView: (String, String) -> Unit
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { CsvHandler.exportToCsv(context, it, itemsToExport) }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImport(CsvHandler.importFromCsv(context, it)) }
    }
    val autofillManager = context.getSystemService(AutofillManager::class.java)
    val isDefaultProvider = autofillManager?.hasEnabledAutofillServices() == true
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                "GERAL",
                style = MaterialTheme.typography.labelMedium,
                color = ComposeColor.Gray,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
            SettingsGroup {
                Row(
                   modifier = Modifier
                       .fillMaxWidth()
                       .padding(16.dp),
                   horizontalArrangement = Arrangement.SpaceBetween,
                   verticalAlignment = Alignment.CenterVertically
                ) {
                   Text("Modo Escuro", style = MaterialTheme.typography.bodyLarge)
                   Switch(
                       checked = isDarkMode, 
                       onCheckedChange = onThemeToggle,
                       modifier = Modifier.height(24.dp)
                   )
                }
                Divider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                             if (!isDefaultProvider) {
                                val intent = Intent("android.settings.REQUEST_SET_AUTOFILL_SERVICE")
                                intent.setData(android.net.Uri.parse("package:${context.packageName}"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    context.startActivity(Intent("android.settings.AUTOFILL_SETTINGS"))
                                }
                             }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Serviço de preenchimento automático", style = MaterialTheme.typography.bodyLarge)
                         Text(
                            text = if (isDefaultProvider) "AuthX é seu gerenciador padrão" else "Definir como padrão",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ComposeColor.Gray
                        )
                    }
                    if (isDefaultProvider) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "IMPORTAR E EXPORTAR",
                style = MaterialTheme.typography.labelMedium,
                color = ComposeColor.Gray,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
             SettingsGroup {
                AppleSettingsLink("Importar arquivo CSV") { 
                    importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/csv", "text/*"))
                }
                Divider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                AppleSettingsLink("Exportar arquivo CSV") { 
                     exportLauncher.launch("vault_export.csv")
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "SOBRE",
                style = MaterialTheme.typography.labelMedium,
                color = ComposeColor.Gray,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
            SettingsGroup {
                AppleSettingsLink("Termos e Condições") { onNavigateToWebView("file:/
                Divider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                AppleSettingsLink("Privacidade") { onNavigateToWebView("file:/
                Divider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                AppleSettingsLink("Enviar Feedback") { 
                     val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:vxncius@hotmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Feedback AuthX")
                     }
                     try {
                        context.startActivity(intent)
                     } catch (e: Exception) {
                     }
                }
                Divider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                AppleSettingsLink("GitHub") { 
                     onNavigateToWebView("https://github.com/vxncius-dev", "GitHub")
                }
            }
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Versão 1.21.4-AuthX",
                    style = MaterialTheme.typography.bodySmall,
                    color = ComposeColor.Gray
                )
                Text(
                    "Copyright © 2025 AuthX Inc. Todos os direitos reservados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ComposeColor.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
@Composable
fun AppleSettingsLink(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = ComposeColor.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = ComposeColor(0xFF007AFF),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column {
            content()
        }
    }
}
@Composable
fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
private fun Modifier.size(size: androidx.compose.ui.unit.Dp) = this.then(Modifier.width(size).height(size))

