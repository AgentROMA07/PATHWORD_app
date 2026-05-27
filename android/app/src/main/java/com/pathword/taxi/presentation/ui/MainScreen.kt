package com.pathword.taxi.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pathword.taxi.map.IMapProvider

@Composable
fun MainScreen(mapProvider: IMapProvider) {
    var role by remember { mutableStateOf<String?>(null) }

    when (role) {
        "Passenger" -> PassengerScreen(mapProvider = mapProvider, onBack = { role = null })
        "Driver" -> DriverScreen(mapProvider = mapProvider, onBack = { role = null })
        else -> RoleSelectionScreen(onRoleSelected = { role = it })
    }
}

@Composable
fun RoleSelectionScreen(onRoleSelected: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { onRoleSelected("Passenger") }, modifier = Modifier.padding(16.dp)) {
            Text("Passenger Mode")
        }
        Button(onClick = { onRoleSelected("Driver") }, modifier = Modifier.padding(16.dp)) {
            Text("Driver Mode")
        }
    }
}
