package me.euaek

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
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
            checkTranspilers()
        }
        val list = load(context)
        if(cfg.isHotReloadEnabled) {
            startWatcher()
        }
    }

    private fun checkTranspilers() {
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
            logger.atInfo().log("🚀 [Hyscript] Using $transpiler for TypeScript support ($transpilerPath)")
        } else {
            logger.atWarning().log("⚠️ [Hyscript] No TS transpiler found (Bun/esbuild). .ts files will be ignored.")
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
    private fun transpileTs(file: File): String? {
        val path = transpilerPath ?: return null
        val isWin = System.getProperty("os.name").contains("Win")

        val command = when(transpiler) {
            TranspilerType.BUN -> listOf(
                path,
                "build",
                file.absolutePath,
                "--minify",
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
                logger.atSevere().log("❌ [Hyscript] Transpile Error ($transpiler): $error")
                return null
            }
            return result
        } catch(e: Exception) {
            logger.atSevere().log("❌ [Hyscript] Failed to execute $transpiler at $path: ${e.message}")
            return null
        }
    }

    private fun load(ctx: CommandContext? = null) {
        try {
            context?.close()

            // 0. Creating context
            val hostAccess = HostAccess.newBuilder(HostAccess.ALL)
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

            // 0. Reload serverApi
            plugin.serverApi.reload()

            // 1. API
            context?.getBindings("js")?.let { bindings ->
                bindings.putMember("console", object {
                    fun log(msg: String) = logger.atInfo().log("[JS] $msg")
                    fun error(msg: String) = logger.atSevere().log("[JS] $msg")
                    fun warn(msg: String) = logger.atWarning().log("[JS] $msg")
                })
                bindings.putMember("plugin", plugin)
                bindings.putMember("__nativeServer", plugin.serverApi)
                bindings.putMember("server", plugin.serverApi)

                // Math
                bindings.putMember("Vector3d", context?.eval("js", "Java.type('com.hypixel.hytale.math.vector.Vector3d')"))
                bindings.putMember("Axis", context?.eval("js", "Java.type('com.hypixel.hytale.math.Axis')"))
                bindings.putMember("Transform", context?.eval("js", "Java.type('com.hypixel.hytale.math.vector.Transform')"))

                // Hytale
                bindings.putMember("Message", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.Message')"))
                bindings.putMember("EventTitleUtil", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.util.EventTitleUtil')"))
                bindings.putMember("World", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.Message')"))
                bindings.putMember("PlayerRef", context?.eval("js", "Java.type('com.hypixel.hytale.server.core.universe.PlayerRef')"))
            }

            // 2. Loading SDK from resources
            val sdkStream = javaClass.getResourceAsStream("/sdk/dist/main.js")
            if(sdkStream != null) {
                val sdkSource = sdkStream.bufferedReader().use { it.readText() }
                context?.eval(Source.newBuilder("js", sdkSource, "sdk-main.js").build())
            } else {
                ctx?.sendMessage(Message.raw("❌ [Hyscript] SDK NOT FOUND IN RESOURCES!"))
                logger.atSevere().log("❌ [Hyscript] SDK NOT FOUND IN RESOURCES!")
                throw Error("SDK not found")
            }

            // 3. Loading Scripts
            scriptsDir.listFiles {_, name ->
                (name.endsWith(".js") || name.endsWith(".ts")) && !name.startsWith("~")
            }?.forEach {file ->
                try {
                    val code = if(file.extension == "ts") transpileTs(file) else file.readText()
                    if(code != null) {
                        context?.eval(Source.newBuilder("js", code, file.name).build())
                        ctx?.sendMessage(Message.raw("✅ [Hyscript] Loaded: ${file.name}"))
                        logger.atInfo().log("✅ [Hyscript] Loaded: ${file.name}")
                    }
                } catch(e: Exception) {
                    ctx?.sendMessage(Message.raw("❌ [Hyscript] Error in ${file.name}: ${e.message}"))
                    logger.atSevere().log("❌ [Hyscript] Error in ${file.name}: ${e.message}")
                }
            }
        } catch(e: Exception) {
            ctx?.sendMessage(Message.raw("❌ [Hyscript] Critical Runtime Error: ${e.message}"))
            logger.atSevere().log("❌ [Hyscript] Critical Runtime Error: ${e.message}")
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
                    logger.atInfo().log("🔌 [Hyscript] Hot Reload disabled, stopping watcher...")
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
                    logger.atInfo().log("🔄 [Hyscript] Changes detected, reloading engine...")
                    load()
                }

                if(!key.reset()) break
            }
        }
    }
}