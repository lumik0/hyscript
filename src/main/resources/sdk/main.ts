import Server from './Server'

const server = new Server();

(globalThis as any).server = server;

// export const internalLog = (msg: string) => (globalThis as any).InternalApi.log(msg);
