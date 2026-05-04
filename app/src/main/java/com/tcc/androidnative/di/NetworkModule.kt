package com.tcc.androidnative.di

import com.tcc.androidnative.core.config.ApiConfig
import com.tcc.androidnative.core.network.AuthTokenInterceptor
import com.tcc.androidnative.core.network.AuthEndpointTimeoutInterceptor
import com.tcc.androidnative.core.network.BigDecimalJsonAdapter
import com.tcc.androidnative.core.network.RetryOnFailureInterceptor
import com.tcc.androidnative.core.network.SessionRefreshInterceptor
import com.tcc.androidnative.core.network.SessionInvalidationInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(BigDecimal::class.java, BigDecimalJsonAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideHttpClient(
        authTokenInterceptor: AuthTokenInterceptor,
        authEndpointTimeoutInterceptor: AuthEndpointTimeoutInterceptor,
        sessionInvalidationInterceptor: SessionInvalidationInterceptor,
        retryOnFailureInterceptor: RetryOnFailureInterceptor,
        sessionRefreshInterceptor: SessionRefreshInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(authEndpointTimeoutInterceptor)
            .addInterceptor(authTokenInterceptor)
            .addInterceptor(sessionRefreshInterceptor)
            .addInterceptor(sessionInvalidationInterceptor)
            .addInterceptor(retryOnFailureInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConfig.retrofitBaseUrl())
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}
