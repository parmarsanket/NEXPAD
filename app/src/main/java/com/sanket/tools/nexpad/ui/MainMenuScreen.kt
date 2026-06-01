package com.sanket.tools.nexpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.utils.LayoutManager

@Composable
fun MainMenuScreen(navController: NavController, layoutManager: LayoutManager) {
    var activeProfile by remember { mutableStateOf(layoutManager.getActiveProfile()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("NEXPAD HUD EDITOR", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Active Layout: ${activeProfile.name}", color = Color.Gray, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { 
                activeProfile = LayoutProfile(name = "Standard")
                layoutManager.saveProfile(activeProfile)
            }) { Text("Standard") }
            
            Button(onClick = { 
                activeProfile = LayoutProfile(name = "Racing")
                layoutManager.saveProfile(activeProfile)
            }) { Text("Racing") }
            
            Button(onClick = { 
                activeProfile = LayoutProfile(name = "Simulation")
                layoutManager.saveProfile(activeProfile)
            }) { Text("Simulation") }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("RGB Lighting", color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = activeProfile.isRgbEnabled,
                onCheckedChange = { enabled -> 
                    activeProfile = activeProfile.copy(isRgbEnabled = enabled)
                    layoutManager.saveProfile(activeProfile)
                }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { navController.navigate("hud_editor") },
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Text("Customize HUD Placement")
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("gamepad") },
            modifier = Modifier.size(200.dp, 60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("START PLAYING", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
