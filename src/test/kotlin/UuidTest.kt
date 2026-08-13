import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.uuid.Uuid

class UuidTest {
    @Test
    fun testUuid() {
        val uuid = Uuid.random()
        val uuidString = Json.encodeToString(uuid)
        println(uuidString)
        val decodeFromString = Json.decodeFromString<Uuid>(uuidString)
        println(decodeFromString)
    }
}