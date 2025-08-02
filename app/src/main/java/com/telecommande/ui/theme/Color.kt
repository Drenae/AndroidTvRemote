package com.telecommande.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val AppBackgroundColor = Color(0xFF1A1A1A)
// val ShadowColor = Color.Black // Pas directement utilisé avec Modifier.shadow, mais bonne référence

// Couleurs pour les boutons ronds et les boutons d'application rectangulaires
val ButtonBackgroundDark = Color(0xFF2A2A2A)
val ButtonBorderDark = Color(0xFF3A3A3A)


// Couleurs pour les boutons rectangulaires avec texte (Retour, Sources)
val AppButtonTextColor = Color.White // #FFFFFF

val DarkBackground = Color(0xFF1C1C1E)
val GradientStart = Color(0xFF2C2C2E)
val GradientEnd = Color(0xFF161616)
val GradientAppStart = Color(0xFF3A3A3C)
val GradientAppEnd = Color(0xFF2C2C2E)
val White = Color.White // Color(0xFFFFFFFF) - déjà défini dans androidx.compose.ui.graphics
val Black = Color.Black // Color(0xFF000000) - déjà défini
val Red = Color(0xFFFF0000) // Pourrait être Color.Red
val Green = Color(0xFF00FF00) // Pourrait être Color.Green
val PastelGreen = Color(0xFF98FB98)

// Vous pouvez également les regrouper dans un objet si vous préférez
object AppColors {
    val darkBackground = Color(0xFF1C1C1E)
    val gradientStart = Color(0xFF2C2C2E)
    val gradientEnd = Color(0xFF161616)
    val gradientAppStart = Color(0xFF3A3A3C)
    val gradientAppEnd = Color(0xFF2C2C2E)
    val appWhite = Color.White
    val appBlack = Color.Black
    val statusRed = Color.Red
    val statusGreen = Color.Green
    val accent = PastelGreen
}