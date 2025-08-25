package storage.types;

/**
 * Enum to specify the direction for pop operations on lists.
 * Helps reduce code duplication between left and right pop operations.
 */
public enum PopDirection {
    LEFT("left"),
    RIGHT("right");

    private final String name;

    PopDirection(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isLeft() {
        return this == LEFT;
    }

    public boolean isRight() {
        return this == RIGHT;
    }
}
