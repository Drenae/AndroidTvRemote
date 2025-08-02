package com.telecommande.ui.remote

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
// import androidx.compose.foundation.clickable // Utilisé dans DiscoveredTvItem, mais DiscoveredTvListContent sera modifié/supprimé
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
// import androidx.compose.foundation.layout.Row // Utilisé dans DiscoveredTvItem
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size // Pour une icône dans NoTvConfiguredContent
import androidx.compose.foundation.layout.systemBarsPadding
// import androidx.compose.foundation.lazy.LazyColumn // Sera supprimé de cet écran
// import androidx.compose.foundation.lazy.items // Sera supprimé de cet écran
import androidx.compose.material.icons.Icons // Pour NoTvConfiguredContent
import androidx.compose.material.icons.filled.Settings // Pour NoTvConfiguredContent & PowerSection
import androidx.compose.material.icons.filled.TvOff // Pour NoTvConfiguredContent
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
// import androidx.compose.material3.Divider // Utilisé dans DiscoveredTvListContent
import androidx.compose.material3.Icon // Pour NoTvConfiguredContent & PowerSection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton // Pour ConnectionError et PairingOrPin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
// import androidx.compose.runtime.rememberCoroutineScope // Pas utilisé directement ici
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
// import androidx.compose.ui.text.font.FontWeight // Utilisé dans DiscoveredTvItem
// import androidx.compose.ui.unit.dp // Déjà importé
// import androidx.compose.ui.unit.sp // Utilisé dans DiscoveredTvItem
// import androidx.navigation.NavController // Remplacé par onNavigateToTvManagement
// import com.telecommande.AppDestinations // Remplacé par onNavigateToTvManagement
// import com.telecommande.core.discovery.DiscoveredTv // Ne devrait plus être nécessaire ici
import com.telecommande.ui.viewmodels.AppUiState
import com.telecommande.ui.viewmodels.RemoteViewModel
import com.telecommande.ui.remote.sections.DpadSection
import com.telecommande.ui.remote.sections.MainControlsSection
import com.telecommande.ui.remote.sections.MediaSection
import com.telecommande.ui.remote.sections.NavSection
import com.telecommande.ui.remote.sections.PowerSection
import com.telecommande.ui.theme.AppColors

// Mise à jour de OverlayContentType: LOADING_OR_DISCOVERY devient juste LOADING_STATE
private enum class OverlayContentType {
    NONE,
    LOADING_STATE, // Renommé et simplifié
    NEEDS_PAIRING_OR_PIN,
    CONNECTION_ERROR,
    NO_TV_CONFIGURED // Nouvel état pour l'overlay
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun RemoteControlScreen(
    viewModel: RemoteViewModel,
    // navController: NavController, // Remplacé par une lambda spécifique
    onNavigateToTvManagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionStatusText by viewModel.connectionStatus.collectAsState()
    val isPinRequired by viewModel.pinRequired.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    var currentPinInput by remember { mutableStateOf(viewModel.currentPin.value) }

    // Supprimé: la découverte est gérée par TvManagementViewModel/Screen
    // val discoveredTvs by viewModel.discoveredTvs.collectAsState()
    // val discoveryStatusText by viewModel.discoveryStatus.collectAsState()
    val currentTargetTv by viewModel.currentTargetTvInfo.collectAsState()

    LaunchedEffect(viewModel.currentPin.value) {
        if (currentPinInput != viewModel.currentPin.value) {
            currentPinInput = viewModel.currentPin.value
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    // Supprimé: showDiscoveryList n'est plus pertinent ici
    // val showDiscoveryList = remember(uiState, isConnected, discoveredTvs) { ... }

    val overlayContentType = remember(uiState, isConnected, currentTargetTv, isPinRequired) {
        when {
            uiState == AppUiState.NO_TV_CONFIGURED -> OverlayContentType.NO_TV_CONFIGURED
            // Si une TV est ciblée et qu'on est en chargement, appairage ou erreur
            currentTargetTv != null && (uiState == AppUiState.LOADING || uiState == AppUiState.PAIRING || uiState == AppUiState.CONNECTION_ERROR || uiState == AppUiState.PAIRING_NEEDED) -> {
                when (uiState) {
                    AppUiState.LOADING -> OverlayContentType.LOADING_STATE
                    AppUiState.PAIRING -> OverlayContentType.NEEDS_PAIRING_OR_PIN // Inclut les cas avec et sans PIN visible
                    AppUiState.PAIRING_NEEDED -> OverlayContentType.NEEDS_PAIRING_OR_PIN // Peut réutiliser le même UI que PAIRING si pertinent, ou nécessiter son propre contenu
                    AppUiState.CONNECTION_ERROR -> OverlayContentType.CONNECTION_ERROR
                    else -> OverlayContentType.NONE // Ne devrait pas arriver avec la condition externe
                }
            }
            // Si aucune TV n'est ciblée mais qu'on n'est pas dans NO_TV_CONFIGURED (devrait être rare avec la nouvelle logique)
            // Cela pourrait être un état transitoire avant que NO_TV_CONFIGURED ne soit défini.
            // On peut le traiter comme un chargement générique ou NONE.
            currentTargetTv == null && uiState != AppUiState.NO_TV_CONFIGURED && (uiState == AppUiState.LOADING || uiState == AppUiState.PAIRING) -> OverlayContentType.LOADING_STATE

            isConnected && uiState == AppUiState.REMOTE_CONTROL -> OverlayContentType.NONE
            else -> OverlayContentType.NONE
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.darkBackground)
            .systemBarsPadding()
    ) {
        // La télécommande est toujours en arrière-plan
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PowerSection(
                    onPowerClick = { viewModel.sendPowerKeyPress() },
                    isConnected = isConnected,
                    onStatusIndicatorClick = {
                        Log.d("RemoteControlScreen", "Status Indicator Clicked - Navigating to TV Management")
                        onNavigateToTvManagement()
                    },
                    modifier = Modifier
                )
                Spacer(Modifier.height(10.dp))
                DpadSection(
                    onOkClick = { viewModel.sendDpadCenter() },
                    onUpClick = { viewModel.sendDpadUp() },
                    onDownClick = { viewModel.sendDpadDown() },
                    onLeftClick = { viewModel.sendDpadLeft() },
                    onRightClick = { viewModel.sendDpadRight() },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                NavSection(
                    onBackClick = { viewModel.sendBack() },
                    onHomeClick = { viewModel.sendHome() },
                    onKeyboardClick = { /* TODO: Gérer l'affichage du clavier virtuel si nécessaire */ },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                MediaSection(
                    onRewindBackClick = { viewModel.sendRewindBack() },
                    onPlayClick = { viewModel.sendPlay() },
                    onStopClick = { viewModel.sendStop() },
                    onRewindForwardClick = { viewModel.sendRewindForward() },
                    modifier = Modifier
                )
            }
            MainControlsSection(
                onVolumeUpClick = { viewModel.sendVolumeUp() },
                onVolumeDownClick = { viewModel.sendVolumeDown() },
                onMuteClick = { viewModel.sendMute() },
                onChannelUpClick = { viewModel.sendChannelUp() },
                onChannelDownClick = { viewModel.sendChannelDown() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // L'overlay prend le dessus si nécessaire
        AnimatedVisibility(
            visible = overlayContentType != OverlayContentType.NONE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f)), // Fond semi-transparent
                contentAlignment = Alignment.Center
            ) {
                when (overlayContentType) {
                    OverlayContentType.NO_TV_CONFIGURED -> {
                        NoTvConfiguredContent(
                            message = connectionStatusText.ifEmpty { "Aucune TV n'est configurée." },
                            onNavigateToTvManagement = onNavigateToTvManagement
                        )
                    }
                    OverlayContentType.LOADING_STATE -> {
                        // Ce cas est maintenant simplifié: il n'affiche plus la liste des TV découvertes.
                        // Il affiche un simple indicateur de chargement pour la TV actuelle si elle est ciblée.
                        if (currentTargetTv != null || uiState == AppUiState.LOADING) { // Montrer si TV ciblée ou explicitement en chargement
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = connectionStatusText,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        // Si currentTargetTv est null et uiState n'est pas LOADING, cet overlay
                        // ne devrait pas être LOADING_STATE grâce à la logique de `overlayContentType`.
                        // S'il l'est quand même, c'est un état non désiré ou transitoire.
                        // L'état NO_TV_CONFIGURED devrait prendre le relais.
                    }
                    OverlayContentType.NEEDS_PAIRING_OR_PIN -> {
                        // Ce composant doit être ajusté car il n'y a plus de "onShowDiscoveredTvList"
                        PairingOrPinDialogContent(
                            statusText = connectionStatusText,
                            isPinEntryVisible = isPinRequired,
                            currentPin = currentPinInput,
                            onPinChange = { currentPinInput = it },
                            onSubmitPin = {
                                viewModel.updateCurrentPin(currentPinInput)
                                viewModel.submitPin()
                                keyboardController?.hide()
                            },
                            onTryPairingAgain = { viewModel.retryConnectionOrInitiatePairing() },
                            onResetPairing = { viewModel.deleteKeystoreAndReInitiatePairing() },
                            // onShowDiscoveredTvList = { ... } // Supprimé
                            onGoToTvManagement = onNavigateToTvManagement // Option pour changer de TV
                        )
                    }
                    OverlayContentType.CONNECTION_ERROR -> {
                        // Ce composant doit être ajusté car il n'y a plus de "onShowDiscoveredTvList"
                        ConnectionErrorContent(
                            statusText = connectionStatusText,
                            onRetry = { viewModel.retryConnectionOrInitiatePairing() },
                            onResetPairing = { viewModel.deleteKeystoreAndReInitiatePairing() },
                            // onShowDiscoveredTvList = { ... } // Supprimé
                            onGoToTvManagement = onNavigateToTvManagement // Option pour changer de TV
                        )
                    }
                    OverlayContentType.NONE -> { /* Ne rien faire */ }
                }
            }
        }
    }
}

@Composable
fun NoTvConfiguredContent(
    message: String,
    onNavigateToTvManagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.TvOff,
            contentDescription = "Aucune TV configurée",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Veuillez aller dans la gestion des TVs pour en rechercher et appairer une.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateToTvManagement) {
            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Gérer les TVs")
        }
    }
}

@Composable
fun PairingOrPinDialogContent(
    statusText: String,
    isPinEntryVisible: Boolean,
    currentPin: String,
    onPinChange: (String) -> Unit,
    onSubmitPin: () -> Unit,
    onTryPairingAgain: () -> Unit,
    onResetPairing: () -> Unit,
    onGoToTvManagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(statusText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))

        if (isPinEntryVisible) {
            OutlinedTextField(
                value = currentPin,
                onValueChange = onPinChange,
                label = { Text("Code PIN",  color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface)
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSubmitPin) { Text("Soumettre PIN") }
        } else if (statusText.contains("Appairage en cours", ignoreCase = true) || statusText.contains("PIN requis", ignoreCase = true)) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Veuillez confirmer sur votre TV si nécessaire.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onTryPairingAgain) { Text("Réessayer l'appairage/connexion") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onResetPairing) { Text("Réinitialiser l'appairage pour cette TV", color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onGoToTvManagement) { Text("Choisir une autre TV / Gérer les TVs") }
    }
}

@Composable
fun ConnectionErrorContent(
    statusText: String,
    onRetry: () -> Unit,
    onResetPairing: () -> Unit,
    onGoToTvManagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Erreur de Connexion", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(statusText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Réessayer la connexion") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onResetPairing) { Text("Oublier cette TV et ré-appairer", color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onGoToTvManagement) { Text("Gérer les TVs / Choisir une autre") }
    }
}