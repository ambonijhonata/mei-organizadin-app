package com.tcc.androidnative.di

import com.tcc.androidnative.feature.auth.data.remote.AuthApi
import com.tcc.androidnative.feature.calendar.data.remote.CalendarApi
import com.tcc.androidnative.feature.clients.data.remote.ClientApi
import com.tcc.androidnative.feature.reports.data.remote.ReportApi
import com.tcc.androidnative.feature.services.data.remote.ServiceApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideCalendarApi(retrofit: Retrofit): CalendarApi = retrofit.create(CalendarApi::class.java)

    @Provides
    @Singleton
    fun provideClientApi(retrofit: Retrofit): ClientApi = retrofit.create(ClientApi::class.java)

    @Provides
    @Singleton
    fun provideServiceApi(retrofit: Retrofit): ServiceApi = retrofit.create(ServiceApi::class.java)

    @Provides
    @Singleton
    fun provideReportApi(retrofit: Retrofit): ReportApi = retrofit.create(ReportApi::class.java)
}

