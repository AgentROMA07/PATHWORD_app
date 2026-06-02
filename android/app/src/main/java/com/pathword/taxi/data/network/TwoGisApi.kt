package com.pathword.taxi.data.network

import retrofit2.http.GET
import retrofit2.http.Query

data class GeocodeResponse(
    val result: GeocodeResult?
)

data class GeocodeResult(
    val items: List<GeocodeItem>?
)

data class GeocodeItem(
    val point: GeocodePoint?
)

data class GeocodePoint(
    val lat: Double,
    val lon: Double
)

interface TwoGisApi {
    @GET("3.0/items/geocode")
    suspend fun geocode(
        @Query("q") address: String,
        @Query("key") apiKey: String = "MOCK_2GIS_KEY"
    ): GeocodeResponse
}
