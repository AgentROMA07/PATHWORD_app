package com.pathword.taxi.di

import com.google.gson.Gson
import com.pathword.taxi.data.network.TaxiWebSocketClient
import com.pathword.taxi.domain.repository.ITaxiRepository
import android.content.Context
import android.content.SharedPreferences
import com.pathword.taxi.map.MapboxMapProvider
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
        return MapboxMapProvider()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("taxi_prefs", Context.MODE_PRIVATE)
    }
}
