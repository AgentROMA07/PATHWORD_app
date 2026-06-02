package com.pathword.taxi.map

import android.animation.ValueAnimator
import com.pathword.taxi.domain.model.Location
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object AnimationUtils {
    fun calculateBearing(start: Location, end: Location): Double {
        val startLat = Math.toRadians(start.lat)
        val startLng = Math.toRadians(start.lng)
        val endLat = Math.toRadians(end.lat)
        val endLng = Math.toRadians(end.lng)

        val dLng = endLng - startLng

        val y = sin(dLng) * cos(endLat)
        val x = cos(startLat) * sin(endLat) - sin(startLat) * cos(endLat) * cos(dLng)

        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360
        return bearing
    }
}
