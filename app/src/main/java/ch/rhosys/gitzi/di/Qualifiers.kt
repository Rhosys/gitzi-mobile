package ch.rhosys.gitzi.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RestHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SocketHttpClient

/** A process-lifetime coroutine scope for work that must outlive any single screen (e.g. the event socket). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
