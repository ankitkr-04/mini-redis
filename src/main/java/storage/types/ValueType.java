package storage.types;

public enum ValueType {
    NONE("none"), STRING("string"), LIST("list"), SET("set"), HASH("hash"), STREAM("stream"), ZSET("zset");

    private final String displayName;

    ValueType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
