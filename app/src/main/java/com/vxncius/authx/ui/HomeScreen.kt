package com.vxncius.authx.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.vxncius.authx.data.VaultItem

private val FabDark = Color(0xFF212121)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    items: List<VaultItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onItemClick: (VaultItem) -> Unit,
    onDeleteItems: (List<VaultItem>) -> Unit,
    onAddCardClick: () -> Unit,
    onAddAddressClick: () -> Unit,
    onGeneratePasswordClick: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    var selectedItems by remember { mutableStateOf(setOf<VaultItem>()) }
    var selectionMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var passwordsExpanded by rememberSaveable { mutableStateOf(true) }
    var cardsExpanded by rememberSaveable { mutableStateOf(true) }
    var placesExpanded by rememberSaveable { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current

    val searchedItems = items.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true) ||
            it.websiteUrl.contains(searchQuery, ignoreCase = true)
    }
    val passwordItems = searchedItems.filter { it.type != "CARD" && it.type != "ADDRESS" }
    val cardItems = searchedItems.filter { it.type == "CARD" }
    val placeItems = searchedItems.filter { it.type == "ADDRESS" }
    val filteredItems = passwordItems + cardItems + placeItems

    fun toggleSelection(item: VaultItem) {
        selectedItems = if (selectedItems.contains(item)) selectedItems - item else selectedItems + item
        if (selectedItems.isEmpty()) selectionMode = false
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir ${selectedItems.size} ite(ns)?") },
            text = { Text("Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteItems(selectedItems.toList())
                    selectedItems = emptySet()
                    selectionMode = false
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

    androidx.activity.compose.BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedItems = emptySet()
    }

    Scaffold(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { focusManager.clearFocus() },
        topBar = {
            if (selectionMode) {
                AuthXHeaderRow(
                    title = {
                        Text(
                            text = "${selectedItems.size} selecionado(s)",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    },
                    actions = {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir selecionados", tint = Color.Red)
                        }
                        IconButton(onClick = {
                            selectionMode = false
                            selectedItems = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.White)
                        }
                    }
                )
            } else {
                Column {
                    AuthXHeaderRow(
                        topPadding = 13.dp,
                        bottomPadding = 8.dp,
                        title = {
                            Text(
                                text = "AuthX",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        },
                        actions = {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Menu, contentDescription = "Abrir menu", tint = Color.White)
                            }
                        }
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Pesquisar") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar")
                                }
                            }
                        },
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        singleLine = true
                    )
                }
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                var showFabMenu by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier.padding(bottom = 64.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showFabMenu) {
                        FabMenuAction("Gerar senha", Icons.Default.Password) {
                            showFabMenu = false
                            onGeneratePasswordClick()
                        }
                        FabMenuAction("Novo endereço", Icons.Default.Home) {
                            showFabMenu = false
                            onAddAddressClick()
                        }
                        FabMenuAction("Novo cartão", Icons.Default.CreditCard) {
                            showFabMenu = false
                            onAddCardClick()
                        }
                        FabMenuAction("Nova senha", Icons.Default.VpnKey) {
                            showFabMenu = false
                            onAddClick()
                        }
                    }
                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = FabDark,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(
                            imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (showFabMenu) "Fechar menu" else "Novo item",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isEmpty()) {
                        "Nenhum item ainda. Adicione um!"
                    } else {
                        "Nenhum item encontrado"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                state = listState
            ) {
                vaultSection(
                    title = "Senhas",
                    count = passwordItems.size,
                    expanded = passwordsExpanded,
                    onToggle = { passwordsExpanded = !passwordsExpanded },
                    items = passwordItems,
                    selectedItems = selectedItems,
                    selectionMode = selectionMode,
                    onItemClick = onItemClick,
                    onToggleSelection = ::toggleSelection,
                    onEnterSelection = { selectionMode = true }
                )
                vaultSection(
                    title = "Cartões",
                    count = cardItems.size,
                    expanded = cardsExpanded,
                    onToggle = { cardsExpanded = !cardsExpanded },
                    items = cardItems,
                    selectedItems = selectedItems,
                    selectionMode = selectionMode,
                    onItemClick = onItemClick,
                    onToggleSelection = ::toggleSelection,
                    onEnterSelection = { selectionMode = true }
                )
                vaultSection(
                    title = "Locais",
                    count = placeItems.size,
                    expanded = placesExpanded,
                    onToggle = { placesExpanded = !placesExpanded },
                    items = placeItems,
                    selectedItems = selectedItems,
                    selectionMode = selectionMode,
                    onItemClick = onItemClick,
                    onToggleSelection = ::toggleSelection,
                    onEnterSelection = { selectionMode = true }
                )
            }
        }
    }
}

@Composable
private fun FabMenuAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(12.dp))
        FloatingActionButton(
            onClick = onClick,
            containerColor = FabDark,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}

private fun LazyListScope.vaultSection(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    items: List<VaultItem>,
    selectedItems: Set<VaultItem>,
    selectionMode: Boolean,
    onItemClick: (VaultItem) -> Unit,
    onToggleSelection: (VaultItem) -> Unit,
    onEnterSelection: () -> Unit
) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$count item(ns)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
    if (expanded) {
        items(items.sortedBy { it.title.lowercase() }) { item ->
            val isSelected = selectedItems.contains(item)
            VaultItemRow(
                item = item,
                isSelected = isSelected,
                onClick = {
                    if (selectionMode) {
                        onToggleSelection(item)
                    } else {
                        onItemClick(item)
                    }
                },
                onLongClick = {
                    onEnterSelection()
                    onToggleSelection(item)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultItemRow(
    item: VaultItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val cleanUrl = item.websiteUrl.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .removePrefix("www.")
        val iconUrl = if (cleanUrl.isNotEmpty()) {
            "https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://$cleanUrl&size=128"
        } else {
            null
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            } else if (item.type == "CARD") {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.Black, modifier = Modifier.padding(10.dp))
            } else if (item.type == "ADDRESS") {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color.Black, modifier = Modifier.padding(10.dp))
            } else if (iconUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(iconUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = item.title.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = item.username.ifEmpty { item.websiteUrl }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
