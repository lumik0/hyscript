## 📜 Документация HyScript
HyScript - это мощный движок для написания скриптов на JavaScript/TypeScript для серверов Hytale. Он позволяет 
создавать игровую логику, мини-игры и инструменты автоматизации без необходимости долгой компиляции модов

[<img height="20" src="images/english.png" width="35" />](DOCUMENTATION_EN.MD)

### 📂 Структура проекта
Все файлы скриптов и конфигурации хранятся в папке плагина:

scripts/ - основная папка для скриптов (.js или .ts)

sdk/ - вспомогательная папка, содержащая типы (main.d.ts). Используется для автодополнения в редакторах кода

config.json - файл настроек плагина

### ⚙️ Конфигурация (config.json)
В корне папки плагина находится файл настроек:
```json
{
  "isHotReloadEnabled": true, 
  "typescript": true
}
```
- `isHotReloadEnabled`: Если `true`, скрипты обновляются мгновенно при сохранении (без перезагрузки сервера)
- `typescript`: Включает поддержку TypeScript

### 🛠 Основные концепции

1.Создание объектов и компонентов

Для создания нативных объектов Hytale используй методы create и createComponent:
```typescript
// Создание вектора
const position = server.create('vector3d', 10, 64, 10);

// Создание компонента трансформации
const transform = server.createComponent('transform', position, rotation);
```

2.Работа с игроками

Метод server.getPlayer(...) позволяет получить обертку над игроком, которая упрощает жизнь:
```typescript
server.on("playerChat", async e => {
    const p = await server.getPlayer({ playerRef: e.sender })
    if(p) {
        p.sendMessage(`Привет ${p.getUsername()} из HyScript!`);
        p.sendNotification("Добро пожаловать!");
        p.health = 50;
        p.teleport(0, 100, 0);
    }
});
```

### 📢 События (Events)

В будущем будет еще больше событий

Пример использования:
```typescript
server.on('playerConnect', (e) => {
    const p = server.getPlayer({ playerRef: e.playerRef });
    p?.sendNotification(`Привет, ${p.username}!`);
});

server.on("playerBreakBlock", e => {
    e.player.sendMessage("Не ломай!");
    e.isCancelled = true;
});
```

```typescript
const event =
    server.on('playerChat', (e) => {
        if(e.content == "ping") {
            e.sender.sendMessage("pong!");
            e.isCancelled = true; // отменить отправку сообщения в общий чат
        }
    })
    .key("ping") // ключ события
    .priority(EventPriority.LOW); // приоритет события

server.removeByKey("ping"); // удалить все события с ключом "ping"
// или
event.remove();
```

### ⌨️ Создание команд

HyScript позволяет легко регистрировать команды

```typescript
server.addCommand({
    name: "heal",
    description: "Восстановить здоровье",
    args: [
        { name: "target", description: "Кого лечим", type: "playerRef", required: true }
    ],
    async execute(context, args) {
        const target = await server.getPlayer({ playerRef: args.target });
        target?.heal();
    }
});
```

Команда только для игрока
```typescript
server.addPlayerCommand({
    name: "spawn",
    async execute(context, args, store, ref, playerRef, world) {
        const p = await server.getPlayer({ playerRef, store, ref, world });
        p?.teleport(0, 64, 0);
    }
});
```

Команда с подкомандами
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

### 🧬 Система Компонентов Сущностей (ECS)

Event System
```typescript
server.addEventSystem('entityEvent', BreakBlockEvent, {
    async handle(index, archetypeChunk, store, commandBuffer, event) {
        const player = await server.getPlayer({ index, archetypeChunk, store });
        if(player) {
            player.sendMessage('Эй, не ломай!');
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

Component System
```typescript
server.addComponentSystem('refChange', Teleport, {
    query() {
        // Только игроки
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
player.getComponent(TransformComponent.getComponentType()).setPosition(...) // Получение компонента

const comp = server.createComponent('teleport', 0, 100, 0)
player.addComponent(Teleport.getComponentType(), comp);
```

Кастомный компонент
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

### 📥 Слушатели пакетов

```typescript
server.addAdapterInbound((packetHandler, packet) => {
    if(packet.getId() == 108) {
        NotificationUtil.sendNotification(packetHandler, 'ты только что начал двигаться!');
    }
});

server.addAdapterOutbound((packetHandler, packet) => {
    if(packet.getId() == 108) {
        NotificationUtil.sendNotification(packetHandler, 'ты двигался!');
    }
});
```

### 🖼️ Интерфейс (UI)

Скоро...