package storage.types;

/**
 * Enum representing the supported Redis value types.
 * Each type has a display name for user-friendly representation.
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public enum ValueType {
    NONE("none"),
    STRING("string"),
    LIST("list"),
    SET("set"),
    HASH("hash"),
    STREAM("stream"),
    ZSET("zset");

    private final String displayName;

    /**
     * Constructs a ValueType with the specified display name.
     *
     * @param displayName the user-friendly name for the value type
     */
    ValueType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display name of the value type.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
