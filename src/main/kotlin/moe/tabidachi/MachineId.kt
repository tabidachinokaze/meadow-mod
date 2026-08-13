package moe.tabidachi

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object MachineId {
    private val OS_NAME = System.getProperty("os.name").lowercase()

    fun get(): String? {
        return when {
            OS_NAME.contains("win") -> getWindowsMachineGuid()
            OS_NAME.contains("mac") -> getMacIOPlatformUUID()
            OS_NAME.contains("nux") || OS_NAME.contains("nix") -> getLinuxMachineId()
            else -> null
        }
    }

    private fun getWindowsMachineGuid(): String? {
        return try {
            val process = ProcessBuilder(
                "reg", "query",
                "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography",
                "/v", "MachineGuid"
            ).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()

            val regex = Regex("MachineGuid\\s+REG_SZ\\s+(.+)")
            regex.find(output)?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            null
        }
    }

    private fun getMacIOPlatformUUID(): String? {
        return try {
            val process = ProcessBuilder(
                "ioreg", "-rd1", "-c", "IOPlatformExpertDevice"
            ).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()

            val regex = Regex("\"IOPlatformUUID\"\\s*=\\s*\"(.+)\"")
            regex.find(output)?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            null
        }
    }

    private fun getLinuxMachineId(): String? {
        return try {
            val file = File("/etc/machine-id")
            if (!file.exists()) return null
            file.readText().trim()
        } catch (e: Exception) {
            null
        }
    }
}