package com.vxncius.authx.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vxncius.authx.R

/**
 * Design system do AuthX (ver authx-design-system.md).
 * Escala de cinza pura + 2 acentos semânticos (amber = dado visível,
 * teal = estado seguro). Borda de 1px sutil em vez de sombra.
 *
 * Fontes via Google Fonts Provider: Poppins para o texto comum,
 * Cormorant Garamond apenas para o wordmark "AuthX" e monospace
 * padrão do sistema para dados sensíveis/OTP.
 */
object AuthXColors {
    val BgBase = Color(0xFF000000)
    val BgElevated = Color(0xFF0A0A0A)
    val SurfaceCard = Color(0xFF121212)
    val SurfaceRow = Color(0xFF111111)
    val BorderSubtle = Color(0xFF202020)
    val BorderCard = Color(0xFF232323)
    val IconPlaceholder = Color(0xFF2A2A2A)
    val IconFill = Color(0xFFD8D8D8)
    val IconBg = Color(0xFFEEEEEE)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9C9C9C)
    val TextTertiary = Color(0xFF6B6B6B)
    val AccentAmber = Color(0xFFD9A15C)
    val AccentTeal = Color(0xFF6FBFAE)
    val RingProgress = Color(0xFFFFFFFF)
    val RingTrack = Color(0xFF2A2A2A)
    val DangerRed = Color(0xFFE0524B)
}

object AuthXSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 14.dp
    val Lg = 20.dp
    val Xl = 32.dp
}

object AuthXRadius {
    val Row = 12.dp
    val Card = 16.dp
    val Sheet = 24.dp
    val Icon = 6.dp
}

private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val Poppins = FontFamily(
    Font(GoogleFont("Poppins"), GoogleFontsProvider, FontWeight.Normal),
    Font(GoogleFont("Poppins"), GoogleFontsProvider, FontWeight.Medium),
    Font(GoogleFont("Poppins"), GoogleFontsProvider, FontWeight.SemiBold),
    Font(GoogleFont("Poppins"), GoogleFontsProvider, FontWeight.Bold)
)

val CormorantGaramond = FontFamily(
    Font(GoogleFont("Cormorant Garamond"), GoogleFontsProvider, FontWeight.SemiBold),
    Font(GoogleFont("Cormorant Garamond"), GoogleFontsProvider, FontWeight.Bold)
)

private val AuthXColorScheme = darkColorScheme(
    primary = AuthXColors.TextPrimary,
    onPrimary = AuthXColors.BgBase,
    primaryContainer = AuthXColors.SurfaceCard,
    onPrimaryContainer = AuthXColors.TextPrimary,
    secondary = AuthXColors.TextSecondary,
    onSecondary = AuthXColors.TextPrimary,
    secondaryContainer = AuthXColors.SurfaceRow,
    onSecondaryContainer = AuthXColors.TextPrimary,
    tertiary = AuthXColors.AccentAmber,
    background = AuthXColors.BgBase,
    onBackground = AuthXColors.TextPrimary,
    surface = AuthXColors.SurfaceCard,
    onSurface = AuthXColors.TextPrimary,
    surfaceVariant = AuthXColors.BgElevated,
    onSurfaceVariant = AuthXColors.TextSecondary,
    outline = AuthXColors.BorderCard,
    outlineVariant = AuthXColors.BorderSubtle,
    error = AuthXColors.DangerRed,
    onError = Color.White
)

val AuthXShapes = Shapes(
    extraSmall = RoundedCornerShape(AuthXRadius.Icon),
    small = RoundedCornerShape(AuthXRadius.Row),
    medium = RoundedCornerShape(AuthXRadius.Card),
    large = RoundedCornerShape(AuthXRadius.Sheet),
    extraLarge = RoundedCornerShape(28.dp)
)

private val DefaultTypography = Typography()

val AuthXTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.Medium),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = Poppins, fontWeight = FontWeight.Medium),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = Poppins),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = Poppins),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = Poppins),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.Medium),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.Medium),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = Poppins)
)

val OtpCodeStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 28.sp,
    letterSpacing = 1.5.sp,
    color = AuthXColors.TextPrimary
)

val MaskedValueStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    letterSpacing = 0.6.sp,
    color = AuthXColors.TextTertiary
)

val CardLastDigitsStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    color = AuthXColors.AccentAmber
)

@Composable
fun AuthXMaterialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuthXColorScheme,
        shapes = AuthXShapes,
        typography = AuthXTypography,
        content = content
    )
}
