package com.pathword.taxi.map

import com.pathword.taxi.domain.model.Location
import org.junit.Assert.assertEquals
import org.junit.Test

class PolylineDecoderTest {

    @Test
    fun decode_validPolyline_returnsCorrectCoordinates() {
        // A standard OSRM polyline representing some short route.
        // E.g., "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        val encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        val result = PolylineDecoder.decode(encoded)

        assertEquals(3, result.size)

        // Verifying the first coordinate against standard decoding output
        // Example logic: lat/lng should match roughly 38.5, -120.2
        val first = result[0]
        assertEquals(38.5, first.lat, 0.0001)
        assertEquals(-120.2, first.lng, 0.0001)

        val second = result[1]
        assertEquals(40.7, second.lat, 0.0001)
        assertEquals(-120.95, second.lng, 0.0001)

        val third = result[2]
        assertEquals(43.252, third.lat, 0.0001)
        assertEquals(-126.453, third.lng, 0.0001)
    }

    @Test
    fun decode_emptyString_returnsEmptyList() {
        val result = PolylineDecoder.decode("")
        assertEquals(0, result.size)
    }
}
