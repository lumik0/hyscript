package me.euaek.api

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Transform
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.euaek.Plugin
import me.euaek.wrapper.PlayerWrapper
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import java.util.UUID
import java.util.concurrent.CompletableFuture

class ServerApi(private val plugin: Plugin) {
    private val logger = plugin.logger

    private val commandApi = CommandApi(plugin)
    private val systemApi = SystemApi(plugin)
    private val packetApi = PacketApi(plugin)
    private val componentApi = ComponentApi(plugin)
    val eventApi = EventApi(plugin)

    fun reload(){
        commandApi.reload()
        systemApi.reload()
        packetApi.reload()
        componentApi.reload()
    }

    fun shutdown(){
        commandApi.shutdown()
        systemApi.shutdown()
        packetApi.shutdown()
        componentApi.shutdown()
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
    fun addComponentSystem(type: String, componentClass: Value, config: Value) {
        systemApi.addComponentSystem(type, componentClass, config)
    }

    @HostAccess.Export
    fun addAdapterInbound(value: Value){
        packetApi.addAdapterInbound(value)
    }
    @HostAccess.Export
    fun addAdapterOutbound(value: Value){
        packetApi.addAdapterOutbound(value)
    }

    @HostAccess.Export
    fun createCustomComponent(value: Value): ComponentApi.CustomComponent {
        return componentApi.createCustomComponent(value)
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
                else if(args.size == 1 && args[0] is Value) Transform((args[0] as Value).invokeMember("getX").asDouble(), (args[0] as Value).invokeMember("getY").asDouble(), (args[0] as Value).invokeMember("getZ").asDouble())
                else if(args.size == 3) Transform((args[0] as Number).toDouble(), (args[1] as Number).toDouble(), (args[2] as Number).toDouble())
                else Transform(0.0, 0.0, 0.0)
            }
            "message" -> {
                if(args.isEmpty()) Message.empty()
                else Message.raw(args[0].toString())
            }
            "vector3d" -> {
                if(args.size == 1 && args[0] is Vector3d) Vector3d(args[0] as Vector3d)
                else if(args.size == 1 && args[0] is Value) Vector3d((args[0] as Value).invokeMember("getX").asDouble(), (args[0] as Value).invokeMember("getY").asDouble(), (args[0] as Value).invokeMember("getZ").asDouble())
                else if(args.size == 3) Vector3d((args[0] as Number).toDouble(), (args[1] as Number).toDouble(), (args[2] as Number).toDouble())
                else if(args.size == 2) Vector3d((args[0] as Number).toFloat(), (args[1] as Number).toFloat())
                else Vector3d()
            }
            "vector3i" -> {
                if(args.size == 1 && args[0] is Vector3i) Vector3i(args[0] as Vector3i)
                else if(args.size == 1 && args[0] is Value) Vector3i((args[0] as Value).invokeMember("getX").asInt(), (args[0] as Value).invokeMember("getY").asInt(), (args[0] as Value).invokeMember("getZ").asInt())
                else if(args.size == 3) Vector3i((args[0] as Number).toInt(), (args[1] as Number).toInt(), (args[2] as Number).toInt())
                else Vector3i()
            }
            else -> null
        }
    }
//
//    @HostAccess.Export
//    fun getPlayer(v: Value): CompletableFuture<PlayerWrapper?> {
//        val cf = CompletableFuture<PlayerWrapper?>()
//        val universe = Universe.get()
//
//        var playerRef = if(v.hasMember("playerRef")) v.getMember("playerRef").asHostObject<PlayerRef>() else null
//        var player = if(v.hasMember("player")) v.getMember("player").asHostObject<Player>() else null
//        var store = if(v.hasMember("store")) v.getMember("store").asHostObject<Store<EntityStore>>() else null
//        var ref = if(v.hasMember("ref")) v.getMember("ref").asHostObject<Ref<EntityStore>>() else null
//        var world = if(v.hasMember("world")) v.getMember("world").asHostObject<World>() else null
//        val uuid = if(v.hasMember("uuid")) v.getMember("uuid").asHostObject<UUID>() else null
//        val commandContext = if(v.hasMember("commandContext")) v.getMember("commandContext").asHostObject<CommandContext>() else null
//        val index = if(v.hasMember("index")) v.getMember("index").asInt() else null
//        val archetypeChunk = if(v.hasMember("archetypeChunk")) v.getMember("archetypeChunk").asHostObject<ArchetypeChunk<EntityStore>>() else null
//
//        if(commandContext != null && commandContext.isPlayer && player == null) player = commandContext as Player
//        if(uuid != null && playerRef == null) playerRef = universe.getPlayer(uuid)
//        if(player != null && world == null) world = player.world
//        if(playerRef != null && world == null && playerRef.worldUuid != null) world = universe.getWorld(playerRef.worldUuid!!)
//        if(player != null && store == null) store = player.reference?.store
//        if(playerRef != null && ref == null) ref = playerRef.reference
//        if(ref != null && store == null) store = ref.store
//
//        if(archetypeChunk != null && index != null && ref == null) ref = archetypeChunk.getReferenceTo(index)
//
//        if(world == null) {
//            if(playerRef != null && player != null) cf.complete(PlayerWrapper(playerRef, player, ref, store))
//            else cf.complete(null)
//            return cf
//        }
//
//        world.execute {
//            try {
//                if(store != null && ref != null) {
//                    if(playerRef == null) playerRef = store.getComponent(ref, PlayerRef.getComponentType())
//                    if(player == null) player = store.getComponent(ref, Player.getComponentType())
//                }
//
//                if(playerRef != null && player != null) {
//                    cf.complete(PlayerWrapper(playerRef!!, player!!, ref, store))
//                } else {
//                    cf.complete(null)
//                }
//            } catch(e: Exception) {
//                plugin.severe("❌ Error in getPlayer: ${e.message}")
//                cf.complete(null)
//            }
//        }
//
//        return cf
//    }

    @HostAccess.Export
    fun getPlayer(v: Value): PlayerWrapper? {
        val universe = Universe.get()

        var playerRef = if(v.hasMember("playerRef")) v.getMember("playerRef").asHostObject<PlayerRef>() else null
        var player = if(v.hasMember("player")) v.getMember("player").asHostObject<Player>() else null
        var store = if(v.hasMember("store")) v.getMember("store").asHostObject<Store<EntityStore>>() else null
        var ref = if(v.hasMember("ref")) v.getMember("ref").asHostObject<Ref<EntityStore>>() else null
        var world = if(v.hasMember("world")) v.getMember("world").asHostObject<World>() else null
        val uuid = if(v.hasMember("uuid")) v.getMember("uuid").asHostObject<UUID>() else null
        val commandContext = if(v.hasMember("commandContext")) v.getMember("commandContext").asHostObject<CommandContext>() else null
        val index = if(v.hasMember("index")) v.getMember("index").asInt() else null
        val archetypeChunk = if(v.hasMember("archetypeChunk")) v.getMember("archetypeChunk").asHostObject<ArchetypeChunk<EntityStore>>() else null

        if(commandContext != null && commandContext.isPlayer && player == null) player = commandContext as Player
        if(uuid != null && playerRef == null) playerRef = universe.getPlayer(uuid)
        if(player != null && world == null) world = player.world
        if(playerRef != null && world == null && playerRef.worldUuid != null) world = universe.getWorld(playerRef.worldUuid!!)
        if(player != null && store == null) store = player.reference?.store
        if(playerRef != null && ref == null) ref = playerRef.reference
        if(ref != null && store == null) store = ref.store

        if(archetypeChunk != null && index != null && ref == null) ref = archetypeChunk.getReferenceTo(index)

        if(store != null && ref != null) {
            if(playerRef == null) playerRef = store.getComponent(ref, PlayerRef.getComponentType())
            if(player == null) player = store.getComponent(ref, Player.getComponentType())
        }

        if(playerRef != null && player != null && ref != null && store != null) {
            return PlayerWrapper(playerRef, player, ref, store)
        }

        return null
    }
}