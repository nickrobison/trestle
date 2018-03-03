const ctx: Worker = self as any;

ctx.addEventListener("message", (message) => {
    console.debug("Message on worker!", message.data);
});
