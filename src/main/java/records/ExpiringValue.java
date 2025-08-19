package records;

public record ExpiringValue(String value, long expiryTime) {
    private static final long NO_EXPIRY = -1L;

    public static ExpiringValue withoutExpiry(String value) {
        return new ExpiringValue(value, NO_EXPIRY);
    }

    public static ExpiringValue withExpiry(String value, long expiryMs) {
        return new ExpiringValue(value, System.currentTimeMillis() + expiryMs);
    }

    public boolean isExpired() {
        return expiryTime != NO_EXPIRY && System.currentTimeMillis() >= expiryTime;
    }
}
