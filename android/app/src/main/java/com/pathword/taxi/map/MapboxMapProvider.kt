package com.pathword.taxi.map

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Point
import com.mapbox.geojson.LineString
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class PointEvaluator : TypeEvaluator<Point> {
    override fun evaluate(fraction: Float, startValue: Point, endValue: Point): Point {
        val lat = startValue.latitude() + (endValue.latitude() - startValue.latitude()) * fraction
        val lng = startValue.longitude() + (endValue.longitude() - startValue.longitude()) * fraction
        return Point.fromLngLat(lng, lat)
    }
}

fun calculateBearing(start: Point, end: Point): Double {
    val startLat = Math.toRadians(start.latitude())
    val startLng = Math.toRadians(start.longitude())
    val endLat = Math.toRadians(end.latitude())
    val endLng = Math.toRadians(end.longitude())

    val dLng = endLng - startLng

    val y = sin(dLng) * cos(endLat)
    val x = cos(startLat) * sin(endLat) - sin(startLat) * cos(endLat) * cos(dLng)

    var bearing = Math.toDegrees(atan2(y, x))
    bearing = (bearing + 360) % 360
    return bearing
}

class MapboxMapProvider : IMapProvider {
    @Composable
    override fun MapView(
        modifier: Modifier,
        initialLocation: MapLocation,
        markers: List<MapMarker>,
        onMapClick: ((MapLocation) -> Unit)?
    ) {
        var mapView by remember { mutableStateOf<MapView?>(null) }
        var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
        var polylineAnnotationManager by remember { mutableStateOf<PolylineAnnotationManager?>(null) }

        val driverAnnotations = remember { mutableMapOf<String, PointAnnotation>() }
        val previousLocations = remember { mutableMapOf<String, Point>() }

        AndroidView(
            factory = { context ->
                MapView(context).apply {
                    mapboxMap.loadStyle(Style.DARK) {
                        val initialCameraOptions = CameraOptions.Builder()
                            .center(Point.fromLngLat(initialLocation.lng, initialLocation.lat))
                            .zoom(12.0)
                            .build()
                        mapboxMap.setCamera(initialCameraOptions)
                    }

                    if (onMapClick != null) {
                        mapboxMap.addOnMapClickListener { point ->
                            onMapClick.invoke(MapLocation(point.latitude(), point.longitude()))
                            true
                        }
                    }

                    val annotationApi = annotations
                    pointAnnotationManager = annotationApi.createPointAnnotationManager()
                    polylineAnnotationManager = annotationApi.createPolylineAnnotationManager()

                    mapView = this
                }
            },
            modifier = modifier,
            update = { view ->
                val pManager = pointAnnotationManager ?: return@AndroidView
                val plManager = polylineAnnotationManager ?: return@AndroidView

                // Clear non-driver markers and routes
                pManager.annotations.filter { annot -> !driverAnnotations.values.contains(annot) }.forEach { pManager.delete(it) }
                plManager.deleteAll()

                var passengerLoc: Point? = null
                var destLoc: Point? = null

                markers.forEach { marker ->
                    val point = Point.fromLngLat(marker.location.lng, marker.location.lat)

                    if (marker.isDriver) {
                        val existingAnnotation = driverAnnotations[marker.id]
                        val prevPoint = previousLocations[marker.id]

                        if (existingAnnotation != null && prevPoint != null) {
                            if (prevPoint != point) {
                                val bearing = calculateBearing(prevPoint, point)
                                val animator = ValueAnimator.ofObject(PointEvaluator(), prevPoint, point)
                                animator.duration = 2000
                                animator.addUpdateListener { animation ->
                                    val animatedPoint = animation.animatedValue as Point
                                    existingAnnotation.point = animatedPoint

                                    // Rotate the text representation simulating an icon rotation
                                    existingAnnotation.textRotate = bearing
                                    pManager.update(existingAnnotation)
                                }
                                animator.start()
                                previousLocations[marker.id] = point

                                val cameraOptions = CameraOptions.Builder()
                                    .center(point)
                                    .build()
                                val animationOptions = MapAnimationOptions.Builder().duration(2000).build()
                                view.camera.flyTo(cameraOptions, animationOptions)
                            }
                        } else {
                            val options = PointAnnotationOptions()
                                .withPoint(point)
                                .withTextField(marker.title ?: "Car")
                                // Explicitly start with a text that will be rotated simulating a car
                                .withTextRotate(0.0)
                            val newAnnotation = pManager.create(options)
                            driverAnnotations[marker.id] = newAnnotation
                            previousLocations[marker.id] = point

                            val cameraOptions = CameraOptions.Builder()
                                .center(point)
                                .build()
                            val animationOptions = MapAnimationOptions.Builder().duration(2000).build()
                            view.camera.flyTo(cameraOptions, animationOptions)
                        }
                    } else {
                        val options = PointAnnotationOptions()
                            .withPoint(point)
                            .withTextField(marker.title ?: "")

                        pManager.create(options)

                        if (marker.id == "dest") {
                            destLoc = point
                        } else if (marker.id == "passenger") {
                            passengerLoc = point
                        }
                    }
                }

                // Draw a mock route along roads if there's a destination
                // Simulate a complex route than a straight line if destination is present.
                if (destLoc != null) {
                    val pLoc = passengerLoc ?: Point.fromLngLat(initialLocation.lng, initialLocation.lat)

                    // Mock route: instead of straight line, add a mid-point to simulate roads
                    val midLng = (pLoc.longitude() + destLoc!!.longitude()) / 2 + 0.01
                    val midLat = (pLoc.latitude() + destLoc!!.latitude()) / 2 - 0.01
                    val midPoint = Point.fromLngLat(midLng, midLat)

                    val points = listOf(pLoc, midPoint, destLoc)
                    val lineString = LineString.fromLngLats(points)

                    val polylineOptions = PolylineAnnotationOptions()
                        .withGeometry(lineString)
                        .withLineColor("#0000FF") // Blue Line
                        .withLineWidth(5.0)

                    plManager.create(polylineOptions)
                }
            }
        )
    }
}
