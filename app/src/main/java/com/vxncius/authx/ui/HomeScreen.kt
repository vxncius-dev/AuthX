package com.vxncius.authx.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.vxncius.authx.data.VaultItem
import com.vxncius.authx.logic.TotpManager
import com.vxncius.authx.ui.theme.AuthXColors
import com.vxncius.authx.ui.theme.AuthXRadius
import com.vxncius.authx.ui.theme.CardLastDigitsStyle
import com.vxncius.authx.ui.theme.MaskedValueStyle
import com.vxncius.authx.ui.theme.OtpCodeStyle
import com.vxncius.authx.ui.theme.Poppins
import kotlinx.coroutines.delay

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

    val searchedItems = remember(searchQuery, items) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            items
        } else {
            items.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.username.contains(query, ignoreCase = true) ||
                    it.websiteUrl.contains(query, ignoreCase = true)
            }
        }
    }
    val passwordItems = remember(searchedItems) {
        searchedItems.filter { it.type != "CARD" && it.type != "ADDRESS" }.sortedBy { it.title.lowercase() }
    }
    val cardItems = remember(searchedItems) {
        searchedItems.filter { it.type == "CARD" }.sortedBy { it.title.lowercase() }
    }
    val placeItems = remember(searchedItems) {
        searchedItems.filter { it.type == "ADDRESS" }.sortedBy { it.title.lowercase() }
    }
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
                            color = AuthXColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    },
                    actions = {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir selecionados", tint = AuthXColors.DangerRed)
                        }
                        IconButton(onClick = {
                            selectionMode = false
                            selectedItems = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = AuthXColors.TextPrimary)
                        }
                    }
                )
            } else {
                Column {
                    AuthXHeaderRow(
                        topPadding = 8.dp,
                        bottomPadding = 8.dp,
                        title = {
                            Text(
                                text = "AuthX",
                                color = AuthXColors.TextPrimary,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 34.sp),
                                fontFamily = Poppins,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        },
                        actions = {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Menu, contentDescription = "Abrir menu", tint = AuthXColors.TextPrimary)
                            }
                        }
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        placeholder = { Text("Pesquisar", color = AuthXColors.TextTertiary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AuthXColors.TextTertiary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar", tint = AuthXColors.TextSecondary)
                                }
                            }
                        },
                        shape = RoundedCornerShape(AuthXRadius.Row),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = AuthXColors.BorderSubtle,
                            focusedBorderColor = AuthXColors.BorderCard,
                            unfocusedContainerColor = AuthXColors.BgElevated,
                            focusedContainerColor = AuthXColors.BgElevated,
                            cursorColor = AuthXColors.TextPrimary
                        ),
                        singleLine = true
                    )
                    TotpCarousel(items = items, onItemClick = onItemClick)
                    if (filteredItems.isNotEmpty()) {
                        SectionHeader("Senhas", passwordItems.size, passwordsExpanded) { passwordsExpanded = !passwordsExpanded }
                    }
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
                    AnimatedVisibility(
                        visible = showFabMenu,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
                    }
                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = AuthXColors.TextPrimary,
                        contentColor = AuthXColors.BgBase,
                        shape = RoundedCornerShape(AuthXRadius.Row),
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
                    color = AuthXColors.TextTertiary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                state = listState
            ) {
                itemsSection(
                    expanded = passwordsExpanded,
                    items = passwordItems,
                    selectedItems = selectedItems,
                    selectionMode = selectionMode,
                    onItemClick = onItemClick,
                    onToggleSelection = ::toggleSelection,
                    onEnterSelection = { selectionMode = true }
                )
                section(
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
                section(
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
    val pillShape = RoundedCornerShape(AuthXRadius.Row)
    Row(
        modifier = Modifier.padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .clip(pillShape)
                .background(AuthXColors.SurfaceRow)
                .border(1.dp, AuthXColors.BorderSubtle, pillShape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                color = AuthXColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = Poppins,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.width(8.dp))
        FloatingActionButton(
            onClick = onClick,
            containerColor = AuthXColors.SurfaceCard,
            contentColor = AuthXColors.TextPrimary,
            shape = RoundedCornerShape(AuthXRadius.Row),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthXColors.BgBase)
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Poppins,
                color = AuthXColors.TextPrimary
            )
            Text(
                text = "$count item(ns)",
                style = MaterialTheme.typography.bodySmall,
                color = AuthXColors.TextTertiary
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = AuthXColors.TextSecondary
        )
    }
}

private fun LazyListScope.itemsSection(
    expanded: Boolean,
    items: List<VaultItem>,
    selectedItems: Set<VaultItem>,
    selectionMode: Boolean,
    onItemClick: (VaultItem) -> Unit,
    onToggleSelection: (VaultItem) -> Unit,
    onEnterSelection: () -> Unit
) {
    if (expanded) {
        items(items) { item ->
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

private fun LazyListScope.section(
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
        SectionHeader(title, count, expanded, onToggle)
    }
    if (expanded) {
        items(items) { item ->
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
    val rowShape = RoundedCornerShape(AuthXRadius.Row)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(rowShape)
            .background(
                if (isSelected) AuthXColors.TextPrimary.copy(alpha = 0.1f)
                else AuthXColors.SurfaceRow
            )
            .border(
                width = 1.dp,
                color = if (isSelected) AuthXColors.TextPrimary.copy(alpha = 0.4f)
                else AuthXColors.BorderSubtle,
                shape = rowShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                .size(40.dp)
                .clip(RoundedCornerShape(AuthXRadius.Icon))
                .background(
                    when {
                        isSelected -> AuthXColors.TextPrimary.copy(alpha = 0.2f)
                        iconUrl != null -> AuthXColors.IconBg
                        else -> AuthXColors.IconPlaceholder
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = AuthXColors.TextPrimary)
            } else if (item.type == "CARD") {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = AuthXColors.IconFill, modifier = Modifier.padding(9.dp))
            } else if (item.type == "ADDRESS") {
                Icon(Icons.Default.Home, contentDescription = null, tint = AuthXColors.IconFill, modifier = Modifier.padding(9.dp))
            } else if (iconUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(iconUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(9.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = item.title.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = Poppins,
                    color = AuthXColors.IconFill
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Poppins,
                color = AuthXColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = item.username.ifEmpty { item.websiteUrl }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuthXColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        VaultItemRowState(item)
    }
}

@Composable
private fun VaultItemRowState(item: VaultItem) {
    when (item.type) {
        "CARD" -> {
            val last4 = item.password.takeLast(4)
            if (last4.isNotEmpty()) {
                Text(
                    text = "•••• $last4",
                    style = CardLastDigitsStyle
                )
            }
        }
        "ADDRESS" -> Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = AuthXColors.AccentTeal,
            modifier = Modifier.size(18.dp)
        )
        else -> Text(
            text = "••••••",
            style = MaskedValueStyle
        )
    }
}

@Composable
private fun TotpCarousel(
    items: List<VaultItem>,
    onItemClick: (VaultItem) -> Unit
) {
    val totpItems = remember(items) { items.filter { !it.totpSecret.isNullOrBlank() } }
    if (totpItems.isEmpty()) return
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(
            text = "Códigos 2FA",
            style = MaterialTheme.typography.titleSmall,
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            color = AuthXColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(totpItems) { item ->
                key(item.id, tick) {
                    TotpCard(item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

@Composable
private fun TotpCard(item: VaultItem, onClick: () -> Unit) {
    val period = if (item.period > 0) item.period else 30
    val remaining = TotpManager.getTimeRemaining(period)
    val code = runCatching {
        TotpManager.generateCode(item.totpSecret!!, item.algorithm, item.digits, period)
    }.getOrDefault("")
    val shape = RoundedCornerShape(AuthXRadius.Card)
    Row(
        modifier = Modifier
            .width(172.dp)
            .clip(shape)
            .background(AuthXColors.SurfaceCard)
            .border(1.dp, AuthXColors.BorderCard, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                color = AuthXColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = code.chunked(3).joinToString(" "),
                style = OtpCodeStyle.copy(fontSize = 16.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        TotpRing(remainingSeconds = remaining, period = period, modifier = Modifier.size(44.dp))
    }
}
