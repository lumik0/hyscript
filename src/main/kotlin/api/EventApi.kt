package me.euaek.api

import com.hypixel.hytale.server.core.event.events.player.*
import me.euaek.Plugin

class EventApi(private val plugin: Plugin) {
    init {
        registerEvents()
    }

    fun reload(){

    }

    fun shutdown(){

    }

    fun callSync(name: String, vararg args: Any){
        try {
            plugin.tsLoader.server?.invokeMember("callSync", name, *args)
        } catch(e: Exception){
            if(e is org.graalvm.polyglot.PolyglotException) {
                val location = e.sourceLocation
                val position = if(location != null) " [Line ${location.startLine}, Col ${location.startColumn}]" else ""

                plugin.logger.atSevere().log("❌ Script Error in '$name'$position: ${e.message}")

                e.polyglotStackTrace.forEach {frame ->
                    if(frame.isGuestFrame) {
                        plugin.logger.atSevere().log("    at ${frame.rootName}(${frame.sourceLocation})")
                    }
                }
            } else {
                plugin.logger.atSevere().log("❌ Internal Error while calling '$name': ${e.message}")
            }
        }
    }

    private fun registerEvents() {
        val eventPriority = plugin.configManager.current.eventPriority

        plugin.eventRegistry.register(eventPriority, PlayerConnectEvent::class.java) { e ->
            callSync("playerConnect", mutableMapOf(
                "playerRef" to e.playerRef,
                "world" to e.world
            ))
        }
        plugin.eventRegistry.register(eventPriority, PlayerDisconnectEvent::class.java) { e ->
            callSync("playerDisconnect", mutableMapOf(
                "playerRef" to e.playerRef,
                "reason" to e.disconnectReason
            ))
        }
        plugin.eventRegistry.register(eventPriority, PlayerReadyEvent::class.java) { e ->
            callSync("playerReady", mutableMapOf(
                "playerRef" to e.playerRef,
                "readyId" to e.readyId
            ))
        }
        plugin.eventRegistry.register(eventPriority, PlayerMouseButtonEvent::class.java) { e ->
            val data = mutableMapOf(
                "playerRef" to e.playerRef,
                "clientUseTime" to e.clientUseTime,
                "itemInHand" to e.itemInHand,
                "targetBlock" to e.targetBlock,
                "targetEntity" to e.targetEntity,
                "screenPoint" to e.screenPoint,
                "mouseButton" to e.mouseButton,
                "isCancelled" to false
            )
            callSync("playerMouseButton", data)
            if(data["isCancelled"] == true) e.isCancelled = true
        }
        plugin.eventRegistry.register(eventPriority, PlayerChatEvent::class.java) { e ->
            val data = mutableMapOf(
                "sender" to e.sender,
                "content" to e.content,
                "formatter" to e.formatter,
                "targets" to e.targets,
                "isCancelled" to false
            )
            callSync("playerChat", data)
            if(data["isCancelled"] == true) e.isCancelled = true
        }
    }
}