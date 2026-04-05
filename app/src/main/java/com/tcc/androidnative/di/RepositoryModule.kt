package com.tcc.androidnative.di

import com.tcc.androidnative.core.session.SessionManager
import com.tcc.androidnative.core.session.SecureSessionManager
import com.tcc.androidnative.feature.auth.data.AuthRepository
import com.tcc.androidnative.feature.auth.data.AuthRepositoryImpl
import com.tcc.androidnative.feature.auth.data.google.GoogleSignInGateway
import com.tcc.androidnative.feature.auth.data.google.GoogleSignInGatewayImpl
import com.tcc.androidnative.feature.calendar.data.CalendarRepository
import com.tcc.androidnative.feature.calendar.data.CalendarRepositoryImpl
import com.tcc.androidnative.feature.clients.data.ClientsRepository
import com.tcc.androidnative.feature.clients.data.ClientsRepositoryImpl
import com.tcc.androidnative.feature.reports.data.ReportsRepository
import com.tcc.androidnative.feature.reports.data.ReportsRepositoryImpl
import com.tcc.androidnative.feature.services.data.ServicesRepository
import com.tcc.androidnative.feature.services.data.ServicesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSessionManager(impl: SecureSessionManager): SessionManager

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindGoogleSignInGateway(impl: GoogleSignInGatewayImpl): GoogleSignInGateway

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(impl: CalendarRepositoryImpl): CalendarRepository

    @Binds
    @Singleton
    abstract fun bindClientsRepository(impl: ClientsRepositoryImpl): ClientsRepository

    @Binds
    @Singleton
    abstract fun bindServicesRepository(impl: ServicesRepositoryImpl): ServicesRepository

    @Binds
    @Singleton
    abstract fun bindReportsRepository(impl: ReportsRepositoryImpl): ReportsRepository
}
