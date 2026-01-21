import './math/vector/Vector3d'
import './math/Axis'

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

declare global {
  //
  // Math
  //
  class Transform {
    getAxis(): Axis
    getPosition(): Vector3d
    setPosition(position: Vector3d): void
    getRotation(): Vector3d // TODO: RENAME TO Vector3f!!!!
    setRotation(rotation: Vector3d): void // TODO: RENAME TO Vector3f!!!!

    constructor(position: Vector3d)
    constructor(x: number, y: number, z: number)
    constructor(x: number, y: number, z: number, pitch: number, yaw: number, roll: number)

    assign(transform: this): void

    clone(): this
  }

  //
  // ECS
  //
  interface Component<ECS_TYPE> {
    clone(): Component<ECS_TYPE>
    cloneSerializable(): Component<ECS_TYPE>
  }
  class ComponentType<ECS_TYPE, T extends Component<ECS_TYPE>> implements Query<ECS_TYPE> {
    requiresComponentType(componentType: ComponentType<ECS_TYPE, T>): boolean;
    validate(): void;
    getTypeClass(): any;
    getIndex(): number;
    isValid(): boolean;
    test(archetype: any): boolean;
    compareTo(other: ComponentType<ECS_TYPE, any>): number;
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
    getWorld(): World
  }

  interface ComponentAccessor<ECS_TYPE> {
    getComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>): T | null
    ensureAndGetComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>): T
    getArchetype(ref: Ref<ECS_TYPE>): Archetype<ECS_TYPE>
    // getResource<T extends Resource<ECS_TYPE>>(resource: ResourceType<ECS_TYPE, T>): T
    getExternalData(): ECS_TYPE
    putComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>, type: T): void
    addComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>, type: T): T
    addComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>): T
    addEntities(holder: Holder<ECS_TYPE>[], reason: AddReason): Ref<ECS_TYPE>[]
    addEntity(holder: Holder<ECS_TYPE>, reason: AddReason): Ref<ECS_TYPE> | null
    removeEntity(ref: Ref<ECS_TYPE>, holder: Holder<ECS_TYPE>, reason: RemoveReason): Holder<ECS_TYPE>
    removeComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>): void
    tryRemoveComponent<T extends Component<ECS_TYPE>>(ref: Ref<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>): void
    invoke<Event extends EcsEvent>(ref: Ref<ECS_TYPE>, event: Event): void
    // invoke<Event extends EcsEvent>(entityEventType: EntityEventType<ECS_TYPE, Event>, ref: Ref<ECS_TYPE>, event: Event): void
    invoke<Event extends EcsEvent>(event: Event): void
    // invoke<Event extends EcsEvent>(worldEventType: WorldEventType<ECS_TYPE, Event>, event: Event): void
  }

  class Query<ECS_TYPE> {
    static any<ECS_TYPE>(): AnyQuery<ECS_TYPE>
    static not<ECS_TYPE>(query: Query<ECS_TYPE>): NotQuery<ECS_TYPE>
    static and<ECS_TYPE>(...queries: Query<ECS_TYPE>[]): AndQuery<ECS_TYPE>
    static or<ECS_TYPE>(...queries: Query<ECS_TYPE>[]): OrQuery<ECS_TYPE>

    test(archetype: Archetype<ECS_TYPE>): boolean
    requiresComponentType(componentType: ComponentType<ECS_TYPE, any>): boolean
    validate(): void
  }

  class AnyQuery<ECS_TYPE> implements Query<ECS_TYPE> {
    test(archetype: Archetype<ECS_TYPE>): boolean
    requiresComponentType(componentType: ComponentType<ECS_TYPE, any>): boolean;
    validate(): void;
  }

  class NotQuery<ECS_TYPE> implements Query<ECS_TYPE> {
    test(archetype: Archetype<ECS_TYPE>): boolean
    requiresComponentType(componentType: ComponentType<ECS_TYPE, any>): boolean;
    validate(): void;
  }

  class AndQuery<ECS_TYPE> implements Query<ECS_TYPE> {
    test(archetype: Archetype<ECS_TYPE>): boolean
    requiresComponentType(componentType: ComponentType<ECS_TYPE, any>): boolean;
    validate(): void;
  }

  class OrQuery<ECS_TYPE> implements Query<ECS_TYPE> {
    test(archetype: Archetype<ECS_TYPE>): boolean
    requiresComponentType(componentType: ComponentType<ECS_TYPE, any>): boolean;
    validate(): void;
  }

  class Archetype<ECS_TYPE> implements Query<ECS_TYPE> {
    static empty<ECS_TYPE>(): Archetype<ECS_TYPE>
    getMinIndex(): number
    count(): number
    length(): number
    get(index: number): ComponentType<ECS_TYPE, any> | null
    isEmpty(): boolean
    contains(componentType: ComponentType<ECS_TYPE, any>): boolean
    contains(archetype: Archetype<ECS_TYPE>): boolean
    validateComponentType(componentType: ComponentType<ECS_TYPE, any>): void
    // validateComponents(components: Component<ECS_TYPE>[], ignore: ComponentType<ECS_TYPE, UnknownComponents<ECS_TYPE>>): void
    asExactQuery(): ExactArchetypeQuery<ECS_TYPE>
    of<ECS_TYPE>(...componentTypes: ComponentType<ECS_TYPE, any>[]): Archetype<ECS_TYPE>
    add<ECS_TYPE, T extends Component<ECS_TYPE>>(archetype: Archetype<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>): Archetype<ECS_TYPE>
    remove<ECS_TYPE, T extends Component<ECS_TYPE>>(archetype: Archetype<ECS_TYPE>, componentType: ComponentType<ECS_TYPE, T>): Archetype<ECS_TYPE>
    test(archetype: Archetype<ECS_TYPE>): boolean
    requiresComponentType(componentType: ComponentType<ECS_TYPE, any>): boolean
    validate(): void
  }

  class ExactArchetypeQuery<ECS_TYPE> implements Query<ECS_TYPE> {
    getArchetype(): Archetype<ECS_TYPE>
    test(archetype: Archetype<ECS_TYPE>): boolean
    requiresComponentType(componentType: ComponentType<ECS_TYPE, any>): boolean
    validate(): void
  }

  class ArchetypeChunk<ECS_TYPE> {
    getArchetype(): Archetype<ECS_TYPE>
    size(): number
    getReferenceTo(index: number): Ref<ECS_TYPE>
    setComponent<T extends Component<ECS_TYPE>>(index: number, componentType: ComponentType<ECS_TYPE, T>, component: T): void
    getComponent<T extends Component<ECS_TYPE>>(index: number, componentType: ComponentType<ECS_TYPE, T>): T
    addEntity(ref: Ref<EntityStore>, holder: Holder<ECS_TYPE>): number
    copyEntity(entityIndex: number, target: Holder<ECS_TYPE>): Holder<ECS_TYPE>
    removeEntity(entityIndex: number, target: Holder<ECS_TYPE>): Holder<ECS_TYPE>
  }

  // ECSEvent
  abstract class EcsEvent { }

  abstract class CancellableEcsEvent extends EcsEvent {
    isCancelled(): boolean
    setCancelled(cancelled: boolean): void
  }

  class BreakBlockEvent extends CancellableEcsEvent {
    getItemInHand(): ItemStack
    // getTargetBlock(): Vector3i
    // getBlockType(): BlockType
    // setTargetBlock(targetBlock: Vector3i): void
  }

  //
  // Component
  //
  class TransformComponent implements Component<EntityStore> {
    clone(): TransformComponent;
    cloneSerializable(): TransformComponent;
    static getComponentType(): ComponentType<EntityStore, TransformComponent>
    getPosition(): Vector3d
    setPosition(position: Vector3d): void
    teleportPosition(position: Vector3d): void
    getRotation(): Vector3d // TODO: RENAME TO Vector3f!!!!
    setRotation(rotation: Vector3d): void // TODO: RENAME TO Vector3f!!!!
    getTransform(): Transform
    teleportRotation(rotation: Vector3d): void // TODO: RENAME TO Vector3f!!!!
    // getChunkRef(): Ref<ChunkStore> | null
    // setChunkLocation(chunkRef: Ref<ChunkStore> | null, chunk: WorldChunk | null): void
  }
  class Velocity implements Component<EntityStore> {
    clone(): Velocity;
    cloneSerializable(): Velocity;
    static getComponentType(): ComponentType<EntityStore, Velocity>
    setZero(): void
    addForce(force: Vector3d): void
    addForce(x: number, y: number, z: number): void
    set(newVelocity: Vector3d): void
    set(x: number, y: number, z: number): void
    setClient(newVelocity: Vector3d): void
    setClient(x: number, y: number, z: number): void
    setX(x: number): void
    setY(y: number): void
    setZ(z: number): void
    getX(): number
    getY(): number
    getZ(): number
    getSpeed(): number
    getVelocity(): Vector3d
    getClientVelocity(): Vector3d
    assignVelocityTo(vector: Vector3d): Vector3d
  }
  class Teleport implements Component<EntityStore> {
    clone(): Teleport;
    cloneSerializable(): Teleport;
    static getComponentType(): ComponentType<EntityStore, Teleport>
    withHeadRotation(headRotation: Vector3d): Teleport // TODO: RENAME TO Vector3f!!!!
    withResetRoll(): Teleport
    withoutVelocityReset(): Teleport
    getWorld(): World | null
    getPosition(): Vector3d
    getRotation(): Vector3d // TODO: RENAME TO Vector3f!!!!
    getHeadRotation(): Vector3d | null // TODO: RENAME TO Vector3f!!!!
    isResetVelocity(): boolean
  }
  type Predictable = 'NONE' | 'SELF' | 'ALL';
  class EntityStatValue {
    getId(): string
    getIndex(): number
    get(): number
    asPercentage(): number
    getMin(): number
    getMax(): number
    getIgnoreInvulnerability(): boolean
  }
  abstract class DefaultEntityStatTypes {
    static HEALTH: number
    static OXYGEN: number
    static STAMINA: number
    static MANA: number
    static SIGNATURE_ENERGY: number
    static AMMO: number
    static getHealth(): number
    static getOxygen(): number
    static getStamina(): number
    static getMana(): number
    static getSignatureEnergy(): number
    static getAmmo(): number
  }
  class EntityStatMap implements Component<EntityStore> {
    clone(): Component<EntityStore>;
    cloneSerializable(): Component<EntityStore>;
    static getComponentType(): ComponentType<EntityStore, EntityStatMap>
    size(): number
    get(index: number): EntityStatValue | null
    update(): void
    // TODO: add Modifier
    setStatValue(index: number, newValue: number): void
    setStatValue(predictable: Predictable, index: number, newValue: number): void
    addStatValue(index: number, amount: number): void
    addStatValue(predictable: Predictable, index: number, amount: number): void
    subtractStatValue(index: number, amount: number): void
    subtractStatValue(predictable: Predictable, index: number, amount: number): void
    minimizeStatValue(index: number): void
    minimizeStatValue(predictable: Predictable, index: number): void
    maximizeStatValue(index: number): void
    maximizeStatValue(predictable: Predictable, index: number): void
    resetStatValue(index: number): void
    resetStatValue(predictable: Predictable, index: number): void
    clearUpdates(): void
    consumeSelfNetworkOutdated(): boolean
    consumeNetworkOutdated(): boolean
  }

  //
  // System
  //

  //
  // Command
  //
  interface CommandSender {
    getDisplayName(): string
    getUuid(): string
  }

  class CommandContext {
    get<DataType>(argument: Argument<any, DataType>): DataType
    getInput(argument: Argument<any, any>): string[]
    provided(argument: Argument<any, any>): boolean
    getInputString(): string
    sendMessage(message: Message): void
    isPlayer(): boolean
    senderAsPlayerRef(): Ref<EntityStore> | null
    sender(): CommandSender
    getCalledCommand(): AbstractCommand
  }

  abstract class ArgumentType<DataType> {
    getName(): Message
    getArgumentUsage(): Message
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
    getUsageMessageWithoutDescription(): Message
    getUsageMessage(): Message
    getUsageOneLiner(): Message
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
    getUsageString(sender: CommandSender): Message
    getUsageShort(sender: CommandSender, fullyQualify: boolean): Message

    withRequiredArg<D>(name: string, description: string, argType: ArgumentType<D>): RequiredArg<D>
    // TODO: add all with args
  }

  //
  // Hytale
  //
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
    resetPlayer(oldPlayer: Holder<EntityStore>): Promise<PlayerRef>
    resetPlayer(oldPlayer: Holder<EntityStore>, world: World, transform: Transform): Promise<PlayerRef>
    sendMessage(message: Message): void
    broadcastPacket(packet: Packet): void
    broadcastPacketNoCache(packet: Packet): void
    broadcastPacket(...packets: Packet[]): void
    getPlayerStorage(): PlayerStorage;
    setPlayerStorage(playerStorage: PlayerStorage): void;
    getPlayerRefComponentType(): ComponentType<EntityStore, PlayerRef>
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
    getEntityRef(uuid: string): Ref<EntityStore>
    getPlayerCount(): number
    getPlayerRefs(): PlayerRef[]
    trackPlayerRef(playerRef: PlayerRef): void
    untrackPlayerRef(playerRef: PlayerRef): void
    sendMessage(message: Message): void
    execute(command: Function): void
    consumeTaskQueue(): void
    // getChunkStore(): ChunkStore
    getEntityStore(): EntityStore
    // getChunkLighting(): ChunkLightingManager
    // getWorldMapManager(): WorldMapManager
    // getWorldPathConfig(): WorldPathConfig
    // getNotificationHandler(): WorldNotificationHandler
    // getEventRegistry(): EventRegistry
    addPlayer(playerRef: PlayerRef): Promise<PlayerRef>
    addPlayer(playerRef: PlayerRef, transform: Transform): Promise<PlayerRef>
    addPlayer(playerRef: PlayerRef, transform: Transform, clearWorldOverride: boolean, fadeInOutOverride: boolean): Promise<PlayerRef>
    drainPlayersTo(fallbackTargetWorld: World): Promise<void>
    // getGameplayConfig(): GameplayConfig
    // getFeatures(): Map<ClientFeature, boolean>
    // isFeatureEnabled(feature: ClientFeature): boolean
    // registerFeature(feature: ClientFeature, enabled: boolean): void
    broadcastFeatures(): void
    getSavePath(): string
    updateEntitySeed(store: Store<EntityStore>): void
    markGCHasRun(): void
    consumeGCHasRun(): boolean
  }

  interface PlayerStorage {
    load(uuid: string): Promise<Holder<EntityStore>>
    save(uuid: string, holder: Holder<EntityStore>): Promise<void>
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

  enum GameMode {
    Adventure,
    Creative
  }

  abstract class PacketHandler {
    abstract getIdentifier(): string
    registered(oldHandler: PacketHandler | null): void
    unregistered(newHandler: PacketHandler | null): void
    handle(packet: Packet): void
    abstract accept(packet: Packet): void
    logCloseMessage(): void
    setQueuePackets(queuePackets: boolean): void
    tryFlush(): void
    write(...packets: Packet[]): void
    writeNoCache(packet: Packet): void
    writePacket(packet: Packet, cache: boolean): void
    disconnect(message: String): void
    getOperationTimeoutThreshold(): bigint
    tickPing(dt: number): void
    sendPing(): void
    stillActive(): boolean
    getQueuedPacketsCount(): number
    isLocalConnection(): boolean
    isLANConnection(): boolean
    setClientReadyForChunksFuture(clientReadyFuture: Promise<void>): void
    getClientReadyForChunksFuture(): Promise<void>
  }

  class EventTitleUtil {
    static showEventTitleToUniverse(primaryTitle: Message, secondaryTitle: Message, isMajor: boolean, icon: string, duration: number, fadeInDuration: number, fadeOutDuration: number): void
    static showEventTitleToUniverse(primaryTitle: Message, secondaryTitle: Message, isMajor: boolean, icon: string, duration: number, fadeInDuration: number, fadeOutDuration: number, store: Store<EntityStore>): void

    static hideEventTitleFromWorld(fadeOutDuration: number, store: Store<EntityStore>): void
    static showEventTitleToPlayer(playerRefComponent: PlayerRef, primaryTitle: Message, secondaryTitle: Message, isMajor: boolean, icon: string, duration: number, fadeInDuration: number, fadeOutDuration: number): void
    static showEventTitleToPlayer(playerRefComponent: PlayerRef, primaryTitle: Message, secondaryTitle: Message, isMajor: boolean): void

    static hideEventTitleFromPlayer(playerRefComponent: PlayerRef, fadeOutDuration: number): void
  }

  type NotificationStyle = 'Default' | 'Danger' | 'Warning' | 'Success';
  class NotificationUtil {
    static sendNotificationToUniverse(message: string): void
    static sendNotificationToUniverse(message: string, secondaryMessage?: string | null): void
    static sendNotificationToUniverse(message: string, style: NotificationStyle): void
    static sendNotificationToUniverse(message: Message): void
    static sendNotificationToUniverse(message: Message, style: NotificationStyle): void
    static sendNotificationToUniverse(message: Message, icon: string | null, style: NotificationStyle): void
    static sendNotificationToUniverse(message: Message, item: ItemStack | null, style: NotificationStyle): void
    static sendNotificationToUniverse(message: Message, secondaryMessage: Message | null, item?: ItemStack | null): void
    static sendNotificationToUniverse(message: Message, secondaryMessage: Message | null, style: NotificationStyle): void
    static sendNotificationToUniverse(message: Message, secondaryMessage: Message | null, icon: string | null, style: NotificationStyle): void
    static sendNotificationToUniverse(message: Message, secondaryMessage: Message | null, item: ItemStack | null, style: NotificationStyle): void
    static sendNotificationToWorld(message: Message, secondaryMessage: Message, icon: string, style: NotificationStyle, store: Store<EntityStore>): void
    static sendNotification(handler: PacketHandler, message: Message, secondaryMessage: Message | null, icon: string | null, item: ItemStack | null, style: NotificationStyle): void
    static sendNotification(handler: PacketHandler, message: string): void
    static sendNotification(handler: PacketHandler, message: string, icon: string): void
    static sendNotification(handler: PacketHandler, message: string, icon: string, style: NotificationStyle): void
    static sendNotification(handler: PacketHandler, message: string, style: NotificationStyle): void
    static sendNotification(handler: PacketHandler, message: Message): void
    static sendNotification(handler: PacketHandler, message: Message, style: NotificationStyle): void
    static sendNotification(handler: PacketHandler, message: Message, secondaryMessage: Message, icon: string): void
    static sendNotification(handler: PacketHandler, message: Message, secondaryMessage: Message): void
    static sendNotification(handler: PacketHandler, message: Message, secondaryMessage: Message, item: ItemStack): void
    static sendNotification(handler: PacketHandler, message: Message, secondaryMessage: Message, style: NotificationStyle): void
    static sendNotification(handler: PacketHandler, message: Message, secondaryMessage: Message, icon: string, style: NotificationStyle): void
    static sendNotification(handler: PacketHandler, message: Message, secondaryMessage: Message, item: ItemStack, style: NotificationStyle): void
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

  class PlayerRef implements Component<EntityStore> {
    clone(): Component<EntityStore>;
    cloneSerializable(): Component<EntityStore>;
    static getComponentType(): ComponentType<EntityStore, PlayerRef>
    addToStore(store: Store<EntityStore>): Ref<EntityStore> | null
    addToStore(store: Ref<EntityStore>): void
    removeFromStore(): Holder<EntityStore>
    isValid(): boolean
    getReference(): Ref<EntityStore> | null
    getHolder(): Holder<EntityStore> | null
    getUuid(): string
    getUsername(): string
    getLanguage(): string
    setLanguage(language: string): void
    getTransform(): Transform
    getWorldUuid(): string | null
    getHeadRotation(): Vector3d // TODO: RENAME TO Vector3f
    updatePosition(world: World, transform: Transform, headRotation: Vector3d): void // TODO: RENAME TO Vector3f
    referToServer(host: string, port: number): void
    referToServer(host: string, port: number, data: number[]): void
    sendMessage(message: Message): void;
  }

  abstract class Entity implements Component<EntityStore> {
    cloneSerializable(): Component<EntityStore>;
    setLegacyUUID(uuid: string): void
    remove(): boolean
    loadIntoWorld(world: World): void
    unloadFromWorld(): void
    getWorld(): World | null
    wasRemoved(): boolean
    isCollidable(): boolean
    setReference(reference: Ref<EntityStore>): void
    getReference(): Ref<EntityStore>
    clone(): Component<EntityStore>
    toHolder(): Holder<EntityStore>
  }

  abstract class LivingEntity extends Entity {
    getInventory(): Inventory
    setInventory(inventory: Inventory): Inventory
    setInventory(inventory: Inventory, ensureCapacity: boolean): Inventory
    setInventory(inventory: Inventory, ensureCapacity: boolean, remainder: ItemStack[]): Inventory
    moveTo(ref: Ref<EntityStore>, locX: number, locY: number, locZ: number, componentAccessor: ComponentAccessor<EntityStore>): void
    canDecreaseItemStackDurability(ref: Ref<EntityStore>, componentAccessor: ComponentAccessor<EntityStore>): boolean
    canApplyItemStackPenalties(ref: Ref<EntityStore>, componentAccessor: ComponentAccessor<EntityStore>): boolean
    invalidateEquipmentNetwork(): void
    consumeEquipmentNetworkOutdated(): boolean
    getCurrentFallDistance(): number
    setCurrentFallDistance(currentFallDistance: number): void
  }

  class Player extends LivingEntity implements CommandSender {
    getUuid(): string;
    executeTriggers: boolean
    executeBlockDamage: boolean
    static getComponentType(): ComponentType<EntityStore, Player>
    copyFrom(oldPlayerComponent: Player): void
    init(uuid: string, playerRef: PlayerRef): void
    setNetworkId(id: number): void
    // getPlayerConfigData(): PlayerConfigData
    markNeedsSave(): void
    startClientReadyTimeout(): void
    handleClientReady(forced: boolean): void
    sendInventory(): void
    saveConfig(world: World, holder: Holder<EntityStore>): Promise<void>
    // getWorldMapTracker(): WorldMapTracker
    // getWindowManager(): WindowManager
    // getPageManager(): PageManager
    // getHudManager(): HudManager
    // getHotbarManager(): HotbarManager
    isFirstSpawn(): boolean
    setFirstSpawn(firstSpawn: boolean): void
    resetManagers(holder: Holder<EntityStore>): void
    notifyPickupItem(ref: Ref<EntityStore>, itemStack: ItemStack, position: Vector3d, componentAccessor: ComponentAccessor<EntityStore>): void
    isOverrideBlockPlacementRestrictions(): boolean
    setOverrideBlockPlacementRestrictions(ref: Ref<EntityStore>, overrideBlockPlacementRestrictions: boolean, componentAccessor: ComponentAccessor<EntityStore>): void
    sendMessage(message: Message): void
    hasPermission(id: string): boolean
    hasPermission(id: string, def: boolean): boolean
    addLocationChange(ref: Ref<EntityStore>, deltaX: number, deltaY: number, deltaZ: number, componentAccessor: ComponentAccessor<EntityStore>): void
    resetVelocity(velocity: Velocity): void
    processVelocitySample(dt: number, position: Vector3d, velocity: Velocity): void
    getRespawnPosition(ref: Ref<EntityStore>, worldName: string, componentAccessor: ComponentAccessor<EntityStore>): Transform
    hasSpawnProtection(): boolean
    isWaitingForClientReady(): boolean
    isHiddenFromLivingEntity(ref: Ref<EntityStore>, targetRef: Ref<EntityStore>, componentAccessor: ComponentAccessor<EntityStore>): boolean
    setClientViewRadius(clientViewRadius: number): void
    getClientViewRadius(): number
    getViewRadius(): number
    canDecreaseItemStackDurability(ref: Ref<EntityStore>, componentAccessor: ComponentAccessor<EntityStore>): boolean
    canApplyItemStackPenalties(ref: Ref<EntityStore>, componentAccessor: ComponentAccessor<EntityStore>): boolean
    setLastSpawnTimeNanos(lastSpawnTimeNanos: bigint): void
    getSinceLastSpawnNanos(): bigint
    getMountEntityId(): number
    setMountEntityId(mountEntityId: number): void
    getGameMode(): GameMode
    setGameMode(ref: Ref<EntityStore>, gameMode: GameMode, componentAccessor: ComponentAccessor<EntityStore>): void
    initGameMode(ref: Ref<EntityStore>, componentAccessor: ComponentAccessor<EntityStore>): void
    getDisplayName(): string;
  }

  type PickupLocation = 'Hotbar' | 'Storage'

  class PlayerSettings implements Component<EntityStore> {
    clone(): PlayerSettings
    cloneSerializable(): PlayerSettings
    static getComponentType(): ComponentType<EntityStore, PlayerSettings>
    static defaults(): PlayerSettings
    showEntityMarkers(): boolean
    armorItemsPreferredPickupLocation(): PickupLocation
    weaponAndToolItemsPreferredPickupLocation(): PickupLocation
    usableItemsItemsPreferredPickupLocation(): PickupLocation
    solidBlockItemsPreferredPickupLocation(): PickupLocation
    miscItemsPreferredPickupLocation(): PickupLocation
    creativeSettings(): PlayerCreativeSettings
  }

  class PlayerCreativeSettings {
    clone(): PlayerCreativeSettings
    allowNPCDetection(): boolean
    respondToHit(): boolean
  }

  //
  // INVENTORY
  //
  class Item {
    getItemIdForState(state: string): string | null
    getItemForState(state: string): Item | null
    isState(): boolean
    getStateForItem(item: Item): string | null
    getStateForItem(key: string): string | null
    getId(): string
    getBlockId(): string
    getTranslationKey(): string
    getDescriptionTranslationKey(): string
    getModel(): string
    getTexture(): string
    isConsumable(): boolean
    isVariant(): boolean
    getUsePlayerAnimations(): boolean
    getPlayerAnimationsId(): string
    getIcon(): string
    getScale(): number
    getReticleId(): string
    getItemLevel(): number
    getMaxStack(): number
    getQualityIndex(): number
    getCategories(): string[]
    getSoundEventId(): string
    getSoundEventIndex(): number
    hasBlockType(): boolean
    getMaxDurability(): number
    getInteractionVars(): Map<string, string>
    getDroppedItemAnimation(): string
    getDurabilityLossOnHit(): number
    getDisplayEntityStatsHUD(): number[]
    getClipsGeometry(): boolean
    getRenderDeployablePreview(): boolean
    getFuelQuality(): boolean
    getItemSoundSetIndex(): number
    hasRecipesToGenerate(): boolean
    dropsOnDeath(): boolean
  }

  class ItemStack {
    getItemId(): string
    getQuantity(): number
    isUnbreakable(): boolean
    isBroken(): boolean
    getMaxDurability(): number
    getDurability(): number
    isEmpty(): boolean
    getOverrideDroppedItemAnimation(): boolean
    setOverrideDroppedItemAnimation(b: boolean): void
    getBlockKey(): string | null
    getItem(): Item
    isValid(): boolean
    withDurability(durability: number): ItemStack
    withMaxDurability(maxDurabilty: number): ItemStack
    withIncreasedDurability(inc: number): ItemStack
    withRestoredDurability(maxDurabilty: number): ItemStack
    withState(state: string): ItemStack
    withQuantity(quantity: number): ItemStack
    isStackableWith(itemStack: ItemStack): boolean
    isEquivalentType(itemStack: ItemStack): boolean
    static isEmpty(itemFrom: ItemStack): boolean
    static isStackableWith(a: ItemStack, b: ItemStack): boolean
    static isEquivalentType(a: ItemStack, b: ItemStack): boolean
    static isSameItemType(a: ItemStack, b: ItemStack): boolean
  }

  class Inventory {
    unregister(): void
    markChanged(): void
    moveItem(fromSectionId: number, fromSlotId: number, quantity: number, toSectionId: number, toSlotId: number): void
    smartMoveItem(fromSectionId: number, fromSlotId: number, quantity: number, moveType: 'EquipOrMergeStack' | 'PutInHotbarOrWindow' | 'PutInHotbarOrBackpack'): void
    dropAllItemStacks(): ItemStack[]
    clear(): void
    getStorage(): ItemContainer
    getArmor(): ItemContainer
    getHotbar(): ItemContainer
    getUtility(): ItemContainer
    getTools(): ItemContainer
    getBackpack(): ItemContainer
    resizeBackpack(capacity: number, remainder: ItemStack[]): void
    // getCombinedHotbarFirst(): CombinedItemContainer
    // getCombinedStorageFirst(): CombinedItemContainer
    // getCombinedBackpackStorageHotbar(): CombinedItemContainer
    // getCombinedArmorHotbarStorage(): CombinedItemContainer
    // getCombinedArmorHotbarUtilityStorage(): CombinedItemContainer
    // getCombinedHotbarUtilityConsumableStorage(): CombinedItemContainer
    // getCombinedEverything(): CombinedItemContainer
    getContainerForItemPickup(item: Item, playerSettings: PlayerSettings): ItemContainer
    setActiveSlot(inventorySectionId: number, slot: number): void
    getActiveSlot(inventorySectionId: number): number
    getActiveHotbarSlot(): number
    setActiveHotbarSlot(slot: number): void
    getActiveHotbarItem(): ItemStack | null
    getActiveToolItem(): ItemStack | null
    getItemInHand(): ItemStack | null
    getActiveUtilitySlot(): number
    setActiveUtilitySlot(slot: number): void
    getUtilityItem(): ItemStack | null
    getActiveToolsSlot(): number
    setActiveToolsSlot(slot: number): void
    getToolsItem(): ItemStack | null
    getSectionById(id: number): ItemContainer
    consumeIsDirty(): boolean
    consumeNeedsSaving(): boolean
    setEntity(entity: LivingEntity): void
    sortStorage(type: 'NAME' | 'TYPE' | 'RARITY'): void
    setSortType(type: 'NAME' | 'TYPE' | 'RARITY'): void
    containsBrokenItem(): boolean
    doMigration(blockMigration: Function): void
    static ensureCapacity(inventory: Inventory, remainder: ItemStack[]): Inventory
    setUsingToolsItem(value: boolean): void
    usingToolsItem(): boolean
  }

  abstract class ItemContainer { // void >>> ItemStackSlotTransaction
    abstract getCapacity(): number
    abstract setGlobalFilter(filterType: 'ALLOW_INPUT_ONLY' | 'ALLOW_OUTPUT_ONLY' | 'ALLOW_ALL' | 'DENY_ALL'): void
    abstract clone(): ItemContainer
    canAddItemStackToSlot(slot: number, itemStack: ItemStack, allOrNothing: boolean, filter: boolean): boolean
    addItemStackToSlot(slot: number, itemStack: ItemStack): void // ItemStackSlotTransaction
    addItemStackToSlot(slot: number, itemStack: ItemStack, allOrNothing: boolean, filter: boolean): void // ItemStackSlotTransaction
    setItemStackForSlot(slot: number, itemStack: ItemStack): void // ItemStackSlotTransaction
    setItemStackForSlot(slot: number, itemStack: ItemStack, filter: boolean): void // ItemStackSlotTransaction
    getItemStack(slot: number): ItemStack
    replaceItemStackInSlot(slot: number, itemStackToRemove: ItemStack, itemStack: ItemStack): void // ItemStackSlotTransaction
    removeItemStackFromSlot(slot: number): void // ItemStackSlotTransaction
    removeItemStackFromSlot(slot: number, filter: boolean): void // ItemStackSlotTransaction
    removeItemStackFromSlot(slot: number, quantityToRemove: number): void // ItemStackSlotTransaction
    removeItemStackFromSlot(slot: number, quantityToRemove: number, allOrNothing: boolean, filter: boolean): void // ItemStackSlotTransaction
    removeItemStackFromSlot(slot: number, itemStackToRemove: ItemStack, quantityToRemove: number): void // ItemStackSlotTransaction
    removeItemStackFromSlot(slot: number, itemStackToRemove: ItemStack, quantityToRemove: number, allOrNothing: boolean, filter: boolean): void // ItemStackSlotTransaction
    // TODO: add more methods...
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
      playerRef: PlayerRef,
      world: World
    }) => void
    playerDisconnect: (e: {
      playerRef: PlayerRef,
      reason: string
    }) => void
    playerReady: (e: {
      playerRef: PlayerRef
      readyId: number
    }) => void
    playerMouseButton: (e: {
      playerRef: PlayerRef
      clientUseTime: bigint
      // itemInHand: Item
      // targetBlock: Vector3i
      // targetEntity: Entity
      // screenPoint: Vector2f
      // mouseButton: MouseButtonEvent
      isCancelled: boolean
    }) => void
    playerChat: (e: {
      sender: PlayerRef
      content: string
      formatter: Formatter
      targets: PlayerRef[]
      isCancelled: boolean
    }) => void
  }

  //
  // create and createComponent
  //
  interface ObjectFactoryMap {
    "uuid": [string, [string]]

    "transform": [Transform, [Vector3d] | [number, number, number] | []]
    "message": [Message, [string] | []]
    "vector3d": [Vector3d, [Vector3d] | [number, number, number] | []]
    "vector3i": [Vector3d, [Vector3d] | [number, number, number] | []]
  }
  interface ComponentFactoryMap {
    "transform": [TransformComponent, [Vector3d, Vector3d] | []]
    'velocity': [Velocity, [Velocity] | [Vector3d] | []]
    "teleport": [Teleport, [World, Transform] | [World, Vector3d, Vector3d] | [Transform] | [Vector3d, Vector3d]]
  }

  //
  // Command
  //
  type ArgTypeMap = {
    'string': string
    'integer': number
    'float': number
    'double': number
    'boolean': boolean
    'playerRef': PlayerRef
    'playerUuid': string
    'uuid': string
    'gameMode': GameMode
    'world': World
  }
  interface ArgDefinition {
    name: string;
    description: string;
    type: keyof ArgTypeMap;
    required?: boolean;
    default?: boolean;
    defaultValue?: any;
    defaultValueDescription?: string;
  }

  type ExtractArgs<A extends readonly ArgDefinition[]> = {
    [K in A[number] as K['name']]: ArgTypeMap[K['type']]
  };

  interface BaseCommandConfig<A extends readonly ArgDefinition[]> {
    name: string;
    description?: string;
    requiresConfirmation?: boolean;
    aliases?: string[];
    permission?: string;
    args?: A;
    subCommands?: Record<string, CommandConfig<any>>;
  }

  interface AsyncCommandConfig<A extends readonly ArgDefinition[]> extends BaseCommandConfig<A> {
    type?: "async";
    execute: (context: CommandContext, args: ExtractArgs<A>) => void;
  }

  interface PlayerCommandConfig<A extends readonly ArgDefinition[]> extends BaseCommandConfig<A> {
    type: "player";
    execute: (
      context: CommandContext,
      args: ExtractArgs<A>,
      store: Store<EntityStore>,
      ref: Ref<EntityStore>,
      playerRef: PlayerRef,
      world: World
    ) => void;
  }

  interface CollectionCommandConfig<A extends readonly ArgDefinition[]> extends BaseCommandConfig<A> {
    type: "collection";
  }

  type CommandConfig<A extends readonly ArgDefinition[]> =
    | AsyncCommandConfig<A>
    | PlayerCommandConfig<A>
    | CollectionCommandConfig<A>;

  //
  // System
  //
  interface SystemType {
    ticking: {
      query(): Query<EntityStore>
      tick(dt: number, index: number, store: Store<EntityStore>): void
    },
    entityTicking: {
      query(): Query<EntityStore>
      // any >>> CommandBuffer<EntityStore>
      tick(dt: number, index: number, archetypeChunk: ArchetypeChunk<EntityStore>, store: Store<EntityStore>, commandBuffer: any): void
    },
    delayedEntity: {
      intervalSec: number
      query(): Query<EntityStore>
      tick(dt: number, index: number, archetypeChunk: ArchetypeChunk<EntityStore>, store: Store<EntityStore>, commandBuffer: any): void
    }
  }
  interface SystemEventType<E extends EcsEvent> {
    entityEvent: {
      query(): Query<EntityStore>
      // any >>> CommandBuffer<EntityStore>
      handle(index: number, archetypeChunk: ArchetypeChunk<EntityStore>, store: Store<EntityStore>, commandBuffer: any, event: E): void
    }
  }
  interface SystemComponentType<E extends Component<EntityStore>> {
    refChange: {
      query(): Query<EntityStore>
      componentType(): ComponentType<EntityStore, E>
      // any >>> CommandBuffer<EntityStore>
      onComponentRemoved(ref: Ref<EntityStore>, component: E, store: Store<EntityStore>, commandBuffer: any): void
      // any >>> CommandBuffer<EntityStore>
      onComponentSet(ref: Ref<EntityStore>, valueComponent: E | null, component: E, store: Store<EntityStore>, commandBuffer: any): void
      // any >>> CommandBuffer<EntityStore>
      onComponentAdded(ref: Ref<EntityStore>, component: E, store: Store<EntityStore>, commandBuffer: any): void
    }
  }

  //
  // Custom Component
  //
  type CompType = 'string' | 'integer' | 'float' | 'double' | 'boolean' | 'array';

  type MapType<T extends CompType> =
    T extends 'string' ? string :
    T extends 'integer' ? number :
    T extends 'float' ? number :
    T extends 'double' ? number :
    T extends 'boolean' ? boolean :
    T extends 'array' ? any[] :
    T extends 'any' ? any :
    never;

  type DataResult<D extends Record<string, [CompType, any]>> = {
    [K in keyof D]: MapType<D[K][0]>
  };

  interface ComponentConfig<D extends Record<string, [CompType, any]>, Args extends any[]> {
    data: D;
    create: (...args: Args) => DataResult<D>;
  }

  interface CustomComponent<R, Args extends any[]> {
    readonly type: ComponentType<EntityStore, Component<EntityStore>>;
    create(...args: Args): R & Component<EntityStore>;
  }

  // test
  type InferCompType<T> =
    T extends string ? 'string' :
    T extends boolean ? 'boolean' :
    T extends number ? 'double' :
    T extends any[] ? 'array' :
    never;

  type InferData<T extends Record<string, any>> = {
    [K in keyof T]: [InferCompType<T[K]>, any]
  };

  // getPlayer()
  interface PlayerWrapper {
    getPlayerRef(): PlayerRef
    getPlayer(): Player
    getRef(): Ref<EntityStore>
    getStore(): Store<EntityStore>

    getHealth(): number
    getWorld(): World
    getUsername(): string
    getDisplayName(): string
    getInventory(): Inventory
    getPacketHandler(): PacketHandler

    getStats(): EntityStatMap | null
    addComponent<T extends Component<EntityStore>>(componentType: ComponentType<EntityStore, T>, component: T): void
    getComponent<T extends Component<EntityStore>>(componentType: ComponentType<EntityStore, T>): T
    sendMessage(message: string | Message): void
    sendEventTitle(primaryTitle: string, secondaryTitle: string, isMajor: boolean): void
    sendEventTitle(primaryTitle: string, secondaryTitle: string, isMajor: boolean, icon: string | null, duration: number, fadeInDuration: number, fadeOutDuration: number): void
    sendNotification(message: string | Message, secondaryMessage?: string | Message, icon?: string, item?: ItemStack, style?: NotificationStyle): void
    teleport(x: number, y: number, z: number, yaw?: number, pitch?: number): void
    teleport(position: Vector3d, rotation?: Vector3d): void // TODO: rename to Vector3f
    kill(): void

    getUuid(): string | null
    getPosition(): Vector3d | null
    getRotation(): Vector3d | null // TODO: RENAME to Vector3f
  }

  // @ts-ignore
  class Server extends Events<ServerEvents> {
    /**
      * Создает нативный Java-объект
      * @param type Тип объекта (например, 'vector3d')
      * @param args Аргументы конструктора
      */
    create<K extends keyof ObjectFactoryMap>(type: K, ...args: ObjectFactoryMap[K][1]): ObjectFactoryMap[K][0];
    /**
      * Создает нативный компонент
      * @param type Тип компонента (например, 'transform')
      * @param args Аргументы конструктора
      */
    createComponent<K extends keyof ComponentFactoryMap>(type: K, ...args: ComponentFactoryMap[K][1]): ComponentFactoryMap[K][0];

    addCommand<const A extends readonly ArgDefinition[]>(config: CommandConfig<A>): void;
    addPlayerCommand<const A extends readonly ArgDefinition[]>(config: Omit<PlayerCommandConfig<A>, "type">): void;
    addCommandCollection(config: Omit<CollectionCommandConfig<any>, "args"> & { subCommands: Record<string, CommandConfig<any>> }): void;

    addSystem<K extends keyof SystemType>(type: K, config: SystemType[K]): void
    addEventSystem<E extends EcsEvent>(type: keyof SystemEventType<InstanceOf<E>>, eventClass: E, config: SystemEventType<InstanceOf<E>>[keyof SystemEventType<InstanceOf<E>>]): void
    addComponentSystem<E extends Component<EntityStore>>(type: keyof SystemComponentType<E>, eventClass: InstanceOf<E>, config: SystemComponentType<E>[keyof SystemComponentType<E>]): void

    addAdapterInbound(callback: (handler: PacketHandler, packet: Packet) => boolean | void): void
    addAdapterOutbound(callback: (handler: PacketHandler, packet: Packet) => boolean | void): void

    // createCustomComponent<D extends Record<string, [CompType, any]>, A extends any[]>(config: ComponentConfig<D, A>): CustomComponent<DataResult<D>, A>;
    createCustomComponent<
      F extends (...args: any[]) => Record<string, any>,
      R = ReturnType<F>, // @ts-ignore
      D extends Record<string, [CompType, any]> = InferData<R>
    >(factory: F): CustomComponent<DataResult<D>, Parameters<F>>;

    getPlayer(v: {
      playerRef?: PlayerRef
      player?: Player
      store?: Store<EntityStore>
      ref?: Ref<EntityStore>
      uuid?: string
      commandContext?: CommandContext
      index?: number
      archetypeChunk?: ArchetypeChunk<EntityStore>
    }): Promise<PlayerWrapper | null>
  }

  // class PluginManifest {
  //   getGroup(): string
  //   getName(): string
  //   getVersion(): string
  //   getDescription(): string
  //   getAuthors(): ({ name: string, email: string, url: string })[]
  //   getWebsite(): string
  //   getMain(): string
  //   getServerVersion(): string
  //   getDependencies(): Map<string, string>
  //   getOptionalDependencies(): Map<string, string>
  //   getLoadBefore(): Map<string, string>
  //   getSubPlugins(): PluginManifest[]
  //   isDisabledByDefault(): boolean
  // }

  // class PluginInit {
  //   getDataDirectory(): string
  //   getPluginManifest(): PluginManifest
  // }

  // class PluginBase {
  //   getName(): string
  //   getIdentifier(): string
  //   getManifest(): PluginManifest
  //   getDataDirectory(): string
  //   getState(): 'NONE' | 'SETUP' | 'START' | 'ENABLED' | 'SHUTDOWN' | 'DISABLED'

  //   // readonly commandRegistry: Registry.CommandRegistry
  //   // TODO: add registry

  //   getBasePermission(): string
  //   isDisabled(): boolean
  //   isEnabled(): boolean
  //   getType(): 'PLUGIN'
  // }

  // class JavaPlugin extends PluginBase {
  //   getFile(): string
  // }
}

// @ts-ignore
declare global {
  // var plugin: API.JavaPlugin
  var server: API.Server
  var universe: Universe
}

export {};
