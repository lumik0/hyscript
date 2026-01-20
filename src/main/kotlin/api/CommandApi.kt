package me.euaek.api

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
import com.hypixel.hytale.server.core.command.system.arguments.system.Argument
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import me.euaek.Plugin
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.concurrent.CompletableFuture

class CommandApi(private val plugin: Plugin) {
    private val commands = mutableListOf<AbstractCommand>()

    fun reload(){
        commands.clear()
    }

    fun shutdown(){
        commands.clear()
    }

    fun addCommand(config: Value) {
        val cmd = parseCommand(config)
        if(cmd != null) {
            if(!commands.contains(cmd))
                commands.add(cmd)
            plugin.commandRegistry.registerCommand(cmd)
            plugin.info("✅ Registered command: ${cmd.name}")
        }
    }

    private fun parseCommand(config: Value): AbstractCommand? {
        val type = config.getMember("type")?.asString() ?: "default"
        val name = config.getMember("name")?.asString() ?: return null
        val desc = config.getMember("description")?.asString() ?: ""
        val requiresConfirmation = config.getMember("requiresConfirmation")?.asBoolean() ?: false

        val argsMap = mutableMapOf<String, Argument<*, *>>()

        val cmd = when(type) {
            "collection" -> object : AbstractCommandCollection(name, desc) {}
            "player" -> object : AbstractPlayerCommand(name, desc, requiresConfirmation) {
                override fun execute(context: CommandContext, store: Store<EntityStore>, ref: Ref<EntityStore>, playerRef: PlayerRef, world: World) {
                    if(!commands.contains(this)) return
                    val args = ProxyObject.fromMap(argsMap.mapValues { context.get(it.value) })
                    config.getMember("execute").execute(context, args, store, ref, playerRef, world)
                }
            }
            else -> object : AbstractAsyncCommand(name, desc, requiresConfirmation) {
                override fun executeAsync(context: CommandContext): CompletableFuture<Void> {
                    if(!commands.contains(this)) CompletableFuture.runAsync {}
                    return CompletableFuture.runAsync { config.getMember("execute").execute(context) }
                }
            }
        }

        val args = config.getMember("args")
        if(args != null && args.hasArrayElements()) {
            for(i in 0 until args.arraySize) {
                val argCfg = args.getArrayElement(i)
                val name = argCfg.getMember("name").asString()
                val description = argCfg.getMember("description").asString()
                val type = when(argCfg.getMember("type").asString()){
                    "string" -> ArgTypes.STRING
                    "integer" -> ArgTypes.INTEGER
                    "float" -> ArgTypes.FLOAT
                    "double" -> ArgTypes.DOUBLE
                    "boolean" -> ArgTypes.BOOLEAN
                    "playerRef" -> ArgTypes.PLAYER_REF
                    "playerUuid" -> ArgTypes.PLAYER_UUID
                    "uuid" -> ArgTypes.UUID
                    "gameMode" -> ArgTypes.GAME_MODE
                    "world" -> ArgTypes.WORLD
                    else -> ArgTypes.STRING
                    // TODO: add more args
                }
                val required = argCfg.getMember("required")?.asBoolean() ?: false
                val default = argCfg.getMember("default")?.asBoolean() ?: false
                val arg = when {
                    required -> cmd.withRequiredArg(name, description, type)
                    default -> cmd.withDefaultArg(name, description, type, argCfg.getMember("defaultValue")?.asHostObject(), argCfg.getMember("defaultValueDescription")?.asString() ?: description)
                    else -> cmd.withOptionalArg(name, description, type)
                }
                argsMap[name] = arg
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
                val subCmd = parseCommand(subConfig)
                if(subCmd != null) cmd.addSubCommand(subCmd)
            }
        }

        return cmd
    }
}