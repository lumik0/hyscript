## 📜 Documentation HyScript
HyScript is a powerful engine for writing JavaScript/TypeScript scripts for Hytale servers. It allows you to create game logic, mini-games, and automation tools without the need for lengthy mod compilation.

[<img height="20" src="images/russian.png" width="35" />](DOCUMENTATION_RU.MD)

### 📂 Project structure
All script and configuration files are stored in the plugin folder:

scripts/ - the main folder for your scripts (.js or .ts)

sdk/ - auxiliary folder containing types (main.d.ts). Used for autocompletion in code editors

config.json - plugin settings file

### ⚙️ Configuration (config.json)
The settings file is located at the root of the plugin folder:
```json
{
  "isHotReloadEnabled": true, 
  "typescript": true
}
```
- `isHotReloadEnabled`: If `true`, scripts are updated instantly upon saving (without restarting the server)
- `typescript`: Enables TypeScript support

### 🛠 Basic concepts

1.Creating objects and components

To create native Hytale objects, use the create and createComponent methods:
```typescript
// Creating a vector
const position = server.create('vector3d', 10, 64, 10);

// Creating a transform component
const transform = server.createComponent('transform', position, rotation);
```

2.Working with players

The server.getPlayer(...) method allows you to get a wrapper over the player, which makes life easier
```typescript
server.on("playerChat", async e => {
    const p = await server.getPlayer({ playerRef: e.sender })
    if(p) {
        p.sendMessage(`Hello ${p.getUsername()} from HyScript!`);
        p.sendNotification("Welcome!");
        p.health = 50;
        p.teleport(0, 100, 0);
    }
});
```

### 📢 Events

There will be even more events in the future.

Example of use:
```typescript
server.on('playerConnect', (e) => {
    const p = server.getPlayer({ playerRef: e.playerRef });
    p?.sendNotification(`Hello, ${p.username}!`);
});

server.on("playerBreakBlock", e => {
    e.player.sendMessage("Don't break!");
    e.isCancelled = true;
});
```

```typescript
const event =
    server.on('playerChat', (e) => {
        if(e.content == "ping") {
            e.sender.sendMessage("pong!");
            e.isCancelled = true; // cancel sending a message to the chat
        }
    })
    .key("ping") // event key
    .priority(EventPriority.LOW); // event priority

server.removeByKey("ping"); // delete all events with the key "ping"
// or
event.remove();
```

### ⌨️ Creating commands

HyScript makes it easy to register commands

```typescript
server.addCommand({
    name: "heal",
    description: "Restore health",
    args: [
        { name: "target", description: "Who we treat", type: "playerRef", required: true }
    ],
    async execute(context, args) {
        const target = await server.getPlayer({ playerRef: args.target });
        target?.heal();
    }
});
```

Command for player only
```typescript
server.addPlayerCommand({
    name: "spawn",
    async execute(context, args, store, ref, playerRef, world) {
        const p = await server.getPlayer({ playerRef, store, ref, world });
        p?.teleport(0, 64, 0);
    }
});
```

A command with subcommands
```typescript
server.addCommandCollection({
    name: "script",
    type: "collection",
    subCommands: {
        "reload": {
            execute(ctx, args) {
                ctx.sendMessage(Message.raw("Reloading..."));
            }
        },
        "info": {
            type: "player",
            execute(ctx, args, store, ref, player, world) {
                player.sendMessage(Message.raw(`World: ${world.getName()}, TPS: ${world.getTps()}`));
            }
        }
    }
});
```

### 🧬 Entity Component System (ECS)

Event System
```typescript
server.addEventSystem('entityEvent', BreakBlockEvent, {
    async handle(index, archetypeChunk, store, commandBuffer, event) {
        const player = await server.getPlayer({ index, archetypeChunk, store });
        if(player) {
            player.sendMessage("Hey, don't break!");
            event.setCancelled(true);
        }
    },
    query() {
        return PlayerRef.getComponentType();
    }
});
```

System
```typescript
server.addSystem('ticking', {
    query() {
        return Query.and(PlayerRef.getComponentType(), ...);
    },
    tick(dt, index, store) {
        // ...
    }
});
```

ComponentSystem
```typescript
server.addComponentSystem('refChange', Teleport, {
    query() {
        // Only players
        return PlayerRef.getComponentType();
    },
    componentType() {
        return Teleport.getComponentType();
    },
    onComponentRemoved(ref, component, store, commandBuffer) {
    },
    onComponentSet(ref, valueComponent, component, store, commandBuffer) {
    },
    onComponentAdded(ref, component, store, commandBuffer) {
        console.log(component.getPosition());
    }
});
```

```typescript
const player = await server.getPlayer({ playerRef });
player.getComponent(TransformComponent.getComponentType()).setPosition(...)

const comp = server.createComponent('teleport', 0, 100, 0)
player.addComponent(Teleport.getComponentType(), comp);
```

Custom Component
```typescript
const customComp = server.createCustomComponent((str: string, lvl: number, xp: number) =>
    return {
        text: test,
        level: lvl,
        xp,
        test: lvl + xp
    }
});

const comp = customComp.create("str", 1, 1.3);
comp.text // "str"
comp.test // 2.3
player.addComponent(customComp.type, comp);
```

### 📥 Packet listeners

```typescript
server.addAdapterInbound((packetHandler, packet) => {
    if(packet.getId() == 108) {
        NotificationUtil.sendNotification(packetHandler, "you've only just started moving!");
    }
});

server.addAdapterOutbound((packetHandler, packet) => {
    if(packet.getId() == 108) {
        NotificationUtil.sendNotification(packetHandler, 'you were moving!');
    }
});
```

### 🖼️ UI

Coming soon...