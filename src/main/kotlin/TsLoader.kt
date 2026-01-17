package me.euaek

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.World
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.io.File
import java.nio.file.*
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

    fun setup() {
        if(!scriptsDir.exists()) scriptsDir.mkdirs()

        val res = "/sdk/declare/main.d.ts"
        val dest = plugin.dataDirectory.resolve("sdk.d.ts")

        if(!Files.exists(dest)) {
            Files.createDirectories(dest.parent)
            javaClass.getResourceAsStream(res)?.use { Files.copy(it, dest) }
        }

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
            plugin.info("🚀 [Hyscript] Using $transpiler for TypeScript support ($transpilerPath)", ctx)
        } else {
            plugin.warning("⚠️ [Hyscript] No TS transpiler found (Bun/esbuild). .ts files will be ignored", ctx)
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
                plugin.severe("❌ [Hyscript] Transpile Error ($transpiler): $error", ctx)
                return null
            }
            return result
        } catch(e: Exception) {
            plugin.severe("❌ [Hyscript] Failed to execute $transpiler at $path: ${e.message}", ctx)
            return null
        }
    }

    private fun load(ctx: CommandContext? = null) {
        try {
            plugin.serverApi.reload()

            context?.close()

            // 1. Creating context
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
                bindings.putMember("console", object {
                    fun log(msg: String) = logger.atInfo().log("[JS] $msg")
                    fun error(msg: String) = logger.atSevere().log("[JS] $msg")
                    fun warn(msg: String) = logger.atWarning().log("[JS] $msg")
                })
                bindings.putMember("plugin", plugin)
                bindings.putMember("__nativeServer", plugin.serverApi)
                bindings.putMember("server", plugin.serverApi)

                // Component
                bindings.putMember("TransformComponent", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.modules.entity.component.TransformComponent')"))

                // ECS
                bindings.putMember("Query", context?.eval("js", "Java.type('com.hypixel.hytale.component.query.Query')"))

                // Event (ECS)
                bindings.putMember("BreakBlockEvent", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent')"))

                // Math
                bindings.putMember("Vector3d", context?.eval("js", "Java.type('com.hypixel.hytale.math.vector.Vector3d')"))
                bindings.putMember("Axis", context?.eval("js", "Java.type('com.hypixel.hytale.math.Axis')"))
                bindings.putMember("Transform", context?.eval("js", "Java.type('com.hypixel.hytale.math.vector.Transform')"))

                // Hytale
                bindings.putMember("universe", Universe.get())
                bindings.putMember("World", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.universe.world.World')"))
                bindings.putMember("Message", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.Message')"))
                bindings.putMember("EventTitleUtil", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.util.EventTitleUtil')"))
                bindings.putMember("PlayerRef", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.universe.PlayerRef')"))
            }

            // 3. Loading SDK from resources
            val sdkStream = javaClass.getResourceAsStream("/sdk/dist/main.js")
            if(sdkStream != null) {
                val sdkSource = sdkStream.bufferedReader().use { it.readText() }
                context?.eval(Source.newBuilder("js", sdkSource, "sdk-main.js").build())
            } else {
                plugin.severe("❌ [Hyscript] SDK NOT FOUND IN RESOURCES!", ctx)
                throw Error("SDK not found")
            }

            // 4. Loading Scripts
            scriptsDir.listFiles {_, name ->
                (name.endsWith(".js") || name.endsWith(".ts")) && !name.startsWith("~")
            }?.forEach {file ->
                try {
                    val code = if(file.extension == "ts") transpileTs(file, ctx) else file.readText()
                    if(code != null) {
                        context?.eval(Source.newBuilder("js", code, file.name).build())
                        plugin.info("✅ [Hyscript] Loaded: ${file.name}", ctx)
                    }
                } catch(e: Exception) {
                    plugin.severe("❌ [Hyscript] Error in ${file.name}: ${e.message}", ctx)
                }
            }
        } catch(e: Exception) {
            plugin.severe("❌ [Hyscript] Critical Runtime Error: ${e.message}", ctx)
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
                    plugin.info("🔌 [Hyscript] Hot Reload disabled, stopping watcher...")
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