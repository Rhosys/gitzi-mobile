package ch.rhosys.gitzi.di

import ch.rhosys.gitzi.data.SwitchableGitziRepository
import ch.rhosys.gitzi.domain.repository.GitziRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the switchable repository for all build types. Mock mode is
 * toggleable via Settings → "Use mock data". Once a real backend is
 * deployed and stable, this can be reverted to bind RemoteGitziRepository
 * directly and the mock toggle removed from Settings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGitziRepository(impl: SwitchableGitziRepository): GitziRepository
}
