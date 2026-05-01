package com.vxncius.authx.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vxncius.authx.data.VaultItem
import com.vxncius.authx.logic.CsvHandler

private val SettingsBackground = Color(0xFF000000)
private val SettingsDividerColor = Color(0xFF242424)
private val SettingsMutedTextColor = Color(0xFF8A8A8A)
private val SettingsRowTextColor = Color(0xFFF4F4F4)
private val SettingsSuccessColor = Color(0xFF00E676)
private val SettingsDividerHorizontalPadding = 20.dp
private val SettingsRowHeight = 72.dp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onImport: (List<VaultItem>) -> Unit,
    itemsToExport: List<VaultItem>,
    onNavigateToWebView: (String, String) -> Unit
) {
    BackHandler(onBack = onBack)
    SettingsDrawerContent(
        onImport = onImport,
        itemsToExport = itemsToExport,
        onNavigateToWebView = onNavigateToWebView
    )
}

@Composable
fun SettingsDrawerContent(
    onImport: (List<VaultItem>) -> Unit,
    itemsToExport: List<VaultItem>,
    onNavigateToWebView: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val versionName = remember {
        getAppVersionName(context.packageManager, context.packageName)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            val exported = CsvHandler.exportToCsv(context, it, itemsToExport)
            val message = if (exported) {
                "Backup criptografado salvo com sucesso"
            } else {
                "Falha ao salvar o backup criptografado"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val importedItems = CsvHandler.importFromCsv(context, it)
            onImport(importedItems)
            val message = if (importedItems.isNotEmpty()) {
                "${importedItems.size} senha(s) importada(s) com sucesso"
            } else {
                "Nenhuma senha foi importada"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val autofillManager = context.getSystemService(AutofillManager::class.java)
    val isDefaultProvider = autofillManager?.hasEnabledAutofillServices() == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 110.dp)
        ) {
            Text(
                text = "Configurações",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 18.dp)
            )
            SettingsAutofillRow(
                isDefaultProvider = isDefaultProvider,
                onClick = {
                    if (!isDefaultProvider) {
                        val requestIntent = Intent("android.settings.REQUEST_SET_AUTOFILL_SERVICE").apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        runCatching { context.startActivity(requestIntent) }
                            .onFailure {
                                context.startActivity(Intent("android.settings.AUTOFILL_SETTINGS"))
                            }
                    }
                }
            )

            SettingsLinkRow(
                title = "Importar arquivo",
                subtitle = "Restaurar backup criptografado do AuthX",
                icon = Icons.Default.FileUpload
            ) {
                importLauncher.launch(
                    arrayOf(
                        "application/octet-stream",
                        "text/comma-separated-values",
                        "text/csv",
                        "application/csv",
                        "text/*"
                    )
                )
            }

            SettingsLinkRow(
                title = "Exportar arquivo",
                subtitle = "Criar backup criptografado exclusivo do app",
                icon = Icons.Default.FileDownload
            ) {
                exportLauncher.launch("authx_backup.authx")
            }

            SettingsLinkRow("Termos e condições") {
                onNavigateToWebView(
                    "file:///android_asset/termos_uso.html",
                    "Termos e condições"
                )
            }

            SettingsLinkRow("Privacidade") {
                onNavigateToWebView(
                    "file:///android_asset/politica_privacidade.html",
                    "Privacidade"
                )
            }

            SettingsLinkRow("Enviar feedback") {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse(
                        "mailto:vxncius@hotmail.com?subject=${
                            Uri.encode("Feedback AuthX")
                        }"
                    )
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback AuthX")
                    putExtra(Intent.EXTRA_TEXT, "Conta pra mim o que posso melhorar no app:\n\n")
                }
                runCatching { context.startActivity(intent) }
            }

            SettingsLinkRow("Portfolio") {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vxncius.com"))
                runCatching { context.startActivity(intent) }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(SettingsBackground)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Versão $versionName - AuthX",
                color = SettingsMutedTextColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp
                ),
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Copyright © 2026 Vxncius - Todos os direitos reservados.",
                color = SettingsMutedTextColor.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp
                ),
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getAppVersionName(
    packageManager: PackageManager,
    packageName: String
): String {
    return runCatching {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        packageInfo.versionName ?: "1.21.5"
    }.getOrDefault("1.21.5")
}

@Composable
private fun SettingsLinkRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SettingsRowHeight)
                .clickable(onClick = onClick)
                .padding(horizontal = SettingsDividerHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = SettingsRowTextColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = SettingsMutedTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SettingsMutedTextColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Divider(
            color = SettingsDividerColor,
            modifier = Modifier.padding(horizontal = SettingsDividerHorizontalPadding)
        )
    }
}

@Composable
private fun SettingsAutofillRow(
    isDefaultProvider: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SettingsRowHeight)
                .clickable(onClick = onClick)
                .padding(horizontal = SettingsDividerHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Serviço de preenchimento automático",
                    color = SettingsRowTextColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isDefaultProvider) {
                        "AuthX já é seu gerenciador padrão"
                    } else {
                        "Definir como padrão"
                    },
                    color = SettingsMutedTextColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.SansSerif
                )
            }

            if (isDefaultProvider) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SettingsSuccessColor,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(28.dp))
            }
        }
        Divider(
            color = SettingsDividerColor,
            modifier = Modifier.padding(horizontal = SettingsDividerHorizontalPadding)
        )
    }
}
