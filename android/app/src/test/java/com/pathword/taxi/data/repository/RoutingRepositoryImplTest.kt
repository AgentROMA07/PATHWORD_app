package com.pathword.taxi.data.repository

import com.pathword.taxi.data.network.OsrmApi
import com.pathword.taxi.data.network.OsrmRouteResponse
import com.pathword.taxi.data.network.OsrmMatchResponse
import com.pathword.taxi.data.network.OsrmRoute
import com.pathword.taxi.data.network.OsrmMatching
import com.pathword.taxi.domain.model.Location
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutingRepositoryImplTest {

    private class FakeOsrmApi(
        private val successRoute: String? = null,
        private val successMatch: String? = null,
        private val shouldThrow: Boolean = false
    ) : OsrmApi {
        override suspend fun getRoute(coordinates: String): OsrmRouteResponse {
            if (shouldThrow) throw RuntimeException("Network Error")
            if (successRoute != null) {
                return OsrmRouteResponse("Ok", listOf(OsrmRoute(successRoute)))
            }
            return OsrmRouteResponse("NoRoute", null)
        }

        override suspend fun matchRoute(coordinates: String): OsrmMatchResponse {
            if (shouldThrow) throw RuntimeException("Network Error")
            if (successMatch != null) {
                return OsrmMatchResponse("Ok", listOf(OsrmMatching(successMatch)))
            }
            return OsrmMatchResponse("NoMatch", null)
        }
    }

    @Test
    fun getRoute_successfulResponse_returnsGeometryString() = runBlocking {
        val fakeApi = FakeOsrmApi(successRoute = "encoded_polyline")
        val repository = RoutingRepositoryImpl(fakeApi)

        val result = repository.getRoute(Location(0.0, 0.0), Location(1.0, 1.0))
        assertEquals("encoded_polyline", result)
    }

    @Test
    fun getRoute_networkFailure_returnsNull() = runBlocking {
        val fakeApi = FakeOsrmApi(shouldThrow = true)
        val repository = RoutingRepositoryImpl(fakeApi)

        val result = repository.getRoute(Location(0.0, 0.0), Location(1.0, 1.0))
        assertNull(result)
    }

    @Test
    fun getRoute_badCode_returnsNull() = runBlocking {
        val fakeApi = FakeOsrmApi() // Returns "NoRoute"
        val repository = RoutingRepositoryImpl(fakeApi)

        val result = repository.getRoute(Location(0.0, 0.0), Location(1.0, 1.0))
        assertNull(result)
    }

    @Test
    fun getMapMatchedRoute_lessThanTwoCoordinates_returnsNull() = runBlocking {
        val repository = RoutingRepositoryImpl(FakeOsrmApi())
        val result = repository.getMapMatchedRoute(listOf(Location(0.0, 0.0)))
        assertNull(result)
    }

    @Test
    fun getMapMatchedRoute_successfulResponse_returnsGeometryString() = runBlocking {
        val fakeApi = FakeOsrmApi(successMatch = "encoded_match_polyline")
        val repository = RoutingRepositoryImpl(fakeApi)

        val result = repository.getMapMatchedRoute(listOf(Location(0.0, 0.0), Location(1.0, 1.0)))
        assertEquals("encoded_match_polyline", result)
    }

    @Test
    fun getMapMatchedRoute_networkFailure_returnsNull() = runBlocking {
        val fakeApi = FakeOsrmApi(shouldThrow = true)
        val repository = RoutingRepositoryImpl(fakeApi)

        val result = repository.getMapMatchedRoute(listOf(Location(0.0, 0.0), Location(1.0, 1.0)))
        assertNull(result)
    }
}
