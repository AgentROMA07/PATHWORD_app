package com.pathword.taxi.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ru.dgis.sdk.geometry.GeoPointWithElevation
import ru.dgis.sdk.map.LogicalPixel
import ru.dgis.sdk.map.lpx
import ru.dgis.sdk.map.Zoom
import ru.dgis.sdk.map.imageFromResource
import ru.dgis.sdk.DGis
import com.pathword.taxi.domain.repository.IRoutingRepository
import com.pathword.taxi.domain.model.Location
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

class Ref<T>(var value: T)

class TwoGisMapProvider(private val routingRepository: IRoutingRepository) : IMapProvider {
    @Composable
    override fun MapView(
        modifier: Modifier,
        initialLocation: MapLocation,
        markers: List<MapMarker>,
        onMapClick: ((MapLocation) -> Unit)?
    ) {
        var mapObjectManager by remember { mutableStateOf<MapObjectManager?>(null) }
        var map by remember { mutableStateOf<DGisMap?>(null) }

        var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
        val markerMap = remember { mutableMapOf<String, Marker>() }
        val animatorMap = remember { mutableMapOf<String, ValueAnimator>() }
        val polylineRef = remember { Ref<Polyline?>(null) }
        val currentRouteRef = remember { Ref<List<GeoPoint>>(emptyList()) }

        val pLoc = markers.firstOrNull { it.id == "passenger" }?.location ?: initialLocation
        val dLoc = markers.firstOrNull { it.id == "dest" }?.location

        LaunchedEffect(pLoc, dLoc) {
            if (dLoc != null) {
                val routeStr = routingRepository.getRoute(
                    Location(pLoc.lat, pLoc.lng),
                    Location(dLoc.lat, dLoc.lng)
                )
                if (routeStr != null) {
                    val decoded = PolylineDecoder.decode(routeStr)
                    routePoints = decoded.map { GeoPoint(latitude = it.lat, longitude = it.lng) }
                } else {
                    routePoints = emptyList()
                }
            } else {
                routePoints = emptyList()
            }
        }

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

                val currentIds = markers.map { it.id }.toSet()
                val idsToRemove = markerMap.keys.toList() - currentIds
                idsToRemove.forEach { id ->
                    markerMap[id]?.let { manager.removeObject(it) }
                    markerMap.remove(id)
                    animatorMap[id]?.cancel()
                    animatorMap.remove(id)
                }

                markers.forEach { markerData ->
                    val point = GeoPoint(latitude = markerData.location.lat, longitude = markerData.location.lng)
                    val pointWithElevation = GeoPointWithElevation(latitude = markerData.location.lat, longitude = markerData.location.lng)
                    val existingMarker = markerMap[markerData.id]

                    if (existingMarker == null) {
                        val newMarker = Marker(
                            MarkerOptions(
                                position = pointWithElevation,
                                text = markerData.title ?: "",
                                icon = imageFromResource(context, android.R.drawable.ic_menu_myplaces)
                            )
                        )
                        manager.addObject(newMarker)
                        markerMap[markerData.id] = newMarker
                    } else {
                        val oldPos = existingMarker.position
                        // Only animate if the position actually changed
                        if (oldPos.latitude.value != point.latitude.value || oldPos.longitude.value != point.longitude.value) {
                            animatorMap[markerData.id]?.cancel()

                            val animator = ValueAnimator.ofFloat(0f, 1f)
                            animator.duration = 1000
                            animator.interpolator = LinearInterpolator()
                            animator.addUpdateListener { anim ->
                                val fraction = anim.animatedValue as Float
                                val lat = oldPos.latitude.value + (point.latitude.value - oldPos.latitude.value) * fraction
                                val lng = oldPos.longitude.value + (point.longitude.value - oldPos.longitude.value) * fraction
                                existingMarker.position = GeoPointWithElevation(
                                    latitude = lat,
                                    longitude = lng
                                )
                            }
                            animator.start()
                            animatorMap[markerData.id] = animator
                        }
                        existingMarker.text = markerData.title ?: ""
                    }
                }

                if (routePoints != currentRouteRef.value) {
                    val existingPolyline = polylineRef.value
                    if (existingPolyline != null) {
                        manager.removeObject(existingPolyline)
                        polylineRef.value = null
                    }
                    if (routePoints.isNotEmpty()) {
                        val newPolyline = Polyline(
                            PolylineOptions(
                                points = routePoints,
                                width = 5.lpx
                            )
                        )
                        manager.addObject(newPolyline)
                        polylineRef.value = newPolyline
                    }
                    currentRouteRef.value = routePoints
                }
            }
        )
    }
}
