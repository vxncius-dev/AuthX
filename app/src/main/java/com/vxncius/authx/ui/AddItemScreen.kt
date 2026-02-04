package com.vxncius.authx.ui
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vxncius.authx.data.VaultItem
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    onBack: () -> Unit,
    onSave: (VaultItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var totpSecret by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    BackHandler {
        if (showScanner) {
            showScanner = false
        } else {
            onBack()
        }
    }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showScanner = true
    }
    if (showScanner) {
        QrScannerScreen(onResult = { result ->
            try {
                if (result.startsWith("otpauth://")) {
                    val uri = Uri.parse(result)
                    totpSecret = uri.getQueryParameter("secret") ?: ""
                    if (title.isEmpty()) title = uri.getQueryParameter("issuer") ?: ""
                    if (username.isEmpty()) username = uri.path?.removePrefix("/totp/")?.substringAfter(":") ?: ""
                } else {
                    totpSecret = result.trim()
                }
                showScanner = false
            } catch (e: Exception) {
                showScanner = false
            }
        })
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título (ex: Netflix)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = websiteUrl,
                onValueChange = { websiteUrl = it },
                label = { Text("URL do site (ex: netflix.com)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuário / Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(24.dp))
            Text("Autenticação de Dois Fatores (Opcional)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = totpSecret,
                onValueChange = { totpSecret = it },
                label = { Text("Chave Secreta TOTP") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                     IconButton(onClick = {
                        val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                            showScanner = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear QR TOTP")
                    }
                }
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            VaultItem(
                                title = title,
                                websiteUrl = websiteUrl.trim(),
                                username = username.trim(),
                                password = password,
                                totpSecret = totpSecret.trim().ifBlank { null }
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = title.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Salvar no Cofre")
            }
        }
    }
}

