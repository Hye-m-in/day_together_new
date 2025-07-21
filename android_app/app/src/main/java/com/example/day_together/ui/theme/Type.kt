package com.example.day_together.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


val AppTypography = Typography(

    displayLarge = TextStyle(
        fontFamily = GothicA1,
        fontWeight = FontWeight.Medium,
        fontSize = 70.sp,
        lineHeight = 78.sp,
        letterSpacing = (-0.25).sp
    ),

    headlineLarge = TextStyle(
        fontFamily = GothicA1,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = GothicA1,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),


    titleLarge = TextStyle(
        fontFamily = GothicA1,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    labelLarge = TextStyle(
        fontFamily = GothicA1,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    bodySmall = TextStyle(
        fontFamily = GothicA1,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),


    displayMedium = TextStyle(fontFamily = GothicA1, fontWeight = FontWeight.Black, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontFamily = GothicA1, fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = GothicA1, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = GothicA1, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = GothicA1, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = GothicA1, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = GothicA1, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    labelMedium = TextStyle(fontFamily = GothicA1, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = GothicA1, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
)