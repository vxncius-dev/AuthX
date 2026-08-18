package com.vxncius.authx.ui
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vxncius.authx.data.VaultItem
import com.vxncius.authx.logic.BiometricHelper
import com.vxncius.authx.logic.TotpManager
import com.vxncius.authx.ui.theme.AuthXColors
import com.vxncius.authx.ui.theme.AuthXRadius
import com.vxncius.authx.ui.theme.OtpCodeStyle
import com.vxncius.authx.ui.theme.Poppins
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
                    Text("Excluir", color = AuthXColors.DangerRed)
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
                            placeholder = { Text("Título", color = AuthXColors.TextTertiary) },
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(
                            text = item.title,
                            color = AuthXColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
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
                            Icon(Icons.Default.Check, contentDescription = "Salvar", tint = AuthXColors.AccentTeal)
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = AuthXColors.TextPrimary)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = AuthXColors.DangerRed)
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
                Text(
                    "Autenticação de Dois Fatores",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Poppins,
                    color = AuthXColors.TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                if (isEditing) {
                    OutlinedTextField(
                        value = editedItem.totpSecret ?: "",
                        onValueChange = { editedItem = editedItem.copy(totpSecret = it.ifBlank { null }) },
                        label = {
                            Text("Chave Secreta TOTP", fontFamily = Poppins, color = AuthXColors.TextTertiary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AuthXRadius.Row),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = AuthXColors.BorderSubtle,
                            focusedBorderColor = AuthXColors.BorderCard,
                            unfocusedContainerColor = AuthXColors.SurfaceRow,
                            focusedContainerColor = AuthXColors.SurfaceRow,
                            cursorColor = AuthXColors.TextPrimary
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = Poppins),
                        trailingIcon = {
                            IconButton(onClick = {
                                val permissionCheckResult = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                                if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                    showScanner = true
                                } else {
                                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear QR", tint = AuthXColors.TextSecondary)
                            }
                        }
                    )
                } else if (item.totpSecret != null) {
                    val otpCardShape = RoundedCornerShape(AuthXRadius.Card)
                    Card(
                        onClick = { copyToClipboard(context, currentTotpCode) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AuthXColors.BorderCard, otpCardShape),
                        colors = CardDefaults.cardColors(
                            containerColor = AuthXColors.SurfaceCard,
                            contentColor = AuthXColors.TextPrimary
                        ),
                        shape = otpCardShape
                    ) {
                        Row(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = currentTotpCode.chunked(3).joinToString(" "),
                                    style = OtpCodeStyle
                                )
                            }
TotpRing(remainingSeconds = timeRemaining, period = item.period, modifier = Modifier.size(52.dp))
                }
            }
}
            }
            else {
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AuthXRadius.Row),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuthXColors.TextPrimary,
                        contentColor = AuthXColors.BgBase
                    )
                ) {
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
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium,
                    color = AuthXColors.TextTertiary
                )
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                singleLine = singleLine,
                shape = RoundedCornerShape(AuthXRadius.Row),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = AuthXColors.BorderSubtle,
                    focusedBorderColor = AuthXColors.BorderCard,
                    unfocusedContainerColor = AuthXColors.SurfaceRow,
                    focusedContainerColor = AuthXColors.SurfaceRow,
                    cursorColor = AuthXColors.TextPrimary
                ),
                visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = if (isPassword) {
                    {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = AuthXColors.TextSecondary)
                        }
                    }
                } else null
            )
        } else {
            val readShape = RoundedCornerShape(AuthXRadius.Row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(readShape)
                    .background(AuthXColors.SurfaceRow)
                    .border(1.dp, AuthXColors.BorderSubtle, readShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPassword && !showPassword) "••••••••••••" else value.ifEmpty { "Nenhum" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = if (isPassword) FontFamily.Monospace else Poppins,
                    color = if (value.isEmpty()) AuthXColors.TextTertiary else AuthXColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (isPassword) {
                    IconButton(onClick = onToggleVisibility) {
                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = AuthXColors.TextSecondary)
                    }
                }
                if (value.isNotEmpty()) {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = AuthXColors.TextSecondary)
                    }
                }
            }
        }
    }
}
private fun copyToClipboard(context: Context, text: String) {
    if (text.isEmpty()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = android.content.ClipData.newPlainText("AuthX", text)
    clipboard.setPrimaryClip(clip)
}

