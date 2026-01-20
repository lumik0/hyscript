package me.euaek

import au.ellie.hyui.builders.PageBuilder
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.RefChangeSystem
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.npc.systems.BlackboardSystems.BreakBlockEventSystem
import me.euaek.api.ServerApi
import me.euaek.commands.HyscriptCommand
import java.io.File
import javax.annotation.Nonnull

class Plugin(@Nonnull init: JavaPluginInit) : JavaPlugin(init) {
    companion object {
        lateinit var instance: Plugin
    }

    lateinit var serverApi: ServerApi
    lateinit var configManager: ConfigManager
    lateinit var tsLoader: TsLoader

    override fun setup() {
        instance = this

        serverApi = ServerApi(this)

        configManager = ConfigManager(this, File(dataDirectory.toFile(), "config.json"))
        configManager.load()

        tsLoader = TsLoader(this)
        tsLoader.setup()

        commandRegistry.registerCommand(HyscriptCommand())

        registerEvents();
    }

    override fun start() {
        logger.atInfo().log("Plugin started!")
    }

    override fun shutdown() {
        logger.atInfo().log("Plugin shutting down!")

        serverApi.shutdown()
    }

    fun info(log: String, context: CommandContext? = null){
        if(context != null){
            context.sendMessage(Message.raw(log))
        } else {
            logger.atInfo().log(log)
        }
    }
    fun warning(log: String, context: CommandContext? = null){
        if(context != null){
            context.sendMessage(Message.raw(log).color("yellow"))
        } else {
            logger.atWarning().log(log)
        }
    }
    fun severe(log: String, context: CommandContext? = null){
        if(context != null){
            context.sendMessage(Message.raw(log).color("red"))
        } else {
            logger.atSevere().log(log)
        }
    }

    fun callSync(name: String, vararg args: Any){
        try {
            tsLoader.server?.invokeMember("callSync", name, *args)
        } catch(e: Exception){
            if(e is org.graalvm.polyglot.PolyglotException) {
                val location = e.sourceLocation
                val position = if(location != null) " [Line ${location.startLine}, Col ${location.startColumn}]" else ""

                logger.atSevere().log("❌ Script Error in '$name'$position: ${e.message}")

                e.polyglotStackTrace.forEach {frame ->
                    if(frame.isGuestFrame) {
                        logger.atSevere().log("    at ${frame.rootName}(${frame.sourceLocation})")
                    }
                }
            } else {
                logger.atSevere().log("❌ Internal Error while calling '$name': ${e.message}")
            }
        }
    }
    private fun registerEvents(){
        eventRegistry.register(PlayerConnectEvent::class.java) { e ->
            callSync("playerConnect", mutableMapOf(
                "playerRef" to e.playerRef,
                "world" to e.world
            ))
        }
        eventRegistry.register(PlayerDisconnectEvent::class.java) { e ->
            callSync("playerDisconnect", mutableMapOf(
                "playerRef" to e.playerRef,
                "reason" to e.disconnectReason
            ))
        }
        eventRegistry.register(PlayerReadyEvent::class.java) { e ->
            callSync("playerReady", mutableMapOf(
                "playerRef" to e.playerRef,
                "readyId" to e.readyId
            ))
        }
        eventRegistry.register(PlayerMouseButtonEvent::class.java) { e ->
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
        eventRegistry.register(PlayerChatEvent::class.java) { e ->
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