module redis {
    requires org.slf4j;

    exports blocking;
    exports collections;
    exports commands.base;
    exports commands.core;
    exports commands.context;
    exports commands.registry;
    exports commands.impl.replication;
    exports commands.result;
    exports commands.impl.basic;
    exports commands.impl.lists;
    exports commands.impl.streams;
    exports commands.impl.strings;
    exports commands.impl.transaction;
    exports events;
    exports errors;
    exports protocol;
    exports server;
    exports replication;
    exports storage;
    exports storage.expiry;
    exports storage.repositories;
    exports storage.types;
    exports storage.types.streams;
    exports transaction;
    exports commands.validation;
    exports config;
    exports utils;
}
