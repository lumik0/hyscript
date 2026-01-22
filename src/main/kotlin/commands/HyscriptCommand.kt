package me.euaek.commands

import au.ellie.hyui.builders.PageBuilder
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.permissions.HytalePermissions
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.euaek.Plugin

class HyscriptCommand : AbstractCommandCollection("hyscript", "Hyscript") {
    init {
        addAliases("hs")
        addSubCommand(ReloadCommand())
        addSubCommand(ReloadConfigCommand())
        addSubCommand(ReloadScriptsCommand())
        requirePermission(HytalePermissions.fromCommand("hyscript.self"));
    }

    class ReloadCommand : AbstractPlayerCommand("reload", "Reload") {
        override fun execute(context: CommandContext, store: Store<EntityStore>, ref: Ref<EntityStore>, playerRef: PlayerRef, world: World) {
            Plugin.instance.configManager.load()
            Plugin.instance.tsLoader.reload(context)
            context.sendMessage(Message.raw("Reloaded config and scripts"))
        }
    }
    class ReloadConfigCommand : AbstractPlayerCommand("reloadconfig", "Reload config") {
        override fun execute(context: CommandContext, store: Store<EntityStore>, ref: Ref<EntityStore>, playerRef: PlayerRef, world: World) {
            Plugin.instance.configManager.load()
            context.sendMessage(Message.raw("Reloaded config"))
        }
    }
    class ReloadScriptsCommand : AbstractPlayerCommand("reloadscripts", "Reload scripts") {
        override fun execute(context: CommandContext, store: Store<EntityStore>, ref: Ref<EntityStore>, playerRef: PlayerRef, world: World) {
            Plugin.instance.tsLoader.reload(context)
            context.sendMessage(Message.raw("Reloaded scripts"))

            val html = """
<div class="page-overlay">
    <div class="container" data-hyui-title="Settings">
        <p>Welcome to the menu!</p>
        <button id="myBtn">Click Me</button>
    </div>
</div>
"""

            PageBuilder.detachedPage()
                .fromHtml(html)
                .addEventListener("myBtn", CustomUIEventBindingType.Activating) { ctx ->
                    playerRef.sendMessage(Message.raw("Clicked! $ctx"));
                }
                .open(playerRef, store);
        }
    }
}