package com.edu.minlish.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
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

data class TabItem(val screen: Screen, val icon: ImageVector, val label: String)

@Composable
fun MinLishBottomNav(
    currentRoute: String?,
    onTabClick: (Screen) -> Unit
) {
    val tabs = listOf(
        TabItem(Screen.Home, Icons.Default.Home, "Home"),
        TabItem(Screen.Library, Icons.Default.Book, "Library"),
        TabItem(Screen.Stats, Icons.Default.TrendingUp, "Stats"),
        TabItem(Screen.PersonalProfile, Icons.Default.Person, "Profile")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        HorizontalDivider(color = Border, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isActive = currentRoute == tab.screen.route
                
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
                        tint = if (isActive) Color(0xFF111111) else Color(0xFFAAAAAA),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = if (isActive) Color(0xFF111111) else Color(0xFFAAAAAA),
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
