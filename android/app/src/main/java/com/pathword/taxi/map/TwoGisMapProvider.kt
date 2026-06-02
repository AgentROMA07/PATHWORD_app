package com.pathword.taxi.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import ru.dgis.sdk.map.MapView
import ru.dgis.sdk.map.Map as DGisMap
import ru.dgis.sdk.map.MapOptions
import ru.dgis.sdk.map.CameraPosition
import ru.dgis.sdk.map.MapObjectManager
import ru.dgis.sdk.map.Marker
import ru.dgis.sdk.map.MarkerOptions
import ru.dgis.sdk.map.Polyline
import ru.dgis.sdk.map.PolylineOptions
import ru.dgis.sdk.coordinates.GeoPoint
import ru.dgis.sdk.map.LogicalPixel
import ru.dgis.sdk.map.lpx
import ru.dgis.sdk.map.Zoom
import ru.dgis.sdk.map.imageFromResource
import ru.dgis.sdk.DGis

class TwoGisMapProvider : IMapProvider {
    @Composable
    override fun MapView(
        modifier: Modifier,
        initialLocation: MapLocation,
        markers: List<MapMarker>,
        onMapClick: ((MapLocation) -> Unit)?
    ) {
        var mapObjectManager by remember { mutableStateOf<MapObjectManager?>(null) }
        var map by remember { mutableStateOf<DGisMap?>(null) }

        AndroidView(
            factory = { context ->
                val sdkContext = DGis.initialize(context)
                val mapView = MapView(context)

                val options = MapOptions().apply {
                    position = CameraPosition(
                        point = GeoPoint(latitude = initialLocation.lat, longitude = initialLocation.lng),
                        zoom = Zoom(12.0f)
                    )
                }

                mapView.getMapAsync { dgisMap ->
                    map = dgisMap
                    mapObjectManager = MapObjectManager(dgisMap)
                }
                mapView
            },
            modifier = modifier,
            update = { view ->
                val manager = mapObjectManager ?: return@AndroidView
                val dgisMap = map ?: return@AndroidView
                val context = DGis.context()

                manager.removeAll()

                var hasDestination = false

                markers.forEach { marker ->
                    val point = GeoPoint(latitude = marker.location.lat, longitude = marker.location.lng)
                    if (marker.id == "dest") {
                        hasDestination = true
                    }

                    // We can handle rotation/animation manually via coroutines or ValueAnimator on the view update side
                    // updating position of a stored reference, but for now we re-create the marker representing state
                    manager.addObject(
                        Marker(
                            MarkerOptions(
                                position = ru.dgis.sdk.geometry.GeoPointWithElevation(point.latitude, point.longitude),
                                text = marker.title ?: "",
                                icon = imageFromResource(context, android.R.drawable.ic_menu_myplaces)
                            )
                        )
                    )
                }

                if (hasDestination) {
                    val pLoc = markers.firstOrNull { it.id == "passenger" }?.location ?: initialLocation
                    val dLoc = markers.firstOrNull { it.id == "dest" }?.location
                    if (dLoc != null) {
                        // Drawing line
                        val points = listOf(
                            GeoPoint(latitude = pLoc.lat, longitude = pLoc.lng),
                            GeoPoint(latitude = (pLoc.lat + dLoc.lat) / 2 - 0.01, longitude = (pLoc.lng + dLoc.lng) / 2 + 0.01),
                            GeoPoint(latitude = dLoc.lat, longitude = dLoc.lng)
                        )
                        manager.addObject(
                            Polyline(
                                PolylineOptions(
                                    points = points,
                                    width = 5.lpx
                                )
                            )
                        )
                    }
                }
            }
        )
    }
}
