package me.euaek.api

import com.hypixel.hytale.server.core.io.adapter.PacketAdapters
import com.hypixel.hytale.server.core.io.adapter.PacketFilter
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher
import me.euaek.Plugin
import org.graalvm.polyglot.Value

class PacketApi(private val plugin: Plugin) {
    private val inboundHandlers = mutableListOf<PacketFilter>()
    private val outboundHandlers = mutableListOf<PacketFilter>()

    private fun clear(){
        inboundHandlers.forEach {
            try {
                PacketAdapters.deregisterInbound(it)
            } catch(e: Exception) {}
        }
        outboundHandlers.forEach {
            try {
                PacketAdapters.deregisterOutbound(it)
            } catch(e: Exception) {}
        }

        inboundHandlers.clear()
        outboundHandlers.clear()
    }

    fun reload(){
        clear()
    }

    fun shutdown(){
        clear()
    }

    fun addAdapterInbound(callback: Value) {
        val filter = PacketFilter { handler, packet ->
            val result = callback.execute(handler, packet)
            if(result.isBoolean) result.asBoolean() else false
        }
        inboundHandlers.add(filter)
        PacketAdapters.registerInbound(filter)
    }
    fun addAdapterOutbound(callback: Value) {
        val filter = PacketFilter { handler, packet ->
            val result = callback.execute(handler, packet)
            if(result.isBoolean) result.asBoolean() else false
        }
        outboundHandlers.add(filter)
        PacketAdapters.registerOutbound(filter)
    }
}