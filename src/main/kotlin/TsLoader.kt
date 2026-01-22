package me.euaek

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.universe.Universe
import org.graalvm.polyglot.*
import java.io.File
import java.net.URI
import java.nio.file.*
import java.util.*
import kotlin.concurrent.thread

object GlobalGraalEngine {
    val hostAccess: HostAccess = HostAccess.newBuilder(HostAccess.ALL)
        .allowAccessAnnotatedBy(HostAccess.Export::class.java)
        .allowPublicAccess(true)
        .targetTypeMapping(
            UUID::class.java,
            String::class.java,
            { it != null },
            { it.toString() }
        )
        .build()

    val engine: Engine by lazy {
        Engine.create()
    }
}

class TsLoader(private val plugin: Plugin) {
    private val logger = plugin.logger

    private val engine get() = GlobalGraalEngine.engine
    private var context: Context? = null

    private val scriptsDir = File(plugin.dataDirectory.toFile(), "scripts")

    enum class TranspilerType { BUN, ESBUILD, NONE }
    private var transpilerPath: String? = null
    private var transpiler = TranspilerType.NONE

    private var threadWatcher: Thread? = null
    private var watchService: WatchService? = null

    val server: Value?
        get() = context?.getBindings("js")?.getMember("server")

    private fun copyResources(resourcePath: String, targetDir: Path) {
        if(!Files.exists(targetDir)) Files.createDirectories(targetDir)

        val root = plugin.javaClass.getResource(resourcePath) ?: return
        val uri = root.toURI()

        if(uri.scheme == "jar") {
            val jarPath = uri.schemeSpecificPart.substringBefore("!")
            val jarFile = java.util.jar.JarFile(Paths.get(URI(jarPath)).toFile())

            jarFile.use { jar ->
                val entries = jar.entries()
                while(entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if(!entry.name.startsWith(resourcePath.removePrefix("/"))) continue
                    if(entry.isDirectory) continue

                    val relative = entry.name.removePrefix(resourcePath.removePrefix("/") + "/")
                    val dest = targetDir.resolve(relative)

                    Files.createDirectories(dest.parent)

                    jar.getInputStream(entry).use { input ->
                        Files.copy(input, dest, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        } else {
            Files.walk(Paths.get(uri)).forEach { path ->
                val relative = Paths.get(uri).relativize(path).toString()
                if(relative.isEmpty()) return@forEach

                val dest = targetDir.resolve(relative)
                if(Files.isDirectory(path)) {
                    Files.createDirectories(dest)
                } else {
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
    //    private fun copyResources(resourcePath: String, targetDir: Path) {
//        if(!Files.exists(targetDir)) Files.createDirectories(targetDir)
//
//        val uri = plugin.javaClass.getResource(resourcePath)?.toURI() ?: return
//
//        val fileSystem = if(uri.scheme == "jar") {
//            try { FileSystems.getFileSystem(uri) }
//            catch(e: Exception) { FileSystems.newFileSystem(uri, Collections.emptyMap<String, Any>()) }
//        } else null
//
//        val sourcePath = fileSystem?.getPath(resourcePath) ?: Paths.get(uri)
//
//        Files.walk(sourcePath).forEach { path ->
//            val relative = sourcePath.relativize(path).toString()
//            if(relative.isEmpty()) return@forEach
//
//            val dest = targetDir.resolve(relative)
//            if(Files.isDirectory(path)) {
//                if(!Files.exists(dest)) Files.createDirectories(dest)
//            } else {//if(!Files.exists(dest)) {
//                Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING)
//            }
//        }
//    }

    fun setup() {
        if(!scriptsDir.exists()) scriptsDir.mkdirs()

        copyResources("/sdk/declare", plugin.dataDirectory.resolve("sdk"))
        if(plugin.configManager.isNew)
            copyResources("/scripts", plugin.dataDirectory.resolve("scripts"))

        reload()

        plugin.serverApi.eventApi.callSync("setup")
    }
    fun reload(context: CommandContext? = null) {
        val cfg = plugin.configManager.current
        if(cfg.enableTypescript) {
            checkTranspilers(context)
        }
        val list = load(context)
        if(cfg.isHotReloadEnabled) {
            startWatcher()
        }
    }

    fun shutdown() {
        stopWatcher()
        context?.close()
        context = null
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
        if(!plugin.configManager.current.enableTypescript) return null
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
            context = Context.newBuilder("js")
                .engine(engine)
                .allowHostAccess(GlobalGraalEngine.hostAccess)
                .allowHostClassLookup { true }
                .build()

            // 2. API
            context?.getBindings("js")?.let { bindings ->
                bindings.putMember("plugin", plugin)
                bindings.putMember("__nativeServer", plugin.serverApi)
                bindings.putMember("server", plugin.serverApi)
                bindings.putMember("universe", Universe.get())
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
        stopWatcher()

        watchService = FileSystems.getDefault().newWatchService()
        scriptsDir.toPath().register(
            watchService,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_CREATE
        )

        threadWatcher = thread(isDaemon = true, name = "HyScript-Watcher") {
            try {
                while(plugin.configManager.current.isHotReloadEnabled) {
                    val key = watchService?.take() ?: break

                    val shouldReload = key.pollEvents().any {
                        val name = it.context().toString()
                        (name.endsWith(".js") || name.endsWith(".ts")) && !name.startsWith("~")
                    }

                    if(shouldReload) {
                        Thread.sleep(150)
                        plugin.info("🔄 [Hyscript] Changes detected, reloading...")
                        load()
                    }

                    if(!key.reset()) break
                }
            } catch(_: InterruptedException) {}
        }
    }

    private fun stopWatcher() {
        try { watchService?.close() } catch(_: Exception) {}
        threadWatcher?.interrupt()
        watchService = null
        threadWatcher = null
    }
}