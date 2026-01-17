/// <reference path="../sdk.d.ts" />

server.on('setup', () => {
  // This event is triggered only when the plugin is started
  // For example, to register something
});

server.on("playerChat", e => {
  e.sender.sendMessage(Message.raw("Hello from HyScript!"));
});
