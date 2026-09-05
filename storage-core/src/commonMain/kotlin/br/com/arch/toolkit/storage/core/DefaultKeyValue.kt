package br.com.arch.toolkit.storage.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

internal class DefaultKeyValue<ResultData> internal constructor(
    private val keyValue: KeyValue<ResultData?>,
    private val default: (() -> ResultData?)
) : KeyValue<ResultData?>() {
    override var lastValue: ResultData?
        set(value) = set(value = value)
        get() = keyValue.lastValue ?: default.invokeCatching()

    override fun get() = keyValue.get().map {
        it ?: withTimeoutOrNull(50.milliseconds) { default.invokeCatching() }
    }

    override fun set(value: ResultData?, scope: CoroutineScope) = keyValue.set(value, scope)

    private fun <R> (() -> R).invokeCatching() = runCatching { invoke() }.getOrNull()
}
