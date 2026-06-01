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
import com.pathword.taxi.domain.model.Location
import com.pathword.taxi.map.IMapProvider
import com.pathword.taxi.map.MapLocation
import com.pathword.taxi.map.MapMarker
import com.pathword.taxi.presentation.viewmodel.PassengerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerScreen(
    mapProvider: IMapProvider,
    onBack: () -> Unit,
    viewModel: PassengerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.connect()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnect()
        }
    }

    val markers = mutableListOf<MapMarker>()
    // Add current passenger location (mocked)
    markers.add(MapMarker("passenger", MapLocation(43.238949, 76.889709), "Me"))

    state.destination?.let { dest ->
        markers.add(MapMarker("dest", MapLocation(dest.lat, dest.lng), "Destination"))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Passenger Mode") }, navigationIcon = {
                Button(onClick = onBack) { Text("Back") }
            })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            mapProvider.MapView(
                modifier = Modifier.fillMaxSize(),
                initialLocation = MapLocation(43.238949, 76.889709), // Almaty center
                markers = markers,
                onMapClick = { loc ->
                    if (!state.rideStarted) {
                        viewModel.setDestination(Location(loc.lat, loc.lng))
                    }
                }
            )

            // ETA Card if destination is selected
            if (state.destination != null && !state.rideStarted) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                ) {
                    Text(
                        "Estimated Time: 15 mins",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (state.rideStarted) {
                Card(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ride Started!", style = MaterialTheme.typography.titleLarge)
                        Text("Driver ID: ${state.acceptedBid?.driverId}")
                        Text("Price: ${state.acceptedBid?.price}")
                    }
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                ) {
                    if (state.destination != null && state.bids.isEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = state.suggestedPrice,
                                    onValueChange = { viewModel.setPrice(it) },
                                    label = { Text("Suggest Price (e.g. 1000 KZT)") }
                                )
                                Button(
                                    onClick = { viewModel.createOrder() },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Request Ride")
                                }
                            }
                        }
                    }

                    if (state.bids.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Incoming Bids", style = MaterialTheme.typography.titleMedium)
                                LazyColumn {
                                    items(state.bids) { bid ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Driver ${bid.driverId}: ${bid.price}")
                                            Button(onClick = { viewModel.acceptBid(bid) }) {
                                                Text("Accept")
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
}
