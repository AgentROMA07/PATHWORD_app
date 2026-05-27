package com.pathword.taxi.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class MapLocation(val lat: Double, val lng: Double)

data class MapMarker(
    val id: String,
    val location: MapLocation,
    val title: String? = null,
    val snippet: String? = null,
    val isDriver: Boolean = false
)

interface IMapProvider {
    @Composable
    fun MapView(
        modifier: Modifier,
        initialLocation: MapLocation,
        markers: List<MapMarker>,
        onMapClick: ((MapLocation) -> Unit)?
    )
}
