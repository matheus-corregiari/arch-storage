import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import br.com.arch.toolkit.storage.core.KeyValue
import br.com.arch.toolkit.storage.core.KeyValue.Companion.default
import br.com.arch.toolkit.storage.core.KeyValue.Companion.map
import br.com.arch.toolkit.storage.core.KeyValue.Companion.required
import br.com.arch.toolkit.storage.core.instant
import br.com.arch.toolkit.storage.datastore.DataStoreProvider
import br.com.arch.toolkit.storage.memory.MemoryStoreProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File

enum class Theme { Light, Dark }

class Settings(entry: KeyValue<Int>) {
    var count by entry.delegate()
}

fun main(args: Array<String>) = runBlocking {
    require(args.size == 2) { "Expected write|read and a .preferences_pb file" }
    val file = File(args[1]).absoluteFile
    file.parentFile.mkdirs()
    val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.IO + job)
    try {
        val provider = DataStoreProvider(PreferenceDataStoreFactory.create(scope = scope) { file })
        if (args[0] == "write") {
            suspend fun <T> save(entry: KeyValue<T>, value: T) {
                entry.set(value, scope)
                withTimeout(10_000) { entry.get().first { it == value } }
            }
            save(provider.boolean("enabled"), true)
            save(provider.int("count"), 42)
            save(provider.long("timestamp"), 123456789L)
            save(provider.float("ratio"), 1.5f)
            save(provider.double("amount"), 2.5)
            save(provider.string("name"), "RC16")
            save(provider.enum("theme", Theme.Light), Theme.Dark)
            save(provider.model<List<Int>>("numbers"), listOf(1, 2, 3))
            val bytes = provider.byteArray("bytes")
            bytes.set(byteArrayOf(1, 2, 3), scope)
            withTimeout(10_000) { bytes.get().first { it.contentEquals(byteArrayOf(1, 2, 3)) } }
            println("RC16 persistence fixture written")
        } else {
            require(args[0] == "read")
            check(provider.boolean("enabled").get().first() == true)
            check(provider.int("count").get().first() == 42)
            check(provider.long("timestamp").get().first() == 123456789L)
            check(provider.float("ratio").get().first() == 1.5f)
            check(provider.double("amount").get().first() == 2.5)
            check(provider.string("name").get().first() == "RC16")
            check(provider.enum("theme", Theme.Light).get().first() == Theme.Dark)
            check(provider.model<List<Int>>("numbers").get().first() == listOf(1, 2, 3))
            check(provider.byteArray("bytes").get().first().contentEquals(byteArrayOf(1, 2, 3)))
            check(provider.string("absent").get().first() == null)
            val memory = MemoryStoreProvider(mutableMapOf())
            val counter = memory.int("counter").required { 0 }.scope(scope)
            val settings = Settings(counter)
            settings.count = 5
            check(settings.count == 5 && counter.instant() == 5)
            val theme = memory.string("theme").required { "Light" }
                .map(Theme::valueOf, Theme::name)
            check(theme.current() == Theme.Light)
            theme.set(Theme.Dark, scope)
            check(memory.string("theme").current() == "Dark")
            check(memory.string("missing").default("fallback").current() == "fallback")
            println("1.0.0 consumer and RC16 persistence compatibility passed")
        }
    } finally {
        job.cancelAndJoin()
    }
}
