package datatype;

import java.time.Instant;

public record ExpiringMetadata(long expirationMillis) {
    public static ExpiringMetadata never() {
        return new ExpiringMetadata(-1);
    }

    public static ExpiringMetadata at(Instant time) {
        return new ExpiringMetadata(time.toEpochMilli());
    }

    public static ExpiringMetadata in(long milliseconds) {
        return new ExpiringMetadata(System.currentTimeMillis() + milliseconds);
    }

    public boolean isExpired() {
        return expirationMillis > 0 && System.currentTimeMillis() > expirationMillis;
    }

    public boolean neverExpires() {
        return expirationMillis == -1;
    }
}
