package me.euaek

import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value

class ServerApi(private val plugin: Plugin) {
    private val logger = Plugin.instance.logger
    private val commandApi = CommandApi(plugin)

    @HostAccess.Export
    fun addCommand(config: Value) {
        commandApi.addCommand(config)
    }

    @HostAccess.Export
    fun addPlayerCommand(config: Value) {
        config.putMember("type", "player")
        commandApi.addCommand(config)
    }

    @HostAccess.Export
    fun addCommandCollection(config: Value) {
        config.putMember("type", "collection")
        commandApi.addCommand(config)
    }

    fun reload(){
    }
}