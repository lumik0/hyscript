package me.euaek

import com.hypixel.hytale.server.core.command.system.AbstractCommand
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.HostAccess
import java.util.concurrent.CompletableFuture

class CommandApi(private val plugin: Plugin) {
    private val logger = plugin.logger
    private val commands = mutableListOf<AbstractCommand>()

    fun addCommand(config: Value) {
        val cmd = parseCommand(config)
        if(cmd != null) {
            if(!commands.contains(cmd))
                commands.add(cmd)
            plugin.commandRegistry.registerCommand(cmd)
            logger.atInfo().log("✅ [Hyscript] Registered command: ${cmd.name}")
        }
    }

    private fun parseCommand(config: Value): AbstractCommand? {
        val name = config.getMember("name")?.asString() ?: return null
        val desc = config.getMember("description")?.asString() ?: ""
        val type = config.getMember("type")?.asString() ?: "default"

        val cmd = when(type) {
            "collection" -> object : AbstractCommandCollection(name, desc) {}
            "player" -> object : AbstractPlayerCommand(name, desc) {
                override fun execute(context: CommandContext, store: Store<EntityStore>, ref: Ref<EntityStore>, playerRef: PlayerRef, world: World) {
                    config.getMember("execute").execute(context, store, ref, playerRef, world)
                }
            }
            else -> object : AbstractAsyncCommand(name, desc) {
                override fun executeAsync(context: CommandContext): CompletableFuture<Void> {
                    return CompletableFuture.runAsync {
                        config.getMember("execute").execute(context)
                    }
                }
            }
        }

        val aliases = config.getMember("aliases")
        if(aliases != null && aliases.hasArrayElements()) {
            for(i in 0 until aliases.arraySize) {
                cmd.addAliases(aliases.getArrayElement(i).asString())
            }
        }

        if(config.hasMember("permission")) {
            cmd.requirePermission(config.getMember("permission").asString())
        }

        val subs = config.getMember("subCommands")
        if(subs != null && subs.hasMembers()) {
            for(key in subs.memberKeys) {
                val subConfig = subs.getMember(key)
                if(!subConfig.hasMember("name")) {
                    // Временно "инжектим" имя, если его нет
                    // (в реальной реализации лучше передать name в parseCommand)
                }
                val subCmd = parseCommand(subConfig)
                if(subCmd != null) cmd.addSubCommand(subCmd)
            }
        }

        return cmd
    }
}