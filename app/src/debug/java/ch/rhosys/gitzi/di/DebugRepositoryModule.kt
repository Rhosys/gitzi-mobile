package ch.rhosys.gitzi.di

import ch.rhosys.gitzi.data.SwitchableGitziRepository
import ch.rhosys.gitzi.domain.repository.GitziRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Debug builds default to mock data (see ConnectionSettings.useMockData) but can hot-swap to the real API from Settings. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DebugRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGitziRepository(impl: SwitchableGitziRepository): GitziRepository
}
