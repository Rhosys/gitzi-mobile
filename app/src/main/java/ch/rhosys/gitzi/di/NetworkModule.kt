package ch.rhosys.gitzi.di

import ch.rhosys.gitzi.data.remote.GitziApiService
import ch.rhosys.gitzi.domain.repository.ConnectionSettingsRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Provider
import javax.inject.Singleton

/**
 * A placeholder Retrofit base URL. The real host is unknown at DI-graph
 * construction time (the user configures it on-device), so [DynamicBaseUrlInterceptor]
 * rewrites every request's scheme/host/port from the current connection
 * settings before it goes out — Retrofit's own base URL never actually matters.
 */
private const val PLACEHOLDER_BASE_URL = "http://gitzi.invalid/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private class DynamicBaseUrlInterceptor(
        private val settingsProvider: Provider<ConnectionSettingsRepository>,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val serverUrl = runBlocking { settingsProvider.get().settings.first().serverUrl }
            val request = chain.request()
            val overrideHost = serverUrl.toHttpUrlOrNull()
            val newUrl =
                if (overrideHost != null) {
                    request.url.newBuilder()
                        .scheme(overrideHost.scheme)
                        .host(overrideHost.host)
                        .port(overrideHost.port)
                        .build()
                } else {
                    request.url
                }
            return chain.proceed(request.newBuilder().url(newUrl).build())
        }
    }

    private class AuthInterceptor(
        private val settingsProvider: Provider<ConnectionSettingsRepository>,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = runBlocking { settingsProvider.get().settings.first().apiToken }
            val request =
                if (token.isBlank()) {
                    chain.request()
                } else {
                    chain.request().newBuilder().header("Authorization", "Bearer $token").build()
                }
            return chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    @RestHttpClient
    fun provideRestOkHttpClient(settingsProvider: Provider<ConnectionSettingsRepository>): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(DynamicBaseUrlInterceptor(settingsProvider))
            .addInterceptor(AuthInterceptor(settingsProvider))
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build()

    @Provides
    @Singleton
    @SocketHttpClient
    fun provideSocketOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideRetrofit(
        @RestHttpClient client: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideGitziApiService(retrofit: Retrofit): GitziApiService = retrofit.create(GitziApiService::class.java)
}
