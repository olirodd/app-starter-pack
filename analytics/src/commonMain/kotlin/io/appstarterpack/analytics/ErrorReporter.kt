package io.appstarterpack.analytics

interface ErrorReporter {
    fun report(throwable: Throwable)
}
