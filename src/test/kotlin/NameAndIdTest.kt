import net.minecraft.server.players.NameAndId
import java.util.*
import kotlin.test.Test

class NameAndIdTest {
    @Test
    fun testNameAndId() {
        val nameAndId1 = NameAndId(UUID.fromString("06f0aa59-7d8a-4546-a4c5-1b902e252ae6"), "tabidachinokaze")
        val nameAndId2 = NameAndId(UUID.fromString("06f0aa59-7d8a-4546-a4c5-1b902e252ae6"), "tabidachinokaze")
        println(nameAndId1 == nameAndId2)
    }
}