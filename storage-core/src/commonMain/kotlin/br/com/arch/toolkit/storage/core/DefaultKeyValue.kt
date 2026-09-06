package br.com.arch.toolkit.storage.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

internal class DefaultKeyValue<ResultData> internal constructor(
    private val keyValue: KeyValue<ResultData?>,
    private val default: (() -> ResultData?)
) : KeyValue<ResultData?>() {
    override var lastValue: ResultData?
        set(value) = set(value = value)
        get() = keyValue.lastValue ?: default.invokeCatching()

    override fun get() = keyValue.get().map {
        it ?: default.invokeCatching()
    }

    override fun set(value: ResultData?, scope: CoroutineScope) = keyValue.set(value, scope)

    private fun <R> (() -> R).invokeCatching() = catchingStorageFailure { invoke() }.getOrNull()
}
