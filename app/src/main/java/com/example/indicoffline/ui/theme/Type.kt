package com.example.indicoffline.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.indicoffline.R

val AppFontFamily = FontFamily(
    // Primary: Baloo 2 (Supports Latin and Devanagari/Hindi)
    Font(R.font.baloo2_regular, FontWeight.Normal),
    Font(R.font.baloo2_medium, FontWeight.Medium),
    Font(R.font.baloo2_semibold, FontWeight.SemiBold),
    Font(R.font.baloo2_bold, FontWeight.Bold),
    Font(R.font.baloo2_extrabold, FontWeight.ExtraBold),

    // Fallback: Baloo Tamma 2 (Supports Kannada)
    Font(R.font.balootamma2_regular, FontWeight.Normal),
    Font(R.font.balootamma2_medium, FontWeight.Medium),
    Font(R.font.balootamma2_semibold, FontWeight.SemiBold),
    Font(R.font.balootamma2_bold, FontWeight.Bold),
    Font(R.font.balootamma2_extrabold, FontWeight.ExtraBold),

    // Fallback: Baloo Tammudu 2 (Supports Telugu)
    Font(R.font.balootammudu2_regular, FontWeight.Normal),
    Font(R.font.balootammudu2_medium, FontWeight.Medium),
    Font(R.font.balootammudu2_semibold, FontWeight.SemiBold),
    Font(R.font.balootammudu2_bold, FontWeight.Bold),
    Font(R.font.balootammudu2_extrabold, FontWeight.ExtraBold),

    // Fallback: Baloo Thambi 2 (Supports Tamil)
    Font(R.font.baloothambi2_regular, FontWeight.Normal),
    Font(R.font.baloothambi2_medium, FontWeight.Medium),
    Font(R.font.baloothambi2_semibold, FontWeight.SemiBold),
    Font(R.font.baloothambi2_bold, FontWeight.Bold),
    Font(R.font.baloothambi2_extrabold, FontWeight.ExtraBold)
)

val baseline = Typography()

// Apply the custom font family across all Material 3 text styles
val Typography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = AppFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = AppFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = AppFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = AppFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = AppFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = AppFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = AppFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = AppFontFamily),
    titleSmall = baseline.titleSmall.copy(fontFamily = AppFontFamily),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = AppFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = AppFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = AppFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = AppFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = AppFontFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = AppFontFamily),
)