package enums;

public enum DataType {
    NONE("none"), STRING("string"), LIST("list"), SET("set"), ZSET("zset"), HASH("hash"), STREAM(
            "stream");

    private final String value;

    DataType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
