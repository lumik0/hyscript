package me.euaek

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
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
}