package com.vxncius.authx.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun PasswordGeneratorScreen(
    onBack: () -> Unit
) {
    var length by remember { mutableStateOf(16f) }
    var useUppercase by remember { mutableStateOf(true) }
    var useLowercase by remember { mutableStateOf(true) }
    var useNumbers by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var generatedPassword by remember { mutableStateOf("") }
    val context = LocalContext.current

    fun generatePassword() {
        val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowercase = "abcdefghijklmnopqrstuvwxyz"
        val numbers = "0123456789"
        val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        var validChars = ""
        if (useUppercase) validChars += uppercase
        if (useLowercase) validChars += lowercase
        if (useNumbers) validChars += numbers
        if (useSymbols) validChars += symbols

        generatedPassword = if (validChars.isEmpty()) {
            ""
        } else {
            buildString {
                repeat(length.toInt()) {
                    append(validChars[Random.nextInt(validChars.length)])
                }
            }
        }
    }

    LaunchedEffect(length, useUppercase, useLowercase, useNumbers, useSymbols) {
        generatePassword()
    }

    val strength = passwordStrength(length.toInt(), useUppercase, useLowercase, useNumbers, useSymbols)

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            AuthXHeader("Gerador de Senha")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = generatedPassword.ifBlank { "Selecione pelo menos uma opção" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (generatedPassword.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Generated Password", generatedPassword)
                                    clipboard.setPrimaryClip(clip)
                                }
                            },
                            enabled = generatedPassword.isNotBlank()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar senha")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = strength.label,
                        color = strength.color,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Text("Opções", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(16.dp))
            Text("Comprimento: ${length.toInt()}")
            Slider(
                value = length,
                onValueChange = { length = it },
                valueRange = 8f..32f,
                steps = 24,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = useUppercase, onCheckedChange = { useUppercase = it })
                Text("Letras Maiúsculas (A-Z)")
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = useLowercase, onCheckedChange = { useLowercase = it })
                Text("Letras Minúsculas (a-z)")
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = useNumbers, onCheckedChange = { useNumbers = it })
                Text("Números (0-9)")
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = useSymbols, onCheckedChange = { useSymbols = it })
                Text("Símbolos (!@#$)")
            }
        }
    }
}

private data class PasswordStrength(
    val label: String,
    val color: Color
)

private fun passwordStrength(
    length: Int,
    useUppercase: Boolean,
    useLowercase: Boolean,
    useNumbers: Boolean,
    useSymbols: Boolean
): PasswordStrength {
    val variety = listOf(useUppercase, useLowercase, useNumbers, useSymbols).count { it }
    val score = length + variety * 4

    return when {
        variety == 0 -> PasswordStrength("Segurança: indisponível", Color(0xFFFF3B30))
        score < 20 -> PasswordStrength("Segurança: fraca", Color(0xFFFF3B30))
        score < 32 -> PasswordStrength("Segurança: média", Color(0xFFFF9800))
        else -> PasswordStrength("Segurança: forte", Color(0xFF00E676))
    }
}
