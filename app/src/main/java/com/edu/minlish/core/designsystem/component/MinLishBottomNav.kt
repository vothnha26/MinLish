package com.edu.minlish.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.navigation.Screen
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.theme.Primary

data class TabItem(val screen: Screen, val icon: ImageVector, val label: String)

@Composable
fun MinLishBottomNav(
    currentRoute: String?,
    onTabClick: (Screen) -> Unit
) {
    val items = listOf(
        TabItem(Screen.Home, Icons.Default.Home, "Home"),
        TabItem(Screen.Stats, Icons.AutoMirrored.Filled.TrendingUp, "Stats"),
        TabItem(Screen.Library, Icons.Default.Book, "Library"),
        TabItem(Screen.PersonalProfile, Icons.Default.Person, "Profile")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = Border, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { tab ->
                val isActive = when (tab.screen) {
                    Screen.Home -> currentRoute == Screen.Home.route
                    Screen.Stats -> currentRoute == Screen.Stats.route
                    Screen.PersonalProfile -> currentRoute == Screen.PersonalProfile.route || currentRoute == Screen.Settings.route
                    Screen.Library -> {
                        currentRoute == Screen.Library.route ||
                        currentRoute?.startsWith("word_list/") == true ||
                        currentRoute?.startsWith("word_detail/") == true ||
                        currentRoute?.startsWith("add_word/") == true ||
                        currentRoute?.startsWith("edit_word/") == true ||
                        currentRoute == Screen.TranslateAndLookup.route ||
                        currentRoute == Screen.CreateWordSet.route ||
                        currentRoute == Screen.AICreateWordSet.route
                    }
                    else -> false
                }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabClick(tab.screen) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isActive) Primary else Color(0xFFAAAAAA),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = if (isActive) Primary else Color(0xFFAAAAAA),
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Primary, shape = androidx.compose.foundation.shape.CircleShape)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp)) // Maintain height layout consistency
                    }
                }
            }
        }
    }
}
