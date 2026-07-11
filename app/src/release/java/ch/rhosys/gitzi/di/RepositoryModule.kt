package ch.rhosys.gitzi.di

import ch.rhosys.gitzi.data.remote.RemoteGitziRepository
import ch.rhosys.gitzi.domain.repository.GitziRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Release builds always talk to the real backend — no mock-data escape
 * hatch. See DebugRepositoryModule for the debug-only hot-swappable variant.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGitziRepository(impl: RemoteGitziRepository): GitziRepository
}
