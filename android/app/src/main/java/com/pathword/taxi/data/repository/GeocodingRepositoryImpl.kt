package com.pathword.taxi.data.repository

import com.pathword.taxi.data.network.TwoGisApi
import com.pathword.taxi.domain.model.Location
import com.pathword.taxi.domain.repository.IGeocodingRepository

class GeocodingRepositoryImpl(
    private val api: TwoGisApi
) : IGeocodingRepository {
    override suspend fun getCoordinatesFromAddress(address: String): Location? {
        return try {
            val response = api.geocode(address)
            val point = response.result?.items?.firstOrNull()?.point
            if (point != null) {
                Location(point.lat, point.lon)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
