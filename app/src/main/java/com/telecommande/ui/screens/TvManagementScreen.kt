package com.telecommande.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.telecommande.core.discovery.DiscoveredTv
import com.telecommande.data.model.PairedTvInfo
import com.telecommande.ui.viewmodels.TvManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvManagementScreen(
    viewModel: TvManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val pairedTvs by viewModel.pairedTvs.collectAsState()
    val activeTvIp by viewModel.activeTvIp.collectAsState()

    val discoveredTvsList by viewModel.discoveredTvs.collectAsState()
    val discoveryStatusMessage by viewModel.discoveryStatusMessage.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()

    var showRenameDialogFor by remember { mutableStateOf<PairedTvInfo?>(null) }
    var showDeleteConfirmDialogFor by remember { mutableStateOf<PairedTvInfo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gérer les TVs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (!isDiscovering) {
                    viewModel.startTvDiscovery()
                } else {
                    viewModel.stopTvDiscovery()
                }
            }) {
                if (isDiscovering) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Arrêter la recherche")
                } else {
                    Icon(Icons.Filled.Search, contentDescription = "Rechercher des TVs")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (pairedTvs.isNotEmpty()) {
                Text(
                    "TVs Appairées",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.weight(0.4f)) {
                    items(pairedTvs, key = { it.ipAddress }) { tv ->
                        TvListItem(
                            tvInfo = tv,
                            isActive = tv.ipAddress == activeTvIp,
                            onTvSelected = { selectedTv ->
                                viewModel.setActiveTv(selectedTv)
                                onNavigateBack()
                            },
                            onRenameClicked = { showRenameDialogFor = it },
                            onDeleteClicked = { showDeleteConfirmDialogFor = it }
                        )
                        HorizontalDivider()
                    }
                }
            }

            Text(
                "TVs Découvertes sur le Réseau",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = if (pairedTvs.isNotEmpty()) 24.dp else 16.dp , bottom = 8.dp)
            )
            Text(
                discoveryStatusMessage,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                fontStyle = FontStyle.Italic
            )

            if (isDiscovering && discoveredTvsList.isEmpty() && pairedTvs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Recherche en cours...")
                }
            } else if (!isDiscovering && discoveredTvsList.isEmpty() && pairedTvs.isEmpty()) {
                EmptyState(
                    modifier = Modifier.weight(0.6f),
                    onDiscoverClick = { viewModel.startTvDiscovery() }
                )
            } else {
                LazyColumn(modifier = Modifier.weight(0.6f)) {
                    items(discoveredTvsList, key = { it.serviceName }) { discoveredTv ->
                        val isAlreadyPaired = pairedTvs.any { it.ipAddress == discoveredTv.ipAddress }
                        DiscoveredTvListItem(
                            tv = discoveredTv,
                            isAlreadyPaired = isAlreadyPaired,
                            onTvSelected = { selectedDiscoveredTv ->
                                viewModel.selectDiscoveredTv(selectedDiscoveredTv)
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    showRenameDialogFor?.let { tvToRename ->
        RenameTvDialog(
            tvInfo = tvToRename,
            onDismiss = { showRenameDialogFor = null },
            onConfirm = { newName ->
                viewModel.renameTv(tvToRename, newName)
                showRenameDialogFor = null
            }
        )
    }

    showDeleteConfirmDialogFor?.let { tvToDelete ->
        DeleteTvConfirmDialog(
            tvInfo = tvToDelete,
            onDismiss = { showDeleteConfirmDialogFor = null },
            onConfirm = {
                viewModel.removeTv(tvToDelete)
                showDeleteConfirmDialogFor = null
            }
        )
    }
}

@Composable
fun DiscoveredTvListItem(
    tv: DiscoveredTv,
    isAlreadyPaired: Boolean,
    onTvSelected: (DiscoveredTv) -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(enabled = !isAlreadyPaired) {
            if (!isAlreadyPaired) onTvSelected(tv)
        },
        headlineContent = {
            Text(
                text = tv.friendlyName ?: "TV Découverte",
                color = if (isAlreadyPaired) LocalContentColor.current.copy(alpha = 0.5f) else LocalContentColor.current
            )
        },
        supportingContent = {
            Text(
                tv.ipAddress,
                color = if (isAlreadyPaired) LocalContentColor.current.copy(alpha = 0.5f) else LocalContentColor.current
            )
        },
        leadingContent = {
            Icon(
                Icons.Filled.Tv,
                contentDescription = "TV Découverte Icon",
                tint = if (isAlreadyPaired) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            if (isAlreadyPaired) {
                Text(
                    "Déjà appairée",
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        colors = if (isAlreadyPaired) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) else ListItemDefaults.colors()

    )
}

@Composable
fun TvListItem(
    tvInfo: PairedTvInfo,
    isActive: Boolean,
    onTvSelected: (PairedTvInfo) -> Unit,
    onRenameClicked: (PairedTvInfo) -> Unit,
    onDeleteClicked: (PairedTvInfo) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        modifier = Modifier.clickable { onTvSelected(tvInfo) },
        headlineContent = {
            Text(
                text = tvInfo.name ?: "TV Inconnue",
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        },
        supportingContent = { Text(tvInfo.ipAddress) },
        leadingContent = {
            Icon(
                Icons.Filled.Tv,
                contentDescription = "TV Icon",
                tint = if (isActive) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Text(
                        "Active",
                        color = MaterialTheme.colorScheme.primary,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Renommer") },
                        onClick = {
                            onRenameClicked(tvInfo)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = "Renommer") }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer") },
                        onClick = {
                            onDeleteClicked(tvInfo)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = "Supprimer") }
                    )
                }
            }
        },
        colors = if (isActive) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ListItemDefaults.colors()
    )
}

@Composable
fun RenameTvDialog(
    tvInfo: PairedTvInfo,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember(tvInfo.name) { mutableStateOf(tvInfo.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renommer ${tvInfo.name ?: "TV"}") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nouveau nom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (newName.isNotBlank()) onConfirm(newName) },
                enabled = newName.isNotBlank()
            ) {
                Text("Confirmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun DeleteTvConfirmDialog(
    tvInfo: PairedTvInfo,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supprimer ${tvInfo.name ?: "TV"} ?") },
        text = { Text("Êtes-vous sûr de vouloir supprimer cette TV (${tvInfo.ipAddress}) de la liste des appareils appairés ?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Supprimer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}


@Composable
fun EmptyState(modifier: Modifier = Modifier, onDiscoverClick: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Tv, contentDescription = null, modifier = Modifier.size(128.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Aucune TV appairée.", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Recherchez des TVs sur votre réseau pour commencer.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDiscoverClick) {
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Rechercher des TVs")
        }
    }
}