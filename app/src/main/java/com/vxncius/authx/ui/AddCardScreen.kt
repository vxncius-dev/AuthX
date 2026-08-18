package com.vxncius.authx.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vxncius.authx.data.VaultItem
import com.vxncius.authx.ui.theme.AuthXColors
import com.vxncius.authx.ui.theme.AuthXRadius
import com.vxncius.authx.ui.theme.Poppins

private val addFieldShape = RoundedCornerShape(AuthXRadius.Row)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    onBack: () -> Unit,
    onSave: (VaultItem) -> Unit
) {
    var cardName by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    val addFieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = AuthXColors.BorderSubtle,
        focusedBorderColor = AuthXColors.BorderCard,
        unfocusedContainerColor = AuthXColors.SurfaceRow,
        focusedContainerColor = AuthXColors.SurfaceRow,
        cursorColor = AuthXColors.TextPrimary
    )

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            AuthXHeader("Novo Cartão", bottomPadding = 0.dp)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = cardName,
                onValueChange = { cardName = it },
                label = { Text("Nome do Cartão (ex: Nubank)") },
                modifier = Modifier.fillMaxWidth(),
                shape = addFieldShape, colors = addFieldColors
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = holderName,
                onValueChange = { holderName = it },
                label = { Text("Nome do Titular") },
                modifier = Modifier.fillMaxWidth(),
                shape = addFieldShape, colors = addFieldColors
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it },
                label = { Text("Número do Cartão") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = addFieldShape, colors = addFieldColors
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Validade (MM/AA)") },
                    modifier = Modifier.weight(1f),
                    shape = addFieldShape, colors = addFieldColors
                )
                OutlinedTextField(
                    value = cvv,
                    onValueChange = { cvv = it },
                    label = { Text("CVV") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = addFieldShape, colors = addFieldColors
                )
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    if (cardName.isNotBlank() && cardNumber.isNotBlank()) {
                        onSave(
                            VaultItem(
                                title = cardName,
                                username = holderName,
                                password = cardNumber,
                                websiteUrl = "Val: $expiryDate | CVV: $cvv",
                                totpSecret = null,
                                type = "CARD"
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = cardName.isNotBlank() && cardNumber.isNotBlank(),
                shape = RoundedCornerShape(AuthXRadius.Row),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuthXColors.TextPrimary,
                    contentColor = AuthXColors.BgBase
                )
            ) {
                Text("Salvar Cartão", fontFamily = Poppins, fontWeight = FontWeight.Medium)
            }
        }
    }
}
