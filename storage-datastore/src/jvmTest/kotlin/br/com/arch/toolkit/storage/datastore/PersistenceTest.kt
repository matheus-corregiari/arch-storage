package br.com.arch.toolkit.storage.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class PersistenceTest {
    @Test
    fun valueSurvivesStoreRecreation() = runBlocking {
        val directory = Files.createTempDirectory("arch-storage-test").toFile()
        val file = directory.resolve("settings.preferences_pb")
        val writerJob = SupervisorJob()
        val readerJob = SupervisorJob()
        try {
            val writer = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(
                    Dispatchers.IO + writerJob
                )
            ) { file }
            val entry = DataStoreProvider(writer).string("name")
            entry.set("persisted", CoroutineScope(Dispatchers.IO + writerJob))
            withTimeout(10_000) { entry.get().first { it == "persisted" } }
            writerJob.cancelAndJoin()
            val reader = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(
                    Dispatchers.IO + readerJob
                )
            ) { file }
            assertEquals("persisted", DataStoreProvider(reader).string("name").get().first())
        } finally {
            listOf<Job>(writerJob, readerJob).forEach { it.cancelAndJoin() }
            directory.deleteRecursively()
        }
    }
}
