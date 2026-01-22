package me.euaek.api

import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.euaek.Plugin
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject

class ComponentApi(private val plugin: Plugin) {
    private val components = mutableListOf<CustomComponent>()

    fun reload() {
        components.clear()
    }

    fun shutdown(){
        components.clear()
    }

    fun createCustomComponent(value: Value): CustomComponent {
        require(value.canExecute()) { "createCustomComponent expects a function" }

        val component = CustomComponent(value)
        components.add(component)
        return component
    }

    inner class CustomComponent(private val createFn: Value) {

        @get:HostAccess.Export
        val type: ComponentType<EntityStore, JsComponent> = plugin.entityStoreRegistry.registerComponent(JsComponent::class.java) { JsComponent(createFn.getContext().eval("js", "({})")) }

        @HostAccess.Export
        fun create(vararg args: Any?): Any? {
            try {
                val result = createFn.execute(*args)
                return JsComponent(result)
            } catch(e: Exception) {
                plugin.severe("❌ Error creating component: ${e.message}")
                return null
            }
        }

        inner class JsComponent(private val data: Value) : Component<EntityStore>, ProxyObject {
            @HostAccess.Export
            override fun clone(): JsComponent = JsComponent(data)

            @HostAccess.Export
            override fun cloneSerializable(): JsComponent = clone()

            override fun getMember(key: String): Any? {
                return when(key) {
                    "clone" -> data.context.asValue { clone() }
                    "cloneSerializable" -> data.context.asValue { cloneSerializable() }
                    else -> {
                        val member = data.getMember(key)
                        if(member == null || member.isNull) null else member
                    }
                }
            }

            override fun putMember(key: String, value: Value?) {
                data.putMember(key, value)
            }

            override fun hasMember(key: String): Boolean {
                return when(key) {
                    "clone", "cloneSerializable" -> true
                    else -> data.hasMember(key)
                }
            }

            override fun getMemberKeys(): Any {
                val keys = data.memberKeys.toMutableSet()
                keys.add("clone")
                keys.add("cloneSerializable")
                return keys.toTypedArray()
            }
        }
    }
}