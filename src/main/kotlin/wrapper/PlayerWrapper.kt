package me.euaek.wrapper

import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Transform
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.protocol.ItemWithAllMetadata
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.UUIDComponent
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.util.EventTitleUtil
import com.hypixel.hytale.server.core.util.NotificationUtil
import me.euaek.Plugin
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import java.util.*
import java.util.concurrent.CompletableFuture

class PlayerWrapper(
    private val playerRef: PlayerRef,
    private val player: Player,
    private val ref: Ref<EntityStore>,
    private val store: Store<EntityStore>
) {
    var health: Float?
        @HostAccess.Export get() = getStats()?.get(DefaultEntityStatTypes.getHealth())?.get()
        @HostAccess.Export set(value) { if(value != null) getStats()?.setStatValue(DefaultEntityStatTypes.getHealth(), value) }

    @HostAccess.Export
    val world: World = if(player.world != null) player.world!! else Universe.get().defaultWorld!!
    @HostAccess.Export
    val username = playerRef.username
    @HostAccess.Export
    val displayName = player.displayName
    @HostAccess.Export
    val inventory = player.inventory
    @HostAccess.Export
    val packetHandler = playerRef.packetHandler

    @HostAccess.Export
    fun getStats(): EntityStatMap? = store.getComponent(ref, EntityStatMap.getComponentType())

    @HostAccess.Export
    fun addComponent(componentType: Value, componentValue: Value) {
        val type = if(componentType.isHostObject) componentType.asHostObject<ComponentType<EntityStore, Component<EntityStore>>>() else null
        val value = if(componentValue.isHostObject) componentValue.asHostObject<Component<EntityStore>>() else null
        if(type == null || value == null ) return
        return store.addComponent(ref, type, value)
    }
    @HostAccess.Export
    fun getComponent(componentType: Value): Any? {
        val type = if(componentType.isHostObject) componentType.asHostObject<ComponentType<EntityStore, out Component<EntityStore>>>() else null
        if(type == null) return null
        return store.getComponent(ref, type)
    }

    @HostAccess.Export
    fun sendMessage(message: Message){
        playerRef.sendMessage(message)
    }
    @HostAccess.Export
    fun sendMessage(message: String){
        playerRef.sendMessage(Message.raw(message))
    }

    @HostAccess.Export
    fun sendEventTitle(primaryTitle: Any, secondaryTitle: Any, isMajor: Boolean, icon: String? = null, duration: Float? = null, fadeInDuration: Float? = null, fadeOutDuration: Float? = null) {
        val primaryTitle = if(primaryTitle is String) Message.raw(primaryTitle) else primaryTitle as Message
        val secondaryTitle = if(secondaryTitle is String) Message.raw(secondaryTitle) else secondaryTitle as Message

        if(duration != null && fadeInDuration != null && fadeOutDuration != null) EventTitleUtil.showEventTitleToPlayer(playerRef, primaryTitle, secondaryTitle, isMajor, icon, duration, fadeInDuration, fadeOutDuration)
        else EventTitleUtil.showEventTitleToPlayer(playerRef, primaryTitle, secondaryTitle, isMajor)
    }

    @HostAccess.Export
    fun sendNotification(message: Any, secondary: Any? = null, icon: String? = null, item: ItemStack? = null, style: NotificationStyle? = null) {
        val msg = if(message is String) Message.raw(message) else message as Message
        val secMsg = if(secondary is String) Message.raw(secondary) else secondary as? Message

        NotificationUtil.sendNotification(playerRef.packetHandler, msg, secMsg ?: Message.empty(), icon ?: "", item as ItemWithAllMetadata, style ?: NotificationStyle.Default)
    }

    @HostAccess.Export
    fun teleport(x: Double, y: Double, z: Double, yaw: Float?, pitch: Float?) {
        val tel = {
            val teleport = if(yaw != null && pitch != null) Teleport(Vector3d(x, y, z), Vector3f(yaw, pitch)) else Teleport(Transform(x, y, z))

            store.addComponent(ref, Teleport.getComponentType(), teleport)
        }

        if(!world.isInThread) world.execute(tel)
        else tel()
    }
    @HostAccess.Export
    fun teleport(v: Vector3d, r: Vector3f?) {
        val tel = {
            val teleport = if(r != null) Teleport(world, v, r) else Teleport(world, Transform(v))

            store.addComponent(ref, Teleport.getComponentType(), teleport)
        }
        if(!world.isInThread) world.execute(tel)
        else tel()
    }

    @HostAccess.Export
    fun kill() {
    }

    @HostAccess.Export
    fun getUuid(): UUID? = getComponent(UUIDComponent.getComponentType())?.uuid
    @HostAccess.Export
    fun getPosition(): Vector3d? = getComponent(TransformComponent.getComponentType())?.position
    @HostAccess.Export
    fun setPosition(value: Vector3d?) {
        if(value == null) return
        store.getComponent(ref, TransformComponent.getComponentType())?.position = value
    }
    @HostAccess.Export
    fun getRotation(): Vector3f? = getComponent(TransformComponent.getComponentType())?.rotation
    @HostAccess.Export
    fun setRotation(value: Vector3f?) {
        if(value == null) return
        store.getComponent(ref, TransformComponent.getComponentType())?.rotation = value
    }

    private fun <T : Component<EntityStore>> getComponent(componentType: ComponentType<EntityStore, T>): T? = store.getComponent(ref, componentType)
}