package com.sanket.tools.nexpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sanket.tools.nexpad.utils.LayoutManager

@Composable
fun HomeScreen(navController: NavController, layoutManager: LayoutManager) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .verticalScroll(scrollState)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("NEXPAD", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Text("Next-Gen Controller Platform", color = Color.Gray, fontSize = 16.sp)
        
        Spacer(modifier = Modifier.height(48.dp))

        // Play Button
        Button(
            onClick = { navController.navigate("gamepad") },
            modifier = Modifier.size(280.dp, 80.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("START PLAYING", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MenuCard(
                title = "HUD Editor",
                icon = "🎮",
                onClick = { navController.navigate("editor") }
            )
            MenuCard(
                title = "Settings",
                icon = "⚙️",
                onClick = { navController.navigate("settings") }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Active Layout: ${layoutManager.getActiveProfile().name}", color = Color.LightGray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCard(title: String, icon: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(160.dp, 120.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 40.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
