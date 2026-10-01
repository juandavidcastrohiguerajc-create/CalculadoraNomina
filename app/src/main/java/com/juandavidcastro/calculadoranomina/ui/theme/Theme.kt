package com.juandavidcastro.calculadoranomina.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = AzulTurquesa,
    onPrimary = Color.White,
    primaryContainer = AzulTurquesaClaro,
    background = FondoBlanco,
    surface = FondoBlanco,
    onBackground = TextoOscuro,
    onSurface = TextoOscuro
)

private val EsquemaOscuro = darkColorScheme(
    primary = AzulTurquesaNoche,
    onPrimary = Color(0xFF003548),
    background = FondoNoche,
    surface = FondoNoche,
    onBackground = TextoClaro,
    onSurface = TextoClaro
)

@Composable
fun CalculadoraNominaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Typography,
        content = content
    )
}
