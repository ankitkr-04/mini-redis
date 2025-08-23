module redis {
    exports blocking;
    exports collections;
    exports commands;
    exports commands.base;
    exports commands.registry;
    exports commands.impl.basic;
    exports commands.impl.lists;
    exports commands.impl.streams;
    exports commands.impl.strings;
    exports events;
    exports errors;
    exports server.handler;
    exports protocol;
    exports storage;
    exports storage.expiry;
    exports storage.repositories;
    exports storage.types;
    exports storage.types.streams;
}
