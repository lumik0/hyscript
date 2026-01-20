// src/main/resources/sdk/Events.ts
class EventHandle {
  event;
  callback;
  owner;
  priorityName;
  keyName;
  constructor(event, callback, owner, priorityName = 2 /* NORMAL */) {
    this.event = event;
    this.callback = callback;
    this.owner = owner;
    this.priorityName = priorityName;
  }
  key(name) {
    this.keyName = name;
    return this;
  }
  getKey() {
    return this.keyName;
  }
  priority(priority) {
    this.priorityName = priority;
    return this;
  }
  getPriority() {
    return this.priorityName;
  }
  remove() {
    this.owner["_removeHandle"](this);
  }
}

class Events {
  customListeners = {};
  on(evt, callback, priority = 2 /* NORMAL */) {
    const handle = new EventHandle(evt, callback, this, priority);
    let arr = this.customListeners[evt];
    if (!arr) {
      arr = [];
      this.customListeners[evt] = arr;
    }
    let i = arr.findIndex((h) => h.getPriority() > priority);
    if (i === -1) {
      arr.push(handle);
    } else {
      arr.splice(i, 0, handle);
    }
    return handle;
  }
  once(evt, callback, priority = 2 /* NORMAL */) {
    const wrapper = (...args) => {
      callback(...args);
      this.off(evt, wrapper);
    };
    return this.on(evt, wrapper, priority);
  }
  off(evt, callback) {
    const arr = this.customListeners[evt];
    if (!arr)
      return false;
    const before = arr.length;
    this.customListeners[evt] = arr.filter((h) => h.callback !== callback);
    return arr.length < before;
  }
  async call(evt, event = undefined) {
    const arr = this.customListeners[evt];
    if (!arr)
      return event;
    for (const h of arr) {
      const r = h.callback(event);
      if (r instanceof Promise)
        await r;
    }
    return event;
  }
  callSync(evt, event = undefined) {
    const arr = this.customListeners[evt];
    if (!arr)
      return event;
    for (const h of arr) {
      h.callback(event);
    }
    return event;
  }
  emit(evt, ...args) {
    const arr = this.customListeners[evt];
    if (!arr)
      return false;
    for (const h of arr) {
      h.callback(...args);
    }
    return arr.length > 0;
  }
  async emitR(evt, ...args) {
    const arr = this.customListeners[evt];
    if (!arr)
      return [];
    const results = [];
    for (const h of arr) {
      const r = h.callback(...args);
      results.push(r instanceof Promise ? await r : r);
    }
    return results;
  }
  async wait(type, timeout = 1e7) {
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.off(type, func);
      }, timeout);
      const func = (...args) => {
        clearTimeout(timer);
        this.off("message", func);
        resolve(args);
      };
      this.on(type, func);
    });
  }
  removeByKey(key) {
    let removed = false;
    for (const evt in this.customListeners) {
      const arr = this.customListeners[evt];
      if (!arr)
        continue;
      const before = arr.length;
      this.customListeners[evt] = arr.filter((h) => h.getKey() !== key);
      if (this.customListeners[evt].length < before)
        removed = true;
    }
    return removed;
  }
  _removeHandle(handle) {
    const arr = this.customListeners[handle.event];
    if (!arr)
      return;
    this.customListeners[handle.event] = arr.filter((h) => h !== handle);
  }
  removeAllEvents() {
    this.customListeners = {};
  }
}

// src/main/resources/sdk/Server.ts
var nativeServer = globalThis.__nativeServer;

class Server extends Events {
  createComponent(type, ...args) {
    return nativeServer.createComponent(type, ...args);
  }
  create(type, ...args) {
    return nativeServer.create(type, ...args);
  }
  addCommand(config) {
    return nativeServer.addCommand(config);
  }
  addCommandCollection(config) {
    return nativeServer.addCommandCollection(config);
  }
  addPlayerCommand(config) {
    return nativeServer.addPlayerCommand(config);
  }
  addSystem(type, config) {
    return nativeServer.addSystem(type, config);
  }
  addEventSystem(type, eventClass, config) {
    return nativeServer.addEventSystem(type, eventClass, config);
  }
  addComponentSystem(type, componentClass, config) {
    return nativeServer.addComponentSystem(type, componentClass, config);
  }
  addAdapterInbound(callback) {
    return nativeServer.addAdapterInbound(callback);
  }
  addAdapterOutbound(callback) {
    return nativeServer.addAdapterOutbound(callback);
  }
  getPlayer(v) {
    return nativeServer.getPlayer(v);
  }
}

// src/main/resources/sdk/main.ts
var server = new Server;
globalThis.server = server;
