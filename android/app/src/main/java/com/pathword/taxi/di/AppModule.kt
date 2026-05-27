package com.pathword.taxi.di

import com.google.gson.Gson
import com.pathword.taxi.data.network.TaxiWebSocketClient
import com.pathword.taxi.domain.repository.ITaxiRepository
import com.pathword.taxi.map.GoogleMapProvider
import com.pathword.taxi.map.IMapProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideTaxiRepository(
        client: OkHttpClient,
        gson: Gson
    ): ITaxiRepository {
        return TaxiWebSocketClient(client, gson)
    }

    @Provides
    @Singleton
    fun provideMapProvider(): IMapProvider {
        return GoogleMapProvider()
    }
}
