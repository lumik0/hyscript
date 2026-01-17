import _Vector3d from './math/vector/Vector3d'
import _Axis from './math/Axis'

type InstanceOf<T> = T extends new (...args: any[]) => infer R ? R : any;

declare module Events {
  enum EventPriority {
    LOWEST = 0,
    LOW = 1,
    NORMAL = 2,
    HIGH = 3,
    HIGHEST = 4,
    MONITOR = 5
  }

  class EventHandle<T extends (...args: any[]) => void> {
    private keyName?: string;

    readonly event: string
    readonly callback: T

    key(name: string): this
    getKey(): string | undefined

    priority(priority: EventPriority): this
    getPriority(): EventPriority

    remove(): void
  }

  type EventCMap = Record<string, (...args: any[]) => any>;
}

declare class Events<Cust extends Events.EventCMap = {}> {
  on<K extends keyof Cust>(evt: K, callback: Cust[K], priority?: Events.EventPriority): Events.EventHandle<Cust[K]>
  once<K extends keyof Cust>(evt: K, callback: Cust[K], priority?: Events.EventPriority): Events.EventHandle<Cust[K]>
  off<K extends keyof Cust>(evt: K, callback: Cust[K]): boolean

  call<K extends keyof Cust>(evt: K, event?: Parameters<Cust[K]>[0]): Promise<Parameters<Cust[K]>[0]>
  callSync<K extends keyof Cust>(evt: K, event?: Parameters<Cust[K]>[0]): Parameters<Cust[K]>[0]
  emit<K extends keyof Cust>(evt: K, ...args: Parameters<Cust[K]> | []): boolean
  emitR<K extends keyof Cust>(evt: K, ...args: Parameters<Cust[K]> | []): Promise<ReturnType<Cust[K]>[]>

  wait<K extends keyof Cust>(type: K, timeout?: number): Promise<Parameters<Cust[K]> | []>

  removeByKey(key: string): boolean
  removeAllEvents(): void
}

declare module Math {
  class Transform {
    getAxis(): _Axis
    getPosition(): _Vector3d
    setPosition(position: _Vector3d): void
    getRotation(): _Vector3d // TODO: RENAME TO Vector3f!!!!
    setRotation(rotation: _Vector3d): void // TODO: RENAME TO Vector3f!!!!

    constructor(position: _Vector3d)
    constructor(x: number, y: number, z: number)
    constructor(x: number, y: number, z: number, pitch: number, yaw: number, roll: number)

    assign(transform: this): void

    clone(): this
  }
}

declare module Component {
  class Transform implements ECS.Component<ECS.EntityStore> {
    clone(): ECS.Component<ECS.EntityStore>;
    cloneSerializable(): ECS.Component<ECS.EntityStore>;

    static getComponentType(): ECS.ComponentType<ECS.EntityStore, Transform>
    getPosition(): _Vector3d
    setPosition(position: _Vector3d): void
    teleportPosition(position: _Vector3d): void
    getRotation(): _Vector3d // TODO: RENAME TO Vector3f!!!!
    setRotation(rotation: _Vector3d): void // TODO: RENAME TO Vector3f!!!!
    getTransform(): Math.Transform
    teleportRotation(rotation: _Vector3d): void // TODO: RENAME TO Vector3f!!!!
    // getChunkRef(): ECS.Ref<ECS.ChunkStore> | null
    // setChunkLocation(chunkRef: ECS.Ref<ECS.ChunkStore> | null, chunk: Hytale.WorldChunk | null): void
  }
}

declare module ECS {
  interface Component<ECS_TYPE> {
    clone(): Component<ECS_TYPE>
    cloneSerializable(): Component<ECS_TYPE>
  }
  class ComponentType<ECS_TYPE, T extends Component<ECS_TYPE>> {
    getTypeClass(): any;
    getIndex(): number;
    isValid(): boolean;
    test(archetype: any): boolean;
    compareTo(other: ComponentType<ECS_TYPE, any>): number;
  }

  interface EcsEvent {

  }

  enum AddReason { SPAWN, LOAD }
  enum RemoveReason { REMOVE, UNLOAD }

  interface Ref<ECS_TYPE> {
    getStore(): Store<ECS_TYPE>
    getIndex(): number
    validate(): void
    isValid(): boolean
  }

  interface Holder<ECS_TYPE> {
    ensureComponentsSize(size: number): Component<ECS_TYPE>[]
    ensureComponent<T extends Component<ECS_TYPE>>(componentType: ComponentType<ECS_TYPE, T>): void
    ensureAndGetComponent<T extends Component<ECS_TYPE>>(componentType: ComponentType<ECS_TYPE, T>): T
    addComponent<T extends Component<ECS_TYPE>>(componentType: ComponentType<ECS_TYPE, T>, component: T): void
    replaceComponent<T extends Component<ECS_TYPE>>(componentType: ComponentType<ECS_TYPE, T>, component: T): void
    putComponent<T extends Component<ECS_TYPE>>(componentType: ComponentType<ECS_TYPE, T>, component: T): void
    getComponent<T extends Component<ECS_TYPE>>(componentType: ComponentType<ECS_TYPE, T>): T
    removeComponent<T extends Component<ECS_TYPE>>(componentType: ComponentType<ECS_TYPE, T>): void
    tryRemoveComponent<T extends Component<ECS_TYPE>>(componentType: ComponentType<ECS_TYPE, T>): void
    clone(): this
  }

  interface Store<ECS_TYPE> {
    getStoreIndex(): number
    getExternalData(): ECS_TYPE
    isShutdown(): boolean
    shutdown(): void
    saveAllResources(): Promise<void>
    getEntityCount(): number
    getEntityCountFor(systemIndex: number): number
    getArchetypeChunkCount(): number
    getArchetypeChunkCountFor(systemIndex: number): number
    addEntity(holder: Holder<ECS_TYPE>, reason: AddReason): Ref<ECS_TYPE> | null
    addEntity(holder: Holder<ECS_TYPE>, ref: Ref<ECS_TYPE>, reason: AddReason): Ref<ECS_TYPE> | null
    addEntities(holders: Holder<ECS_TYPE>[], reason: AddReason): Ref<ECS_TYPE>[]
    addEntities(holders: Holder<ECS_TYPE>[], start: number, length: number, reason: AddReason): Ref<ECS_TYPE>[]
    addEntities(holders: Holder<ECS_TYPE>[], refs: Ref<ECS_TYPE>[], reason: AddReason): Ref<ECS_TYPE>[]
    addEntities(holders: Holder<ECS_TYPE>[], holderStart: number, refs: Ref<ECS_TYPE>[], refStart: number, length: number, reason: AddReason): Ref<ECS_TYPE>[]
    copyEntity(ref: Ref<ECS_TYPE>): Holder<ECS_TYPE>
    copyEntity(ref: Ref<ECS_TYPE>, holder: Holder<ECS_TYPE>): Holder<ECS_TYPE>
    copySerializableEntity(ref: Ref<ECS_TYPE>): Holder<ECS_TYPE>
    copySerializableEntity(ref: Ref<ECS_TYPE>, holder: Holder<ECS_TYPE>): Holder<ECS_TYPE>
    removeEntity(ref: Ref<ECS_TYPE>, reason: RemoveReason): Holder<ECS_TYPE>
    removeEntity(ref: Ref<ECS_TYPE>, holder: Holder<ECS_TYPE>, reason: RemoveReason): Holder<ECS_TYPE>
    removeEntities(refs: Ref<ECS_TYPE>, reason: RemoveReason): Holder<ECS_TYPE>[]
    removeEntities(refs: Ref<ECS_TYPE>[], start: number, length: number, reason: RemoveReason): Ref<ECS_TYPE>[]
    removeEntities(refs: Ref<ECS_TYPE>[], holders: Holder<ECS_TYPE>[], reason: RemoveReason): Ref<ECS_TYPE>[]
    removeEntities(refs: Ref<ECS_TYPE>[], refStart: number, holders: Holder<ECS_TYPE>[], holderStart: number, length: number, reason: RemoveReason): Ref<ECS_TYPE>[]
    ensureComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>): void
    ensureAndGetComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>): T
    addComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>, component: T): void
    replaceComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>, component: T): void
    putComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>, component: T): void
    getComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>): T
    removeComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>): void
    tryRemoveComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>): void
    removeComponentIfExists<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>): boolean
    invoke<Event extends EcsEvent>(ref: Ref<ECS_TYPE>, param: Event): void
    invoke<Event extends EcsEvent>(param: Event): void
    tick(dt: number): void
    pausedTick(dt: number): void
    isInThread(): boolean
    isAliveInDifferentThread(): boolean
  }

  interface EntityStore {
    shutdown(): void
    getStore(): Store<EntityStore>
    getRefFromUUID(uuid: string): Ref<EntityStore> | null
    getRefFromNetworkId(networkId: number): Ref<EntityStore> | null
    takeNextNetworkId(): number
    getWorld(): Hytale.World
  }

  class Query<ECS_TYPE> {
    static any<ECS_TYPE>(): AnyQuery<ECS_TYPE>
    static not<ECS_TYPE>(query: Query<ECS_TYPE>): NotQuery<ECS_TYPE>
    static and<ECS_TYPE>(...queries: Query<ECS_TYPE>[]): AndQuery<ECS_TYPE>
    static or<ECS_TYPE>(...queries: Query<ECS_TYPE>[]): OrQuery<ECS_TYPE>

    // test(archetype: Archetype<ECS_TYPE>): boolean
    requiresComponentType(componentType: ComponentType<ECS_TYPE, any>): boolean
    validate(): void
  }

  class AnyQuery<ECS_TYPE> implements Query<ECS_TYPE> {
    // test(archetype: Archetype<ECS_TYPE>): boolean
    requiresComponentType(componentType: ComponentType<ECS_TYPE, any>): boolean;
    validate(): void;
  }

  class NotQuery<ECS_TYPE> implements Query<ECS_TYPE> {
    // test(archetype: Archetype<ECS_TYPE>): boolean
    requiresComponentType(componentType: ComponentType<ECS_TYPE, any>): boolean;
    validate(): void;
  }

  class AndQuery<ECS_TYPE> implements Query<ECS_TYPE> {
    // test(archetype: Archetype<ECS_TYPE>): boolean
    requiresComponentType(componentType: ComponentType<ECS_TYPE, any>): boolean;
    validate(): void;
  }

  class OrQuery<ECS_TYPE> implements Query<ECS_TYPE> {
    // test(archetype: Archetype<ECS_TYPE>): boolean
    requiresComponentType(componentType: ComponentType<ECS_TYPE, any>): boolean;
    validate(): void;
  }
}

declare module ECSEvent {
  abstract class EcsEvent { }

  abstract class CancellableEcsEvent extends EcsEvent {
    isCancelled(): boolean
    setCancelled(cancelled: boolean): void
  }

  class BreakBlockEvent extends CancellableEcsEvent {
    // getItemInHand(): ItemStack
    // getTargetBlock(): Vector3i
    // getBlockType(): BlockType
    // setTargetBlock(targetBlock: Vector3i): void
  }
}

declare module Command {
  interface CommandSender {
    getDisplayName(): string
    getUuid(): string
  }

  class CommandContext {
    get<DataType>(argument: Argument<any, DataType>): DataType
    getInput(argument: Argument<any, any>): string[]
    provided(argument: Argument<any, any>): boolean
    getInputString(): string
    sendMessage(message: Hytale.Message): void
    isPlayer(): boolean
    senderAsPlayerRef(): ECS.Ref<ECS.EntityStore> | null
    sender(): CommandSender
    getCalledCommand(): AbstractCommand
  }

  abstract class ArgumentType<DataType> {
    getName(): Hytale.Message
    getArgumentUsage(): Hytale.Message
    getNumberOfParameters(): number
    getExamples(): string[]
  }

  abstract class Argument<Arg extends Argument<Arg, DataType>, DataType> {
    getName(): string
    getDescription(): string
    getArgumentType(): ArgumentType<DataType>
    getCommandRegisteredTo(): AbstractCommand

    provided(context: CommandContext): boolean
    get(context: CommandContext): DataType
    getProcessed(context: CommandContext): DataType

    // TODO: add suggestions
  }

  class RequiredArg<DataType> extends Argument<RequiredArg<DataType>, DataType> {
    getUsageMessageWithoutDescription(): Hytale.Message
    getUsageMessage(): Hytale.Message
    getUsageOneLiner(): Hytale.Message
  }

  abstract class AbstractCommand {
    constructor(description: string)
    constructor(name: string, description: string)
    constructor(name: string, description: string, requiresConfirmation: boolean)
    permissionGroups: string[]
    permissionGroupsRecursive: Map<string, Set<string>>
    unavailableInSingleplayer: boolean
    allowsExtraArguments: boolean
    requirePermission(permission: string): void
    addAliases(...aliases: string[]): void
    addSubCommand(command: AbstractCommand): void
    addUsageVariant(command: AbstractCommand): void
    hasPermission(sender: CommandSender): void
    getUsageString(sender: CommandSender): Hytale.Message
    getUsageShort(sender: CommandSender, fullyQualify: boolean): Hytale.Message

    withRequiredArg<D>(name: string, description: string, argType: ArgumentType<D>): RequiredArg<D>
    // TODO: add all with args
  }

  interface BaseCommandConfig {
    name: string;
    description?: string;
    requiresConfirmation?: boolean;
    aliases?: string[];
    permission?: string;
    subCommands?: Record<string, CommandConfig>;
  }

  interface AsyncCommandConfig extends BaseCommandConfig {
    type?: "default";
    execute: (context: Command.CommandContext) => void;
  }

  interface PlayerCommandConfig extends BaseCommandConfig {
    type: "player";
    execute: (
      context: Command.CommandContext,
      store: ECS.Store<ECS.EntityStore>,
      ref: ECS.Ref<ECS.EntityStore>,
      playerRef: Hytale.PlayerRef,
      world: Hytale.World
    ) => void;
  }

  interface CollectionCommandConfig extends BaseCommandConfig {
    type: "collection";
  }

  type CommandConfig = AsyncCommandConfig | PlayerCommandConfig | CollectionCommandConfig;
}

declare module System {
  interface Type {

  }
  interface EventType<E extends ECSEvent.EcsEvent> {
    entityEvent: {
      // any >>> ArchetypeChunk<EntityStore>, CommandBuffer<ECS.EntityStore>
      handle(index: number, archetypeChunk: any, store: ECS.Store<ECS.EntityStore>, commandBuffer: any, event: E): void
      query(): ECS.Query<ECS.EntityStore>
    }
  }
}

declare module Hytale {
  interface Universe {
    runBackup(): Promise<void>
    disconnectAllPLayers(): void
    shutdownAllWorlds(): void
    getUniverseReady(): Promise<void>
    isWorldLoadable(name: string): boolean
    addWorld(name: string): Promise<World>
    makeWorld(name: string, savePath: string, worldConfig: WorldConfig): void
    makeWorld(name: string, savePath: string, worldConfig: WorldConfig, start: boolean): void
    loadWorldFromStart(savePath: string, name: String): Promise<void>
    loadWorld(name: string): Promise<World>
    getWorld(worldName: string): World | null
    getWorld(uuid: string): World | null
    getDefaultWorld(): World | null
    removeWorld(name: string): boolean
    removeWorldExceptionally(name: string): void
    getPath(): string
    getWorlds(): Map<string, World>
    getPlayers(): PlayerRef[]
    getPlayer(uuid: string): PlayerRef | null
    getPlayerByUsername(value: string, matching: 'EXACT' | 'EXACT_IGNORE_CASE' | 'STARTS_WITH' | 'STARTS_WITH_IGNORE_CASE'): PlayerRef | null
    getPlayerCount(): number
    removePlayer(playerRef: PlayerRef): void
    resetPlayer(oldPlayer: PlayerRef): Promise<PlayerRef>
    resetPlayer(oldPlayer: ECS.Holder<ECS.EntityStore>): Promise<PlayerRef>
    resetPlayer(oldPlayer: ECS.Holder<ECS.EntityStore>, world: World, transform: Math.Transform): Promise<PlayerRef>
    sendMessage(message: Message): void
    broadcastPacket(packet: Packet): void
    broadcastPacketNoCache(packet: Packet): void
    broadcastPacket(...packets: Packet[]): void
    getPlayerStorage(): PlayerStorage;
    setPlayerStorage(playerStorage: PlayerStorage): void;
    getPlayerRefComponentType(): ECS.ComponentType<ECS.EntityStore, PlayerRef>
    getWorldGenPath(): string
  }

  class World {
    init(): Promise<this>
    stopIndividualWorld(): void
    validateDeleteOnRemove(): void
    getTps(): number
    getName(): string
    isAlive(): boolean
    getWorldConfig(): WorldConfig
    getDeathConfig(): DeathConfig
    getDaytimeDurationSeconds(): number
    getNighttimeDurationSeconds(): number
    isTicking(): boolean
    setTicking(ticking: boolean): void
    isPaused(): boolean
    setPaused(paused: boolean): void
    getTick(): number
    isCompassUpdating(): boolean
    setCompassUpdating(compassUpdating: boolean): void
    // @ts-ignore TODO: WorldChunk
    loadChunkIfInMemory(index: bigint): WorldChunk|null
    // @ts-ignore TODO: WorldChunk
    getChunkIfInMemory(index: bigint): WorldChunk|null
    // @ts-ignore TODO: WorldChunk
    getChunkIfLoaded(index: bigint): WorldChunk|null
    // @ts-ignore TODO: WorldChunk
    getChunkIfNonTicking(index: bigint): WorldChunk|null
    // @ts-ignore TODO: WorldChunk
    getChunkAsync(index: bigint): Promise<WorldChunk>
    // @ts-ignore TODO: WorldChunk
    getNonTickingChunkAsync(index: bigint): Promise<WorldChunk>
    getEntityRef(uuid: string): ECS.Ref<ECS.EntityStore>
    getPlayerCount(): number
    getPlayerRefs(): PlayerRef[]
    trackPlayerRef(playerRef: PlayerRef): void
    untrackPlayerRef(playerRef: PlayerRef): void
    sendMessage(message: Message): void
    execute(command: Function): void
    consumeTaskQueue(): void
    // getChunkStore(): ECS.ChunkStore
    getEntityStore(): ECS.EntityStore
    // getChunkLighting(): ChunkLightingManager
    // getWorldMapManager(): WorldMapManager
    // getWorldPathConfig(): WorldPathConfig
    // getNotificationHandler(): WorldNotificationHandler
    // getEventRegistry(): EventRegistry
    addPlayer(playerRef: PlayerRef): Promise<PlayerRef>
    addPlayer(playerRef: PlayerRef, transform: Math.Transform): Promise<PlayerRef>
    addPlayer(playerRef: PlayerRef, transform: Math.Transform, clearWorldOverride: boolean, fadeInOutOverride: boolean): Promise<PlayerRef>
    drainPlayersTo(fallbackTargetWorld: World): Promise<void>
    // getGameplayConfig(): GameplayConfig
    // getFeatures(): Map<ClientFeature, boolean>
    // isFeatureEnabled(feature: ClientFeature): boolean
    // registerFeature(feature: ClientFeature, enabled: boolean): void
    broadcastFeatures(): void
    getSavePath(): string
    updateEntitySeed(store: ECS.Store<ECS.EntityStore>): void
    markGCHasRun(): void
    consumeGCHasRun(): boolean
  }

  interface PlayerStorage {
    load(uuid: string): Promise<ECS.Holder<ECS.EntityStore>>
    save(uuid: string, holder: ECS.Holder<ECS.EntityStore>): Promise<void>
    remove(uuid: string): Promise<void>
    getPlayers(): string[]
  }

  interface Packet {
    getId(): number
    getComputeSize(): number
  }

  class Message {
    constructor(message: string, i18n: boolean)

    param(key: string, value: string): this
    param(key: string, value: boolean): this
    param(key: string, value: number): this
    param(key: string, value: bigint): this
    param(key: string, value: Message): this

    bold(bold: boolean): this
    italic(italic: boolean): this
    monospace(monospace: boolean): this
    color(color: string): this
    link(url: string): this

    insert(message: Message): this
    insert(message: string): this
    insertAll(...messages: string[]): this

    static empty(): Message
    static translation(messageId: string): Message
    static raw(message: string): Message
    static parse(message: string): Message
    static join(...messages: string[]): Message
  }

  interface Formatter {
    format(playerRef: PlayerRef, message: string): Message
  }

  class EventTitleUtil {
    static showEventTitleToUniverse(primaryTitle: Message, secondaryTitle: Message, isMajor: boolean, icon: string, duration: number, fadeInDuration: number, fadeOutDuration: number): void
    static showEventTitleToUniverse(primaryTitle: Message, secondaryTitle: Message, isMajor: boolean, icon: string, duration: number, fadeInDuration: number, fadeOutDuration: number, store: ECS.Store<ECS.EntityStore>): void

    static hideEventTitleFromWorld(fadeOutDuration: number, store: ECS.Store<ECS.EntityStore>): void
    static showEventTitleToPlayer(playerRefComponent: PlayerRef, primaryTitle: Message, secondaryTitle: Message, isMajor: boolean, icon: string, duration: number, fadeInDuration: number, fadeOutDuration: number): void
    static showEventTitleToPlayer(playerRefComponent: PlayerRef, primaryTitle: Message, secondaryTitle: Message, isMajor: boolean): void

    static hideEventTitleFromPlayer(playerRefComponent: PlayerRef, fadeOutDuration: number): void
  }

  interface WorldConfig {
    getUuid(): string
    isDeleteOnUniverseStart(): boolean
    isDeleteOnRemove(): boolean
    isSavingConfig(): boolean
    getDisplayName(): string
    formatDisplayName(name: string): string
    getSeed(): bigint

    isTicking(): boolean
    isBlockTicking(): boolean
    isPvpEnabled(): boolean

    // TODO!!!!!!
  }

  interface DeathConfig {

  }

  class PlayerRef implements ECS.Component<ECS.EntityStore> {
    clone(): ECS.Component<ECS.EntityStore>;
    cloneSerializable(): ECS.Component<ECS.EntityStore>;
    getComponentType(): ECS.ComponentType<ECS.EntityStore, PlayerRef>
    addToStore(store: ECS.Store<ECS.EntityStore>): ECS.Ref<ECS.EntityStore> | null
    addToStore(store: ECS.Ref<ECS.EntityStore>): void
    removeFromStore(): ECS.Holder<ECS.EntityStore>
    isValid(): boolean
    getReference(): ECS.Ref<ECS.EntityStore> | null
    getHolder(): ECS.Holder<ECS.EntityStore> | null
    getUuid(): string
    getUsername(): string
    getLanguage(): string
    setLanguage(language: string): void
    getTransform(): Math.Transform
    getWorldUuid(): string | null
    getHeadRotation(): _Vector3d // TODO: RENAME TO Vector3f
    updatePosition(world: World, transform: Math.Transform, headRotation: _Vector3d): void // TODO: RENAME TO Vector3f
    referToServer(host: string, port: number): void
    referToServer(host: string, port: number, data: number[]): void
    sendMessage(message: Message): void;
  }
}

declare module API {
  interface ServerEvents {
    // setupConnect: (e: {
    //   username: string
    //   uuid: string
    // }) => void
    setup: () => void
    playerConnect: (e: {
      playerRef: Hytale.PlayerRef,
      world: Hytale.World
    }) => void
    playerDisconnect: (e: {
      playerRef: Hytale.PlayerRef,
      reason: string
    }) => void
    playerReady: (e: {
      playerRef: Hytale.PlayerRef
      readyId: number
    }) => void
    playerMouseButton: (e: {
      playerRef: Hytale.PlayerRef
      clientUseTime: bigint
      // itemInHand: Item
      // targetBlock: Vector3i
      // targetEntity: Entity
      // screenPoint: Vector2f
      // mouseButton: MouseButtonEvent
      isCancelled: boolean
    }) => void
    playerChat: (e: {
      sender: Hytale.PlayerRef
      content: string
      formatter: Hytale.Formatter
      targets: Hytale.PlayerRef[]
      isCancelled: boolean
    }) => void
  }

  interface ObjectFactoryMap {
    "uuid": [string, [string]]

    "transform": [Math.Transform, [number, number, number] | [_Vector3d]]
    "message": [Hytale.Message, [string] | []]
    "vector3d": [_Vector3d, [number, number, number]]
  }
  interface ComponentFactoryMap {
    "transform": [Component.Transform, [_Vector3d, _Vector3d] | []]
  }

  // @ts-ignore
  class Server extends Events<ServerEvents> {
    /**
      * Создает нативный Java-объект
      * @param type Тип объекта (например, 'transform')
      * @param args Аргументы конструктора
      */
    create<K extends keyof ObjectFactoryMap>(type: K, ...args: ObjectFactoryMap[K][1]): ObjectFactoryMap[K][0];
    /**
      * Создает нативный компонент
      * @param type Тип компонента (например, 'transform')
      * @param args Аргументы конструктора
      */
    createComponent<K extends keyof ComponentFactoryMap>(type: K, ...args: ComponentFactoryMap[K][1]): ComponentFactoryMap[K][0];

    addCommand(config: Command.CommandConfig): void;
    addCommandCollection(config: Command.BaseCommandConfig & { subCommands: Record<string, Command.CommandConfig> }): void;
    addPlayerCommand(config: Omit<Command.PlayerCommandConfig, "type">): void;

    addSystem<K extends keyof System.Type>(type: K, config: System.Type[K]): void
    addEventSystem<E extends ECSEvent.EcsEvent>(type: keyof System.EventType<InstanceOf<E>>, eventClass: E, config: System.EventType<InstanceOf<E>>[keyof System.EventType<InstanceOf<E>>]): void
  }

  class PluginManifest {
    getGroup(): string
    getName(): string
    getVersion(): string
    getDescription(): string
    getAuthors(): ({ name: string, email: string, url: string })[]
    getWebsite(): string
    getMain(): string
    getServerVersion(): string
    getDependencies(): Map<string, string>
    getOptionalDependencies(): Map<string, string>
    getLoadBefore(): Map<string, string>
    getSubPlugins(): PluginManifest[]
    isDisabledByDefault(): boolean
  }

  class PluginInit {
    getDataDirectory(): string
    getPluginManifest(): PluginManifest
  }

  class PluginBase {
    getName(): string
    getIdentifier(): string
    getManifest(): PluginManifest
    getDataDirectory(): string
    getState(): 'NONE' | 'SETUP' | 'START' | 'ENABLED' | 'SHUTDOWN' | 'DISABLED'

    // readonly commandRegistry: Registry.CommandRegistry
    // TODO: add registry

    getBasePermission(): string
    isDisabled(): boolean
    isEnabled(): boolean
    getType(): 'PLUGIN'
  }

  class JavaPlugin extends PluginBase {
    getFile(): string
  }
}

// @ts-ignore
export declare global {
  var plugin: API.JavaPlugin
  var server: API.Server

  // Math
  var Vector3d: typeof _Vector3d;
  var Axis: typeof _Axis;
  var Transform: typeof Math.Transform;

  // Component
  var TransformComponent: typeof Component.Transform

  // ECS
  var Query: typeof ECS.Query;

  // Event (ECS)
  var BreakBlockEvent: typeof ECSEvent.BreakBlockEvent

  // Hytale
  var universe: Hytale.Universe;
  var World: typeof Hytale.World;
  var Message: typeof Hytale.Message;
  var EventTitleUtil: typeof Hytale.EventTitleUtil
  var PlayerRef: typeof Hytale.PlayerRef;
}

export {};
