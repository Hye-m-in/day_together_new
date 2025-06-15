package com.example.day_together.ui.navigation
import com.example.day_together.R


object Routes {
    const val HOME = "home"
    const val MESSAGE = "message"
    const val GALLERY = "gallery"
    const val SETTINGS = "settings"
}


sealed class BottomNavItem(val route: String, val iconResId: Int, val label: String) {
    object Home : BottomNavItem(Routes.HOME, R.drawable.ic_home, "홈")
    object Message : BottomNavItem(Routes.MESSAGE, R.drawable.ic_message, "메시지")
    object Gallery : BottomNavItem(Routes.GALLERY, R.drawable.ic_gallery, "갤러리")
    object Settings : BottomNavItem(Routes.SETTINGS, R.drawable.ic_settings, "설정")
}