package com.telecommande.ui.remote.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box // Box est plus flexible pour superposer des effets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton // On utilise toujours IconButton pour la sémantique et le ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
// import androidx.compose.ui.graphics.graphicsLayer // Peut être utile pour des effets plus complexes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Supposons que shadowCompat ressemble à quelque chose comme ça (simplifié)
// Si vous avez un composable shadowCompat, vous devrez peut-être ajuster son comportement
// ou le remplacer par la gestion d'ombre standard pour l'état pressé.
// Pour cet exemple, nous allons utiliser Modifier.shadow() directement.

@Composable
fun RemoteButton(
    onClick: () -> Unit,
    @DrawableRes iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 74.dp,
    defaultBackgroundColorBrush: Brush = Brush.linearGradient(colors = listOf(Color(0xFF2C2C2E), Color(0xFF161616))),
    pressedBackgroundColorBrush: Brush = Brush.linearGradient(colors = listOf(Color(0xFF1F1F1F), Color(0xFF282828))), // Légèrement plus clair ou différent pour l'appui
    shape: Shape = CircleShape,
    iconTint: Color = Color.Unspecified,
    iconPadding: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color(0xFF161616),
    defaultElevation: Dp = 8.dp, // Ombre "relief"
    pressedElevation: Dp = 2.dp,   // Ombre moins prononcée ou ajustée pour l'effet incrusté
    shadowColorLight: Color = Color(0x66FFFFFF), // Couleur claire pour l'ombre du haut (relief)
    shadowColorDark: Color = Color(0x66000000),   // Couleur sombre pour l'ombre du bas (relief)
    pressedShadowColorLight: Color = Color(0x66000000), // Couleur sombre pour l'ombre du haut (incrusté)
    pressedShadowColorDark: Color = Color(0x33FFFFFF)    // Couleur claire pour l'ombre du bas (incrusté)

) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentElevation = if (isPressed) pressedElevation else defaultElevation
    val currentShadowColorLight = if (isPressed) pressedShadowColorLight else shadowColorLight
    val currentShadowColorDark = if (isPressed) pressedShadowColorDark else shadowColorDark
    val currentBackgroundBrush = if (isPressed) pressedBackgroundColorBrush else defaultBackgroundColorBrush
    val iconSize = size - (iconPadding * 2)
    val pressedIconSize = iconSize - 2.dp // Optionnel: rétrécir légèrement l'icône

    // Le IconButton gère le clic et l'état d'interaction
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size), // Le IconButton prend la taille globale pour la zone de clic
        interactionSource = interactionSource
    ) {
        // Box pour superposer les ombres et le fond
        // L'ordre des modificateurs est important pour les ombres
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size) // La Box interne a aussi la taille du bouton
                // Ombre externe (simule le bord sombre de l'effet "relief" ou le haut de "l'incrustation")
                .shadow(
                    elevation = currentElevation,
                    shape = shape,
                    spotColor = currentShadowColorDark, // Utilisé pour l'ombre principale
                    ambientColor = currentShadowColorDark // Utilisé pour l'ombre ambiante
                )
                // Ombre interne (simule le bord clair de l'effet "relief" ou le bas de "l'incrustation")
                // Pour un véritable effet d'ombre interne, des techniques plus avancées avec Canvas sont nécessaires.
                // Ici, nous simulons avec une deuxième ombre décalée ou une couleur de spot différente.
                // Pour simplifier, nous utilisons une seule ombre et jouons avec spotColor/ambientColor,
                // ou nous pourrions utiliser deux modificateurs .shadow distincts avec des décalages (plus complexe).
                // Alternative pour l'effet "incrusté":
                // L'ombre principale devient sombre en haut et une "lueur" claire en bas.
                // L'ombre "relief": ombre claire en haut, ombre sombre en bas.
                // Pour l'effet incrusté, on inverse souvent cela, ou on a une ombre globale plus douce
                // et on joue sur le dégradé du fond.

                // Solution simplifiée avec un seul `shadow` par souci de clarté,
                // en ajustant les couleurs de spotColor et ambientColor.
                // Pour un contrôle plus fin des ombres "neumorphiques" (haut/bas distincts),
                // il faudrait dessiner manuellement ou utiliser des bibliothèques dédiées.

                .background(brush = currentBackgroundBrush, shape = shape)
                .border(width = borderWidth, color = borderColor, shape = shape)
            // Le padding pour l'icône doit être appliqué ici, à l'intérieur de la Box qui a le fond et la bordure
            // Mais l'icône elle-même est un enfant, donc on ajuste sa taille.
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(if (isPressed) pressedIconSize else iconSize) // Taille de l'icône
            )
        }
    }
}
