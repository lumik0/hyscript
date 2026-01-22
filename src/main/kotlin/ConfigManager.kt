package me.euaek

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hypixel.hytale.event.EventPriority
import java.io.File

data class Config(
    var isHotReloadEnabled: Boolean = true,
    var enableTypescript: Boolean = true,
    var eventPriority: EventPriority = EventPriority.LATE
)

class ConfigManager(private val plugin: Plugin, private val configFile: File) {
    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    var isNew = false
    var current = Config()

    fun load() {
        if(!configFile.exists()) {
            isNew = true
            save()
            return
        }
        try {
            val loaded: Config = mapper.readValue(configFile)

            current = loaded

            save()
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