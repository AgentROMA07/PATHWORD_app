package com.pathword.taxi.data.network

import retrofit2.http.GET
import retrofit2.http.Path

data class OsrmRouteResponse(
    val code: String,
    val routes: List<OsrmRoute>?
)

data class OsrmRoute(
    val geometry: String?
)

data class OsrmMatchResponse(
    val code: String,
    val matchings: List<OsrmMatching>?
)

data class OsrmMatching(
    val geometry: String?
)

interface OsrmApi {
    @GET("route/v1/driving/{coordinates}?overview=full&geometries=polyline")
    suspend fun getRoute(
        @Path("coordinates") coordinates: String
    ): OsrmRouteResponse

    @GET("match/v1/driving/{coordinates}?overview=full&geometries=polyline")
    suspend fun matchRoute(
        @Path("coordinates") coordinates: String
    ): OsrmMatchResponse
}
