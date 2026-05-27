package com.pathword.taxi.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

class GoogleMapProvider : IMapProvider {
    @Composable
    override fun MapView(
        modifier: Modifier,
        initialLocation: MapLocation,
        markers: List<MapMarker>,
        onMapClick: ((MapLocation) -> Unit)?
    ) {
        val almaty = LatLng(initialLocation.lat, initialLocation.lng)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(almaty, 12f)
        }

        GoogleMap(
            modifier = modifier,
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                onMapClick?.invoke(MapLocation(latLng.latitude, latLng.longitude))
            }
        ) {
            markers.forEach { mapMarker ->
                Marker(
                    state = MarkerState(position = LatLng(mapMarker.location.lat, mapMarker.location.lng)),
                    title = mapMarker.title,
                    snippet = mapMarker.snippet
                )
            }
        }
    }
}
