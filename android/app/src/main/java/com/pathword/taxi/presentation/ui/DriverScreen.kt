package com.pathword.taxi.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pathword.taxi.map.IMapProvider
import com.pathword.taxi.map.MapLocation
import com.pathword.taxi.map.MapMarker
import com.pathword.taxi.presentation.viewmodel.DriverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(
    mapProvider: IMapProvider,
    onBack: () -> Unit,
    viewModel: DriverViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val markers = mutableListOf<MapMarker>()
    markers.add(
        MapMarker(
            "driver",
            MapLocation(state.currentLocation.lat, state.currentLocation.lng),
            "Me",
            isDriver = true
        )
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Driver Mode") },
                navigationIcon = { Button(onClick = onBack) { Text("Back") } },
                actions = {
                    Switch(
                        checked = state.isOnline,
                        onCheckedChange = { viewModel.toggleOnline() }
                    )
                    Text(if (state.isOnline) "Online" else "Offline", modifier = Modifier.padding(start = 8.dp))
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            mapProvider.MapView(
                modifier = Modifier.fillMaxSize(),
                initialLocation = MapLocation(state.currentLocation.lat, state.currentLocation.lng),
                markers = markers,
                onMapClick = null
            )

            if (state.activeRide != null) {
                Card(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active Ride", style = MaterialTheme.typography.titleLarge)
                        Text("Order ID: ${state.activeRide!!.orderId}")
                        Text("Price: ${state.activeRide!!.price}")
                    }
                }
            } else if (state.incomingOrders.isNotEmpty()) {
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Incoming Orders", style = MaterialTheme.typography.titleMedium)
                        LazyColumn {
                            items(state.incomingOrders) { order ->
                                var suggestedPrice by remember { mutableStateOf(order.price) }
                                Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                                    Text("Passenger ${order.passengerId} to ${order.destination.lat}, ${order.destination.lng}")
                                    Text("Price: ${order.price}")

                                    Button(
                                        onClick = { viewModel.sendBid(order.id, order.price) },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text("Accept")
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = suggestedPrice,
                                            onValueChange = { suggestedPrice = it },
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                        Button(onClick = { viewModel.sendBid(order.id, suggestedPrice) }) {
                                            Text("Counter-offer")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
