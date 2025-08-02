package com.telecommande.ui.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PairingOrPinDialogContent(
    statusText: String,
    isPinEntryVisible: Boolean,
    currentPin: String,
    onPinChange: (String) -> Unit,
    onSubmitPin: () -> Unit,
    onTryPairingAgain: () -> Unit,
    onResetPairing: () -> Unit,
    onShowDiscoveredTvList: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isPinEntryVisible) "Saisir le PIN affiché sur la TV" else "Appairage Requis",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            if (isPinEntryVisible) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = onPinChange,
                    label = { Text("PIN") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmitPin() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = onSubmitPin, modifier = Modifier.fillMaxWidth()) {
                    Text("Envoyer PIN")
                }
            } else {
                Button(onClick = onTryPairingAgain, modifier = Modifier.fillMaxWidth()) {
                    Text("Démarrer l'appairage / Réessayer")
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onResetPairing) {
                    Text("Réinitialiser l'appairage (si bloqué)")
                }
                OutlinedButton(onClick = onShowDiscoveredTvList, modifier = Modifier.fillMaxWidth(0.8f)) {
                    Text("Choisir une autre TV")
                }
            }
        }
    }
}
