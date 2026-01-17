package me.euaek

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

data class Config(
    var isHotReloadEnabled: Boolean = true,
    var typescript: Boolean = true
)

class ConfigManager(private val plugin: Plugin, private val configFile: File) {
    private val mapper = jacksonObjectMapper()
    var current = Config()

    fun load() {
        if(!configFile.exists()) {
            save()
            return
        }
        try {
            current = mapper.readValue(configFile)
        } catch(e: Exception) {
            plugin.logger.atSevere().log("❌ Failed to load config: ${e.message}")
        }
    }

    fun save() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, current)
        } catch(e: Exception) {
            plugin.logger.atSevere().log("❌ Failed to save config: ${e.message}")
        }
    }
}