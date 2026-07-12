package ch.rhosys.gitzi.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import ch.rhosys.gitzi.data.local.ConnectionSettingsDataStore
import ch.rhosys.gitzi.domain.repository.ConnectionSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.connectionSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "connection_settings")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideConnectionSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.connectionSettingsDataStore
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectionSettingsModule {
    @Binds
    @Singleton
    abstract fun bindConnectionSettingsRepository(impl: ConnectionSettingsDataStore): ConnectionSettingsRepository
}
