package me.euaek.api

import com.hypixel.hytale.math.vector.Transform
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import me.euaek.Plugin
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import java.util.UUID

class ServerApi(private val plugin: Plugin) {
    private val logger = plugin.logger
    private val commandApi = CommandApi(plugin)
    private val systemApi = SystemApi(plugin)

    fun reload(){
        commandApi.reload()
        systemApi.reload()
    }

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

    @HostAccess.Export
    fun addSystem(type: String, config: Value) {
        systemApi.addSystem(type, config)
    }
    @HostAccess.Export
    fun addEventSystem(type: String, eventClass: Value, config: Value) {
        systemApi.addEventSystem(type, eventClass, config)
    }

    @HostAccess.Export
    fun createComponent(type: String, vararg args: Any?): Any? {
        return when(type.lowercase()) {
            "transform" -> {
                if(args.size == 2 && args[0] is Vector3d && args[1] is Vector3f) TransformComponent(args[0] as Vector3d, args[1] as Vector3f)
                else TransformComponent()
            }
            else -> null
        }
    }

    @HostAccess.Export
    fun create(type: String, vararg args: Any?): Any? {
        return when(type.lowercase()) {
            "uuid" -> UUID.fromString(args[0] as String)
            "transform" -> {
                if(args.size == 1 && args[0] is Vector3d) Transform(args[0] as Vector3d)
                else if(args.size == 3) Transform(
                    (args[0] as Number).toDouble(),
                    (args[1] as Number).toDouble(),
                    (args[2] as Number).toDouble()
                )
                else Transform(0.0, 0.0, 0.0)
            }
            "message" -> {
                if(args.isEmpty()) Message.empty()
                else Message.raw(args[0].toString())
            }
            else -> null
        }
    }
}