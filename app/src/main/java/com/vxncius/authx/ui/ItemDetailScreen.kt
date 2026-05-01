package com.vxncius.authx.ui
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vxncius.authx.data.VaultItem
import com.vxncius.authx.logic.BiometricHelper
import com.vxncius.authx.logic.TotpManager
import kotlinx.coroutines.delay
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    item: VaultItem,
    onBack: () -> Unit,
    onUpdate: (VaultItem) -> Unit,
    onDelete: (VaultItem) -> Unit
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }
    var editedItem by remember(item) { mutableStateOf(item) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf(TotpManager.getTimeRemaining(item.period)) }
    var currentTotpCode by remember(item.totpSecret, timeRemaining == item.period.toLong()) {
        mutableStateOf(if (item.totpSecret != null) TotpManager.generateCode(item.totpSecret, item.algorithm, item.digits, item.period) else "")
    }
    BackHandler {
        if (showScanner) {
            showScanner = false
        } else if (isEditing) {
            isEditing = false
        } else {
            onBack()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showScanner = true
    }
    LaunchedEffect(item.period) {
        while (true) {
            val remaining = TotpManager.getTimeRemaining(item.period)
            if (remaining > timeRemaining) {
                if (item.totpSecret != null) {
                    currentTotpCode = TotpManager.generateCode(item.totpSecret, item.algorithm, item.digits, item.period)
                }
            }
            timeRemaining = remaining
            delay(500)
        }
    }
    if (showScanner) {
        QrScannerScreen(onResult = { result ->
            try {
                val secret = if (result.startsWith("otpauth://")) {
                    Uri.parse(result).getQueryParameter("secret") ?: ""
                } else result.trim()
                if (isEditing) {
                    editedItem = editedItem.copy(totpSecret = secret)
                } else {
                    onUpdate(item.copy(totpSecret = secret))
                }
                showScanner = false
            } catch (e: Exception) {
                showScanner = false
            }
        })
        return
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir item?") },
            text = { Text("Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(item)
                    showDeleteDialog = false
                }) {
                    Text("Excluir", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    Scaffold(
        topBar = {
            AuthXHeaderRow(
                title = { 
                    if (isEditing) {
                        TextField(
                            value = editedItem.title,
                            onValueChange = { editedItem = editedItem.copy(title = it) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Título") },
                            colors = TextFieldDefaults.textFieldColors(containerColor = Color.Transparent)
                        )
                    } else {
                        Text(
                            text = item.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { 
                            onUpdate(editedItem)
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Salvar", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (item.type) {
                "CARD" -> {
                    EditableDetailField("Titular do Cartão", editedItem.username, isEditing, onValueChange = { editedItem = editedItem.copy(username = it) }, onCopy = { copyToClipboard(context, item.username) })
                    Spacer(Modifier.height(16.dp))
                    EditableDetailField("Número do Cartão", editedItem.password, isEditing, isPassword = true, showPassword = showPassword, onToggleVisibility = { showPassword = !showPassword }, onValueChange = { editedItem = editedItem.copy(password = it) }, onCopy = { copyToClipboard(context, item.password) })
                    Spacer(Modifier.height(16.dp))
                    EditableDetailField("Detalhes (Validade/CVV)", editedItem.websiteUrl, isEditing, onValueChange = { editedItem = editedItem.copy(websiteUrl = it) }, onCopy = { copyToClipboard(context, item.websiteUrl) })
                }
                "ADDRESS" -> {
                    EditableDetailField("Endereço Completo", editedItem.username, isEditing, singleLine = false, onValueChange = { editedItem = editedItem.copy(username = it) }, onCopy = { copyToClipboard(context, item.username) })
                    Spacer(Modifier.height(16.dp))
                    EditableDetailField("Link do Mapa", editedItem.websiteUrl, isEditing, onValueChange = { editedItem = editedItem.copy(websiteUrl = it) }, onCopy = { copyToClipboard(context, item.websiteUrl) })
                }
                else -> {
                    EditableDetailField("Usuário", editedItem.username, isEditing, onValueChange = { editedItem = editedItem.copy(username = it) }, onCopy = { copyToClipboard(context, item.username) })
                    Spacer(Modifier.height(16.dp))
                    EditableDetailField("Senha", editedItem.password, isEditing, isPassword = true, showPassword = showPassword, onToggleVisibility = { showPassword = !showPassword }, onValueChange = { editedItem = editedItem.copy(password = it) }, onCopy = { copyToClipboard(context, item.password) })
                    Spacer(Modifier.height(16.dp))
                    EditableDetailField("URL do Site", editedItem.websiteUrl, isEditing, onValueChange = { editedItem = editedItem.copy(websiteUrl = it) }, onCopy = { copyToClipboard(context, item.websiteUrl) })
                }
            }
            Spacer(Modifier.height(16.dp))
            EditableDetailField("Notas", editedItem.notes, isEditing, singleLine = false, onValueChange = { editedItem = editedItem.copy(notes = it) }, onCopy = { copyToClipboard(context, item.notes) })
            if (item.totpSecret != null || isEditing) {
                Spacer(Modifier.height(32.dp))
                Text("Autenticação de Dois Fatores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (isEditing) {
                    OutlinedTextField(
                        value = editedItem.totpSecret ?: "",
                        onValueChange = { editedItem = editedItem.copy(totpSecret = it.ifBlank { null }) },
                        label = { Text("Chave Secreta TOTP") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                val permissionCheckResult = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                                if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                    showScanner = true
                                } else {
                                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear QR")
                            }
                        }
                    )
                } else if (item.totpSecret != null) {
                    Card(onClick = { copyToClipboard(context, currentTotpCode) }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = currentTotpCode.chunked(3).joinToString(" "),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 2.sp
                                )
                            }
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                                CircularProgressIndicator(
                                    progress = (item.period - timeRemaining).toFloat() / item.period.toFloat(),
                                    modifier = Modifier.fillMaxSize(),
                                    color = if (timeRemaining < 5) Color.Red else MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp
                                )
                                Text(
                                    text = timeRemaining.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(32.dp))
                Button(onClick = { isEditing = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar 2FA (TOTP)")
                }
            }
        }
    }
}
@Composable
fun EditableDetailField(
    label: String,
    value: String,
    isEditing: Boolean,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onToggleVisibility: () -> Unit = {},
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit = {},
    onCopy: () -> Unit = {}
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                singleLine = singleLine,
                visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = if (isPassword) {
                    {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    }
                } else null
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isPassword && !showPassword) "••••••••••••" else value.ifEmpty { "Nenhum" },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                if (isPassword) {
                    IconButton(onClick = onToggleVisibility) {
                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                }
                if (value.isNotEmpty()) {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar")
                    }
                }
            }
            Divider(color = Color.Gray.copy(alpha = 0.2f))
        }
    }
}
private fun copyToClipboard(context: Context, text: String) {
    if (text.isEmpty()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = android.content.ClipData.newPlainText("AuthX", text)
    clipboard.setPrimaryClip(clip)
}

