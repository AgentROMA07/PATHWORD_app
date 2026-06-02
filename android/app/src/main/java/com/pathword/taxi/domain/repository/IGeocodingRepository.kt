package com.pathword.taxi.domain.repository

import com.pathword.taxi.domain.model.Location

interface IGeocodingRepository {
    suspend fun getCoordinatesFromAddress(address: String): Location?
}
