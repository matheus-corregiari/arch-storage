package br.com.arch.toolkit.storage.datastore

import kotlin.test.Test
import kotlin.test.assertFailsWith

class UnsupportedPlatformTest {
    @Test
    @Suppress("DEPRECATION")
    fun factoryFailsExplicitlyOnUnsupportedPlatforms() {
        assertFailsWith<IllegalStateException> { DataStoreProvider() }
    }
}
