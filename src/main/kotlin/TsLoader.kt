package me.euaek

import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.protocol.GameMode
import com.hypixel.hytale.protocol.Packet
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import com.hypixel.hytale.server.core.command.system.AbstractCommand
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.CommandSender
import com.hypixel.hytale.server.core.command.system.arguments.system.Argument
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType
import com.hypixel.hytale.server.core.entity.Entity
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import com.hypixel.hytale.server.core.inventory.Inventory
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.inventory.container.ItemContainer
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.physics.component.Velocity
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.playerdata.PlayerStorage
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.WorldConfig
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.util.NotificationUtil
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.io.File
import java.nio.file.*
import java.util.*
import kotlin.concurrent.thread

class TsLoader(private val plugin: Plugin) {
    private val logger = plugin.logger

    private var context: Context? = null

    private val scriptsDir = File(plugin.dataDirectory.toFile(), "scripts")

    enum class TranspilerType { BUN, ESBUILD, NONE }
    private var transpilerPath: String? = null
    private var transpiler = TranspilerType.NONE

    private var threadWatcher: Thread? = null

    val server: Value?
        get() = context?.getBindings("js")?.getMember("server")

    private fun copyResources(resourcePath: String, targetDir: Path) {
        if(!Files.exists(targetDir)) Files.createDirectories(targetDir)

        val uri = plugin.javaClass.getResource(resourcePath)?.toURI() ?: return

        val fileSystem = if(uri.scheme == "jar") {
            try { FileSystems.getFileSystem(uri) }
            catch(e: Exception) { FileSystems.newFileSystem(uri, Collections.emptyMap<String, Any>()) }
        } else null

        val sourcePath = fileSystem?.getPath(resourcePath) ?: Paths.get(uri)

        Files.walk(sourcePath).forEach { path ->
            val relative = sourcePath.relativize(path).toString()
            if(relative.isEmpty()) return@forEach

            val dest = targetDir.resolve(relative)
            if(Files.isDirectory(path)) {
                if(!Files.exists(dest)) Files.createDirectories(dest)
            } else {//if(!Files.exists(dest)) {
                Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    fun setup() {
        if(!scriptsDir.exists()) scriptsDir.mkdirs()

        copyResources("/sdk/declare", plugin.dataDirectory.resolve("sdk"))
        if(plugin.configManager.isNew)
            copyResources("/scripts", plugin.dataDirectory.resolve("scripts"))

        reload()

        plugin.callSync("setup")
    }
    fun reload(context: CommandContext? = null) {
        val cfg = plugin.configManager.current
        if(cfg.typescript) {
            checkTranspilers(context)
        }
        val list = load(context)
        if(cfg.isHotReloadEnabled) {
            startWatcher()
        }
    }

    private fun checkTranspilers(ctx: CommandContext? = null) {
        val bunPath = findCommandPath("bun")
        if(bunPath != null) {
            transpilerPath = bunPath
            transpiler = TranspilerType.BUN
        } else {
            val esbuildPath = findCommandPath("esbuild")
            if(esbuildPath != null) {
                transpilerPath = esbuildPath
                transpiler = TranspilerType.ESBUILD
            }
        }

        if(transpiler != TranspilerType.NONE) {
            plugin.info("🚀 Using $transpiler for TypeScript support ($transpilerPath)", ctx)
        } else {
            plugin.warning("⚠️ No TS transpiler found (Bun/esbuild). .ts files will be ignored", ctx)
        }
    }
    private fun findCommandPath(cmd: String): String? {
        return try {
            val isWin = System.getProperty("os.name").contains("Win")
            val command = if(isWin) listOf("where", cmd) else listOf("which", cmd)
            val process = ProcessBuilder(command).start()
            val path = process.inputStream.bufferedReader().readLine()
            process.waitFor()
            if(process.exitValue() == 0 && path != null && File(path).exists()) path else null
        } catch(e: Exception) {
            null
        }
    }
    private fun transpileTs(file: File, ctx: CommandContext? = null): String? {
        val path = transpilerPath ?: return null
        val isWin = System.getProperty("os.name").contains("Win")

        val command = when(transpiler) {
            TranspilerType.BUN -> listOf(
                path,
                "build",
                file.absolutePath,
                "--minify",
                "--bundle",
                "--target", "browser"
            )
            TranspilerType.ESBUILD -> listOf(
                path,
                file.absolutePath,
                "--bundle",
                "--format=iife",
                "--target=es2020"
            )
            TranspilerType.NONE -> return null
        }

        val finalCommand = if(isWin) {
            listOf("cmd", "/c") + command
        } else {
            command
        }

        try {
            val process = ProcessBuilder(finalCommand)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            val result = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()

            if(process.exitValue() != 0) {
                plugin.severe("❌ Transpile Error ($transpiler): $error", ctx)
                return null
            }
            return result
        } catch(e: Exception) {
            plugin.severe("❌ Failed to execute $transpiler at $path: ${e.message}", ctx)
            return null
        }
    }

    private fun load(ctx: CommandContext? = null) {
        try {
            plugin.serverApi.reload()
            context?.close()

            // 1. context
            val hostAccess = HostAccess.newBuilder(HostAccess.ALL)
                .allowAccessAnnotatedBy(HostAccess.Export::class.java)
                .allowPublicAccess(true)
                .targetTypeMapping(
                    java.util.UUID::class.java,
                    String::class.java,
                    { it != null },
                    { it.toString() }
                )
                .build()

            context = Context.newBuilder("js")
                .allowHostAccess(hostAccess)
                .allowHostClassLookup { true }
                .build()

            // 2. API
            context?.getBindings("js")?.let { bindings ->
//                bindings.putMember("console", object {
//                    fun log(msg: String) = logger.atInfo().log("[JS] $msg")
//                    fun error(msg: String) = logger.atSevere().log("[JS] $msg")
//                    fun warn(msg: String) = logger.atWarning().log("[JS] $msg")
//                })

                bindings.putMember("plugin", plugin)
                bindings.putMember("__nativeServer", plugin.serverApi)
                bindings.putMember("server", plugin.serverApi)
                bindings.putMember("universe", Universe.get())
                bindings.putMember("Transform", context?.eval("js", "Java.type('com.hypixel.hytale.math.vector.Transform')"))
                bindings.putMember("Component", context?.eval("js", "Java.type('com.hypixel.hytale.component.Component')"))
                bindings.putMember("ComponentType", context?.eval("js", "Java.type('com.hypixel.hytale.component.ComponentType')"))
                bindings.putMember("Ref", context?.eval("js", "Java.type('com.hypixel.hytale.component.Ref')"))
                bindings.putMember("Holder", context?.eval("js", "Java.type('com.hypixel.hytale.component.Holder')"))
                bindings.putMember("Store", context?.eval("js", "Java.type('com.hypixel.hytale.component.Store')"))
                bindings.putMember("EntityStore", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.universe.world.storage.EntityStore')"))
                bindings.putMember("ComponentAccessor", context?.eval("js", "Java.type('com.hypixel.hytale.component.ComponentAccessor')"))
                bindings.putMember("Query", context?.eval("js", "Java.type('com.hypixel.hytale.component.query.Query')"))
                bindings.putMember("NotQuery", context?.eval("js", "Java.type('com.hypixel.hytale.component.query.NotQuery')"))
                bindings.putMember("AndQuery", context?.eval("js", "Java.type('com.hypixel.hytale.component.query.AndQuery')"))
                bindings.putMember("OrQuery", context?.eval("js", "Java.type('com.hypixel.hytale.component.query.OrQuery')"))
                bindings.putMember("Archetype", context?.eval("js", "Java.type('com.hypixel.hytale.component.Archetype')"))
                bindings.putMember("ExactArchetypeQuery", context?.eval("js", "Java.type('com.hypixel.hytale.component.query.ExactArchetypeQuery')"))
                bindings.putMember("EcsEvent", context?.eval("js", "Java.type('com.hypixel.hytale.component.system.EcsEvent')"))
                bindings.putMember("CancellableEcsEvent", context?.eval("js", "Java.type('com.hypixel.hytale.component.system.CancellableEcsEvent')"))
                bindings.putMember("BreakBlockEvent", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent')"))
                bindings.putMember("TransformComponent", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.modules.entity.component.TransformComponent')"))
                bindings.putMember("Velocity", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.modules.physics.component.Velocity')"))
                bindings.putMember("Teleport", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.modules.entity.teleport.Teleport')"))
                bindings.putMember("CommandSender", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.command.system.CommandSender')"))
                bindings.putMember("CommandContext", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.command.system.CommandContext')"))
                bindings.putMember("ArgumentType", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType')"))
                bindings.putMember("Argument", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.command.system.arguments.system.Argument')"))
                bindings.putMember("RequiredArg", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg')"))
                bindings.putMember("AbstractCommand", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.command.system.AbstractCommand')"))
                bindings.putMember("Universe", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.universe.Universe')"))
                bindings.putMember("World", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.universe.world.World')"))
                bindings.putMember("PlayerStorage", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.universe.playerdata.PlayerStorage')"))
                bindings.putMember("Packet", context?.eval("js", "Java.type('com.hypixel.hytale.protocol.Packet')"))
                bindings.putMember("Message", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.Message')"))
                bindings.putMember("GameMode", context?.eval("js", "Java.type('com.hypixel.hytale.protocol.GameMode')"))
                bindings.putMember("PacketHandler", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.io.PacketHandler')"))
                bindings.putMember("EventTitleUtil", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.util.EventTitleUtil')"))
                bindings.putMember("NotificationUtil", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.util.NotificationUtil')"))
                bindings.putMember("WorldConfig", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.universe.world.WorldConfig')"))
                bindings.putMember("DeathConfig", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig')"))
                bindings.putMember("PlayerRef", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.universe.PlayerRef')"))
                bindings.putMember("Entity", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.entity.Entity')"))
                bindings.putMember("LivingEntity", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.entity.LivingEntity')"))
                bindings.putMember("Player", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.entity.entities.Player')"))
                bindings.putMember("Item", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.asset.type.item.config.Item')"))
                bindings.putMember("ItemStack", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.inventory.ItemStack')"))
                bindings.putMember("Inventory", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.inventory.Inventory')"))
                bindings.putMember("ItemContainer", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.inventory.container.ItemContainer')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
//                bindings.putMember("", context?.eval("js", "Java.type('')"))
            }

            // 3. Loading SDK from resources
            val sdkStream = javaClass.getResourceAsStream("/sdk/dist/main.js")
            if(sdkStream != null) {
                val sdkSource = sdkStream.bufferedReader().use { it.readText() }
                context?.eval(Source.newBuilder("js", sdkSource, "sdk-main.js").build())
            } else {
                plugin.severe("❌ SDK NOT FOUND IN RESOURCES!", ctx)
                throw Error("SDK not found")
            }

            // 4. Loading Scripts
            scriptsDir.listFiles {_, name ->
                (name.endsWith(".js") || name.endsWith(".ts")) && !name.startsWith("~")
            }?.forEach {file ->
                try {
                    val code = if(file.extension == "ts") transpileTs(file, ctx) else file.readText()
                    if(code != null) {
                        context?.getBindings("js")?.let { bindings ->
                            bindings.putMember("console", object {
                                fun log(msg: Value) = logger.atInfo().log("[${file.name}] $msg")
                                fun error(msg: Value) = logger.atSevere().log("[${file.name}] $msg")
                                fun warn(msg: Value) = logger.atWarning().log("[${file.name}] $msg")
                            })
                        }
                        context?.eval(Source.newBuilder("js", code, file.name).build())
                        plugin.info("✅ Loaded: ${file.name}", ctx)
                    }
                } catch(e: Exception) {
                    if(e is org.graalvm.polyglot.PolyglotException) {
                        val location = e.sourceLocation
                        val lineInfo = if(location == null) "unknown location" else "at ${location.startLine}:${location.startColumn}"

                        plugin.severe("❌ Error in ${file.name} ($lineInfo):", ctx)
                        plugin.severe("   > ${e.message}", ctx)

                        e.polyglotStackTrace.forEach { frame ->
                            plugin.severe("     at ${frame.rootName}(${frame.sourceLocation})", ctx)
                        }
                    } else {
                        plugin.severe("❌ Unexpected error in ${file.name}: ${e.message}", ctx)
                    }
                }
            }
        } catch(e: Exception) {
            plugin.severe("❌ Critical Runtime Error: ${e.message}", ctx)
            e.printStackTrace()
        }
    }

    private fun startWatcher() {
        threadWatcher?.interrupt()
        threadWatcher = thread(isDaemon = true) {
            val watcher = FileSystems.getDefault().newWatchService()
            scriptsDir.toPath().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE)

            while(true) {
                if(!plugin.configManager.current.isHotReloadEnabled) {
                    plugin.info("⭕ [Hyscript] Hot Reload disabled, stopping watcher...")
                    break
                }

                val key = watcher.take()
                val events = key.pollEvents()

                val shouldReload = events.any {
                    val name = it.context().toString()
                    (name.endsWith(".js") || name.endsWith(".ts")) && !name.startsWith("~")
                }

                if(shouldReload) {
                    Thread.sleep(150)
                    plugin.info("🔄 [Hyscript] Changes detected, reloading engine...")
                    load()
                }

                if(!key.reset()) break
            }
        }
    }
}