package me.euaek.api

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.EcsEvent
import com.hypixel.hytale.component.system.EntityEventSystem
import com.hypixel.hytale.component.system.EventSystem
import com.hypixel.hytale.component.system.System
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.euaek.Plugin
import org.graalvm.polyglot.Value

class SystemApi(private val plugin: Plugin) {
    private val eventProxies = mutableMapOf<Class<out EcsEvent>, EntityEventProxy>()

    fun reload(){
        eventProxies.values.forEach { it.activeConfig = null }
    }

    fun addSystem(type: String, config: Value){

    }
    fun addEventSystem(type: String, eventClass: Value, config: Value) {
        val eventTypeJavaClass = if(eventClass.isHostObject) {
            eventClass.asHostObject<Class<*>>() as Class<EcsEvent>
        } else {
            throw IllegalArgumentException("eventClass must be a Java Class")
        }

        when(type) {
            "entityEvent" -> {
                val proxy = eventProxies.getOrPut(eventTypeJavaClass) {
                    EntityEventProxy(eventTypeJavaClass).also {
                        plugin.entityStoreRegistry.registerSystem(it)
                    }
                }

                proxy.activeConfig = config
                plugin.info("✅ [Hyscript] Registered system: ${eventTypeJavaClass.simpleName}")
            }
        }
    }

    inner class EntityEventProxy(eventType: Class<EcsEvent>) : EntityEventSystem<EntityStore, EcsEvent>(eventType) {
        var activeConfig: Value? = null

        override fun getQuery(): Query<EntityStore>? {
            val cfg = activeConfig ?: return Query.any()

            return try {
                val queryMember = cfg.getMember("query")

                if(queryMember == null || queryMember.isNull) return Query.any()

                when {
                    queryMember.isHostObject -> queryMember.asHostObject<Query<EntityStore>>()
                    queryMember.canExecute() -> queryMember.execute().asHostObject<Query<EntityStore>>()
                    else -> Query.any()
                }
            } catch(e: Exception) {
                activeConfig = null
                Query.any()
            }
        }

        override fun handle(
            index: Int,
            archetypeChunk: ArchetypeChunk<EntityStore>,
            store: Store<EntityStore>,
            commandBuffer: CommandBuffer<EntityStore>,
            event: EcsEvent
        ) {
            val cfg = activeConfig ?: return

            try {
                cfg.invokeMember("handle", index, archetypeChunk, store, commandBuffer, event)
            } catch(e: Exception) {
                activeConfig = null
            }
        }
    }
}