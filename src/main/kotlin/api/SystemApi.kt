package me.euaek.api

import com.hypixel.hytale.component.*
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.*
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.component.system.tick.TickingSystem
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.euaek.Plugin
import org.graalvm.polyglot.Value

class SystemApi(private val plugin: Plugin) {
    private class MasterData(val configs: MutableList<Value> = mutableListOf(), var registered: Boolean = false)

    private val masters = mutableMapOf<String, MasterData>()
    private val eventMasters = mutableMapOf<Class<out EcsEvent>, MasterData>()
    private val componentMasters = mutableMapOf<Class<Component<EntityStore>>, MasterData>()

    fun reload(){
        masters.values.forEach { it.configs.clear() }
        eventMasters.values.forEach { it.configs.clear() }
        componentMasters.values.forEach { it.configs.clear() }
    }
    fun shutdown(){
        masters.values.forEach { it.configs.clear() }
        eventMasters.values.forEach { it.configs.clear() }
        componentMasters.values.forEach { it.configs.clear() }
    }

    private fun getMaster(type: String) = masters.getOrPut(type) { MasterData() }

    fun addSystem(type: String, config: Value){
        val master = getMaster(type)
        master.configs.add(config)
        if(master.registered) return

        val system = when(type) {
            "ticking" -> object : TickingSystem<EntityStore>() {
                override fun tick(dt: Float, index: Int, store: Store<EntityStore>) {
                    process(master.configs) { cfg -> invokeSafe(cfg, "tick", dt, index, store) }
                }
            }
            "entityTicking" -> object : EntityTickingSystem<EntityStore>() {
                override fun getQuery(): Query<EntityStore> = query(config)

                override fun tick(dt: Float, index: Int, archetypeChunk: ArchetypeChunk<EntityStore>, store: Store<EntityStore>, commandBuffer: CommandBuffer<EntityStore>) {
                    process(master.configs) { cfg -> invokeSafe(cfg, "tick", dt, index, archetypeChunk, store, commandBuffer) }
                }
            }
            "delayedEntity" -> object : DelayedEntitySystem<EntityStore>(config.getMember("intervalSec").asFloat()) {
                override fun getQuery(): Query<EntityStore> = query(config)

                override fun tick(dt: Float, index: Int, archetypeChunk: ArchetypeChunk<EntityStore>, store: Store<EntityStore>, commandBuffer: CommandBuffer<EntityStore>) {
                    process(master.configs) { cfg -> invokeSafe(cfg, "tick", dt, index, archetypeChunk, store, commandBuffer) }
                }
            }
            else -> null
        }
        if(system != null){
            master.registered = true
            plugin.entityStoreRegistry.registerSystem(system)
            plugin.info("✅ Registered system: $type")
        }
    }
    fun addEventSystem(type: String, eventClass: Value, config: Value) {
        val eventType = eventClass.asHostObject<Class<EcsEvent>>()
        val master = eventMasters.getOrPut(eventType) {MasterData()}

        master.configs.add(config)
        if(master.registered) return

        val system = when(type) {
            "entityEvent" -> object : EntityEventSystem<EntityStore, EcsEvent>(eventType) {
                override fun getQuery(): Query<EntityStore> = query(config)

                override fun handle(
                    index: Int,
                    archetypeChunk: ArchetypeChunk<EntityStore>,
                    store: Store<EntityStore>,
                    commandBuffer: CommandBuffer<EntityStore>,
                    event: EcsEvent
                ) {
                    process(master.configs) { cfg -> invokeSafe(cfg, "handle", index, archetypeChunk, store, commandBuffer, event) }
                }
            }
            else -> null
        }
        if(system != null){
            master.registered = true
            plugin.entityStoreRegistry.registerSystem(system)
            plugin.info("✅ Registered event system: $type")
        }
    }
    fun addComponentSystem(type: String, componentClass: Value, config: Value) {
        val eventType = componentClass.asHostObject<Class<Component<EntityStore>>>()
        val master = componentMasters.getOrPut(eventType) {MasterData()}

        master.configs.add(config)
        if(master.registered) return

        val system = when(type) {
            "refChange" -> object : RefChangeSystem<EntityStore, Component<EntityStore>>() {
                override fun getQuery(): Query<EntityStore> = query(config)
                override fun componentType(): ComponentType<EntityStore, Component<EntityStore>> {
                    return invokeSafe(config, "componentType")!!.asHostObject()
                }
                override fun onComponentRemoved(ref: Ref<EntityStore>, component: Component<EntityStore>, store: Store<EntityStore>, commandBuffer: CommandBuffer<EntityStore>) {
                    invokeSafe(config, "onComponentRemoved", ref, component, store, commandBuffer)
                }
                override fun onComponentSet(ref: Ref<EntityStore>, valueComponent: Component<EntityStore>?, component: Component<EntityStore>, store: Store<EntityStore>, commandBuffer: CommandBuffer<EntityStore>) {
                    invokeSafe(config, "onComponentSet", ref, valueComponent, component, store, commandBuffer)
                }
                override fun onComponentAdded(ref: Ref<EntityStore>, component: Component<EntityStore>, store: Store<EntityStore>, commandBuffer: CommandBuffer<EntityStore>) {
                    invokeSafe(config, "onComponentAdded", ref, component, store, commandBuffer)
                }
            }
            else -> null
        }
        if(system != null){
            plugin.entityStoreRegistry.registerSystem(system)
            plugin.info("✅ Registered component system: $type")
        }
    }
    private inline fun process(list: MutableList<Value>, action: (Value) -> Unit) {
        val it = list.iterator()
        while(it.hasNext()) {
            val cfg = it.next()
            if(isContextActive(cfg)) action(cfg) else it.remove()
        }
    }

    private fun isContextActive(config: Value?): Boolean {
        if(config == null) return false
        return try {
            config.context
            true
        } catch(e: IllegalStateException) {
            false
        }
    }

    private fun invokeSafe(config: Value?, member: String, vararg args: Any?): Value? {
        if(!isContextActive(config)) return null

        return try {
            if(config!!.hasMember(member)) {
                config.invokeMember(member, *args)
            } else {
                null
            }
        } catch(e: Exception) {
            plugin.severe("❌ Error in system '$member': ${e.message}")
            null
        }
    }

    private fun query(cfg: Value?): Query<EntityStore> {
        if(!isContextActive(cfg)) return Archetype.empty()

        return try {
            val queryMember = cfg!!.getMember("query")
            when {
                queryMember == null || queryMember.isNull -> Archetype.empty()
                queryMember.isHostObject -> queryMember.asHostObject()
                queryMember.canExecute() -> queryMember.execute().asHostObject()
                else -> Archetype.empty()
            }
        } catch(e: Exception) {
            Archetype.empty()
        }
    }
}