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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vxncius.authx.data.VaultItem
import com.vxncius.authx.logic.AuthxFileCrypto
import com.vxncius.authx.logic.AuthxFileCrypto.ImportResult
import com.vxncius.authx.logic.CsvHandler
import com.vxncius.authx.logic.ImportValidator
import com.vxncius.authx.ui.theme.AuthXColors
import com.vxncius.authx.ui.theme.AuthXRadius
import com.vxncius.authx.ui.theme.Poppins

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

    var exportDialogVisible by remember { mutableStateOf(false) }
    var importDialogVisible by remember { mutableStateOf(false) }
    var pendingExportPassword by remember { mutableStateOf<CharArray?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            val password = pendingExportPassword
            pendingExportPassword = null
            if (password != null) {
                val exported = CsvHandler.exportToAuthx(context, it, itemsToExport, password)
                password.fill('\u0000')
                val message = if (exported) {
                    "Backup criptografado salvo com sucesso"
                } else {
                    "Falha ao salvar o backup criptografado"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val performImport: (Uri, CharArray?) -> Unit = { uri, password ->
        when (val result = CsvHandler.importFromAuthx(context, uri, password)) {
            is ImportResult.Success -> {
                val unique = ImportValidator.deduplicate(result.items, itemsToExport)
                val skipped = result.items.size - unique.size
                onImport(unique)
                val suffix = if (skipped > 0) " ($skipped duplicada(s) ignorada(s))" else ""
                Toast.makeText(
                    context,
                    "${unique.size} senha(s) importada(s) com sucesso$suffix",
                    Toast.LENGTH_LONG
                ).show()
            }
            ImportResult.WrongPassword ->
                Toast.makeText(context, "Senha incorreta.", Toast.LENGTH_LONG).show()
            ImportResult.InvalidFile ->
                Toast.makeText(context, "Arquivo AUTHX inválido ou corrompido.", Toast.LENGTH_LONG).show()
            ImportResult.UnsupportedVersion ->
                Toast.makeText(context, "A versão deste arquivo de backup não é suportada.", Toast.LENGTH_LONG).show()
            ImportResult.LegacyKeyMissing ->
                Toast.makeText(
                    context,
                    "Este backup foi criado por uma instalação antiga do AuthX e não pode ser recuperado nesta instalação.",
                    Toast.LENGTH_LONG
                ).show()
            ImportResult.IoError ->
                Toast.makeText(context, "Falha ao ler o arquivo de backup.", Toast.LENGTH_LONG).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            when (CsvHandler.detectFormat(context, it)) {
                AuthxFileCrypto.AuthxFormat.V2 -> {
                    pendingImportUri = it
                    importDialogVisible = true
                }
                AuthxFileCrypto.AuthxFormat.V1,
                AuthxFileCrypto.AuthxFormat.CSV -> performImport(it, null)
                AuthxFileCrypto.AuthxFormat.UNSUPPORTED ->
                    Toast.makeText(context, "A versão deste arquivo de backup não é suportada.", Toast.LENGTH_LONG).show()
                AuthxFileCrypto.AuthxFormat.INVALID ->
                    Toast.makeText(context, "Arquivo AUTHX inválido ou corrompido.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val autofillManager = context.getSystemService(AutofillManager::class.java)
    val isDefaultProvider = autofillManager?.hasEnabledAutofillServices() == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthXColors.BgBase)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 110.dp)
        ) {
            AuthXHeader(title = "Configurações", bottomPadding = 18.dp)
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
                subtitle = "Criar backup criptografado com senha",
                icon = Icons.Default.FileDownload
            ) {
                exportDialogVisible = true
            }

            SettingsLinkRow("Termos e condições", icon = Icons.Default.OpenInNew) {
                onNavigateToWebView(
                    "file:///android_asset/termos_uso.html",
                    "Termos e condições"
                )
            }

            SettingsLinkRow("Privacidade", icon = Icons.Default.OpenInNew) {
                onNavigateToWebView(
                    "file:///android_asset/politica_privacidade.html",
                    "Privacidade"
                )
            }

            SettingsLinkRow("Enviar feedback", icon = Icons.Default.Email) {
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

            SettingsLinkRow("Portfolio", icon = Icons.Default.OpenInNew) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vxncius.com"))
                runCatching { context.startActivity(intent) }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AuthXColors.BgBase)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Versão $versionName - AuthX",
                color = AuthXColors.TextTertiary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp
                ),
                fontFamily = Poppins,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Copyright © 2026 Vxncius - Todos os direitos reservados.",
                color = AuthXColors.TextTertiary.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp
                ),
                fontFamily = Poppins,
                textAlign = TextAlign.Center
            )
        }
    }

    if (exportDialogVisible) {
        ExportPasswordDialog(
            onConfirm = { password ->
                exportDialogVisible = false
                pendingExportPassword = password
                exportLauncher.launch("authx_backup.authx")
            },
            onDismiss = { exportDialogVisible = false }
        )
    }

    if (importDialogVisible) {
        ImportPasswordDialog(
            onConfirm = { password ->
                importDialogVisible = false
                val uri = pendingImportUri
                pendingImportUri = null
                if (uri != null) {
                    performImport(uri, password)
                }
                password.fill('\u0000')
            },
            onDismiss = {
                importDialogVisible = false
                pendingImportUri = null
            }
        )
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
                .padding(horizontal = 20.dp, vertical = 5.dp)
                .clip(RoundedCornerShape(AuthXRadius.Row))
                .background(AuthXColors.SurfaceRow)
                .border(1.dp, AuthXColors.BorderSubtle, RoundedCornerShape(AuthXRadius.Row))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = AuthXColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Normal
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = AuthXColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = Poppins
                    )
                }
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AuthXColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
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
                .padding(horizontal = 20.dp, vertical = 5.dp)
                .clip(RoundedCornerShape(AuthXRadius.Row))
                .background(AuthXColors.SurfaceRow)
                .border(1.dp, AuthXColors.BorderSubtle, RoundedCornerShape(AuthXRadius.Row))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Serviço de preenchimento automático",
                    color = AuthXColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isDefaultProvider) {
                        "AuthX já é seu gerenciador padrão"
                    } else {
                        "Definir como padrão"
                    },
                    color = AuthXColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = Poppins
                )
            }

            if (isDefaultProvider) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AuthXColors.AccentTeal,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = AuthXColors.DangerRed,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun PasswordVisibilityIcon(
    visible: Boolean,
    onToggle: () -> Unit
) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (visible) "Ocultar senha" else "Mostrar senha",
            tint = AuthXColors.TextSecondary
        )
    }
}

@Composable
private fun ExportPasswordDialog(
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar backup criptografado") },
        text = {
            Column {
                Text(
                    text = "Esta senha protege o arquivo de backup. Se você esquecê-la, o AuthX não poderá recuperar o conteúdo.",
                    color = AuthXColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = null
                    },
                    label = { Text("Senha") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = { PasswordVisibilityIcon(showPassword) { showPassword = !showPassword } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        error = null
                    },
                    label = { Text("Confirmar senha") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = { PasswordVisibilityIcon(showPassword) { showPassword = !showPassword } }
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    password.isBlank() -> error = "A senha não pode estar vazia."
                    password != confirmPassword -> error = "As senhas não coincidem."
                    else -> {
                        val passwordChars = password.toCharArray()
                        password = ""
                        confirmPassword = ""
                        onConfirm(passwordChars)
                    }
                }
            }) {
                Text("Exportar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ImportPasswordDialog(
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restaurar backup") },
        text = {
            Column {
                Text(
                    text = "Digite a senha usada para proteger este arquivo de backup.",
                    color = AuthXColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = null
                    },
                    label = { Text("Senha") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = { PasswordVisibilityIcon(showPassword) { showPassword = !showPassword } }
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (password.isBlank()) {
                    error = "A senha não pode estar vazia."
                } else {
                    val passwordChars = password.toCharArray()
                    password = ""
                    onConfirm(passwordChars)
                }
            }) {
                Text("Importar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
