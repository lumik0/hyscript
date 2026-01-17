import Events from './Events';

const nativeServer = (globalThis as any).__nativeServer;

export default class Server extends Events {
  createComponent(type: string, ...args: any[]) {
    return nativeServer.createComponent(type, ...args);
  }
  create(type: string, ...args: any[]) {
    return nativeServer.create(type, ...args);
  }

  addCommand(config: any) {
    return nativeServer.addCommand(config);
  }
  addCommandCollection(config: any) {
    return nativeServer.addCommandCollection(config);
  }
  addPlayerCommand(config: any) {
    return nativeServer.addPlayerCommand(config);
  }

  addSystem(type: string, config: any) {
    return nativeServer.addSystem(type, config);
  }
  addEventSystem(type: string, eventClass: any, config: any) {
    return nativeServer.addEventSystem(type, eventClass, config);
  }
}
