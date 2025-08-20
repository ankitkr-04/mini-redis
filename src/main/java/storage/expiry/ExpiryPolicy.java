package storage.expiry;

import java.time.Duration;
import java.time.Instant;

public sealed interface ExpiryPolicy
        permits ExpiryPolicy.Never, ExpiryPolicy.AfterDuration, ExpiryPolicy.AtTime {
    boolean isExpired();

    static ExpiryPolicy never() {
        return new Never();
    }

    record Never() implements ExpiryPolicy {
        @Override
        public boolean isExpired() {
            return false;
        }
    }

    static ExpiryPolicy at(Instant expireAt) {
        return new AtTime(expireAt);
    }

    static ExpiryPolicy inMillis(long millis) {
        return new AtTime(Instant.now().plusMillis(millis));
    }

    record AtTime(Instant expireAt) implements ExpiryPolicy {
        @Override
        public boolean isExpired() {
            return Instant.now().isAfter(expireAt);
        }
    }

    static ExpiryPolicy after(Duration duration) {
        return new AfterDuration(Instant.now(), duration);
    }



    record AfterDuration(Instant createdAt, Duration duration) implements ExpiryPolicy {
        @Override
        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(duration));
        }
    }

}
