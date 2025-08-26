package storage.expiry;

import java.time.Duration;
import java.time.Instant;

/**
 * Defines policies for expiring objects in storage.
 * <p>
 * Provides implementations for never expiring, expiring after a duration,
 * or expiring at a specific time.
 * </p>
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public sealed interface ExpiryPolicy
        permits ExpiryPolicy.Never, ExpiryPolicy.AfterDuration, ExpiryPolicy.AtTime {

    /**
     * Checks if the policy has expired.
     *
     * @return true if expired, false otherwise
     */
    boolean isExpired();

    /**
     * Returns a policy that never expires.
     *
     * @return ExpiryPolicy instance
     */
    static ExpiryPolicy never() {
        return new Never();
    }

    /**
     * Policy that never expires.
     */
    record Never() implements ExpiryPolicy {
        @Override
        public boolean isExpired() {
            return false;
        }
    }

    /**
     * Returns a policy that expires at the specified instant.
     *
     * @param expirationTime the time at which the policy expires
     * @return ExpiryPolicy instance
     */
    static ExpiryPolicy at(Instant expirationTime) {
        return new AtTime(expirationTime);
    }

    /**
     * Returns a policy that expires after the specified milliseconds from now.
     *
     * @param milliseconds duration in milliseconds until expiration
     * @return ExpiryPolicy instance
     */
    static ExpiryPolicy inMillis(long milliseconds) {
        return new AtTime(Instant.now().plusMillis(milliseconds));
    }

    /**
     * Policy that expires at a specific instant.
     *
     * @param expirationTime the time at which the policy expires
     */
    record AtTime(Instant expirationTime) implements ExpiryPolicy {
        @Override
        public boolean isExpired() {
            return Instant.now().isAfter(expirationTime);
        }
    }

    /**
     * Returns a policy that expires after the specified duration from now.
     *
     * @param duration duration until expiration
     * @return ExpiryPolicy instance
     */
    static ExpiryPolicy after(Duration duration) {
        return new AfterDuration(Instant.now(), duration);
    }

    /**
     * Policy that expires after a duration from creation.
     *
     * @param creationTime the time the policy was created
     * @param duration     duration until expiration
     */
    record AfterDuration(Instant creationTime, Duration duration) implements ExpiryPolicy {
        @Override
        public boolean isExpired() {
            return Instant.now().isAfter(creationTime.plus(duration));
        }
    }
}
