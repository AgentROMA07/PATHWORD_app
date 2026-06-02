package com.pathword.taxi.data.repository

import com.pathword.taxi.data.network.OsrmApi
import com.pathword.taxi.domain.model.Location
import com.pathword.taxi.domain.repository.IRoutingRepository

class RoutingRepositoryImpl(
    private val api: OsrmApi
) : IRoutingRepository {
    override suspend fun getRoute(origin: Location, destination: Location): String? {
        return try {
            val coords = "${origin.lng},${origin.lat};${destination.lng},${destination.lat}"
            val response = api.getRoute(coords)
            if (response.code == "Ok") {
                response.routes?.firstOrNull()?.geometry
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getMapMatchedRoute(coordinates: List<Location>): String? {
        if (coordinates.size < 2) return null
        return try {
            val coords = coordinates.joinToString(";") { "${it.lng},${it.lat}" }
            val response = api.matchRoute(coords)
            if (response.code == "Ok") {
                response.matchings?.firstOrNull()?.geometry
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
