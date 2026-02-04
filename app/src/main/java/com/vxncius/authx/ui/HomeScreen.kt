package com.vxncius.authx.ui
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.vxncius.authx.data.VaultItem
import com.vxncius.authx.playfairDisplayFont
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
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    var selectedItems by remember { mutableStateOf(setOf<VaultItem>()) }
    var selectionMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val filteredItems = items.filter { 
        it.title.contains(searchQuery, ignoreCase = true) || 
        it.username.contains(searchQuery, ignoreCase = true) ||
        it.websiteUrl.contains(searchQuery, ignoreCase = true)
    }
    val groupedItems = filteredItems.groupBy { 
        when (it.type) {
            "CARD" -> "Cartões"
            "ADDRESS" -> "Locais"
            else -> if (it.totpSecret != null) "Vaults + 2FA" else "Vaults"
        }
    }.toSortedMap(compareBy {
        when (it) {
            "Vaults + 2FA" -> 0
            "Vaults" -> 1
            "Cartões" -> 2
            "Locais" -> 3
            else -> 4
        }
    })
    fun toggleSelection(item: VaultItem) {
        selectedItems = if (selectedItems.contains(item)) {
            selectedItems - item
        } else {
            selectedItems + item
        }
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
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Scaffold(
        modifier = Modifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null
        ) { focusManager.clearFocus() },
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("${selectedItems.size} selecionado(s)") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        IconButton(onClick = { 
                            showDeleteDialog = true
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir Selecionados", tint = Color.Red)
                        }
                        IconButton(onClick = { 
                            selectionMode = false
                            selectedItems = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar")
                        }
                    }
                )
            } else {
                Column {
                    TopAppBar(
                        title = { 
                            Text(
                                "AuthX", 
                                fontFamily = playfairDisplayFont, 
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ) 
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        actions = {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, contentDescription = "Configurações")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Pesquisar nos cofres...") },
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
                Box(modifier = Modifier.padding(bottom = 80.dp)) {
                    FloatingActionButton(
                        onClick = { showFabMenu = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Novo Item")
                    }
                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                         DropdownMenuItem(
                            text = { Text("Novo Item") },
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                            onClick = { 
                                showFabMenu = false
                                onAddClick() 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Novo Cartão") },
                            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                            onClick = { 
                                showFabMenu = false
                                onAddCardClick()
                            }
                        )
                         DropdownMenuItem(
                            text = { Text("Novo Endereço") },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                            onClick = { 
                                showFabMenu = false 
                                onAddAddressClick()
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Gerar Senha Forte") },
                            leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
                            onClick = { 
                                showFabMenu = false
                                onGeneratePasswordClick()
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (filteredItems.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    if (searchQuery.isEmpty()) "Nenhum item ainda. Adicione um!" else "Nenhum item encontrado",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                state = listState
            ) {
                groupedItems.forEach { (header, groupItems) ->
                    item {
                        Text(
                            header,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(groupItems.sortedBy { it.title.lowercase() }) { item ->
                        val isSelected = selectedItems.contains(item)
                        VaultItemRow(
                            item = item,
                            isSelected = isSelected,
                            selectionMode = selectionMode,
                            onClick = { 
                                if (selectionMode) {
                                    toggleSelection(item)
                                } else {
                                    onItemClick(item)
                                }
                            },
                            onLongClick = {
                                selectionMode = true
                                toggleSelection(item)
                            }
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultItemRow(
    item: VaultItem,
    isSelected: Boolean,
    selectionMode: Boolean,
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
        } else null
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
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    item.title.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            val subtitle = item.username.ifEmpty { item.websiteUrl }
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

