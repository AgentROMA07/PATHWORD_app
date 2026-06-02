package com.pathword.taxi.di

import com.google.gson.Gson
import com.pathword.taxi.data.network.TaxiWebSocketClient
import com.pathword.taxi.domain.repository.ITaxiRepository
import android.content.Context
import android.content.SharedPreferences
import com.pathword.taxi.map.TwoGisMapProvider
import com.pathword.taxi.map.IMapProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOsrmApi(client: OkHttpClient): com.pathword.taxi.data.network.OsrmApi {
        return retrofit2.Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000/") // Localhost from Android emulator
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.pathword.taxi.data.network.OsrmApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRoutingRepository(api: com.pathword.taxi.data.network.OsrmApi): com.pathword.taxi.domain.repository.IRoutingRepository {
        return com.pathword.taxi.data.repository.RoutingRepositoryImpl(api)
    }

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
    fun provideTwoGisApi(client: OkHttpClient): com.pathword.taxi.data.network.TwoGisApi {
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://catalog.api.2gis.com/")
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.pathword.taxi.data.network.TwoGisApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGeocodingRepository(api: com.pathword.taxi.data.network.TwoGisApi): com.pathword.taxi.domain.repository.IGeocodingRepository {
        return com.pathword.taxi.data.repository.GeocodingRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideMapProvider(): IMapProvider {
        return TwoGisMapProvider()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("taxi_prefs", Context.MODE_PRIVATE)
    }
}
