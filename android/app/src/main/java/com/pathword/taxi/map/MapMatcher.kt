package com.pathword.taxi.map

import com.pathword.taxi.domain.model.Location
import com.pathword.taxi.domain.repository.IRoutingRepository

class MapMatcher(private val routingRepository: IRoutingRepository) {
    suspend fun matchCoordinates(rawCoordinates: List<Location>): List<Location> {
        val polyline = routingRepository.getMapMatchedRoute(rawCoordinates)
        return if (polyline != null) {
            PolylineDecoder.decode(polyline)
        } else {
            rawCoordinates
        }
    }
}
