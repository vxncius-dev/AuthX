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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.vxncius.authx.data.VaultItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAddressScreen(
    onBack: () -> Unit,
    onSave: (VaultItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var zip by remember { mutableStateOf("") }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            AuthXHeader("Novo Endereço", bottomPadding = 0.dp)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome do Local (ex: Casa)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("Rua") },
                    modifier = Modifier.weight(2f),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Nº") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Cidade") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("UF") },
                    modifier = Modifier.weight(0.5f),
                    shape = MaterialTheme.shapes.medium
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = zip,
                onValueChange = { zip = it },
                label = { Text("CEP") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    if (name.isNotBlank() && street.isNotBlank()) {
                        val fullAddress = "$street, $number - $city/$state - $zip"
                        onSave(
                            VaultItem(
                                title = name,
                                username = fullAddress,
                                password = "",
                                websiteUrl = "https://maps.google.com/?q=$fullAddress",
                                totpSecret = null,
                                type = "ADDRESS"
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = name.isNotBlank() && street.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Salvar Endereço")
            }
        }
    }
}
