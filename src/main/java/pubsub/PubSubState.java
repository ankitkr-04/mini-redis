package pubsub;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains the state for publish-subscribe messaging.
 * Tracks channels and patterns a client is subscribed to,
 * and manages the pub/sub mode accordingly.
 */
public final class PubSubState {

    private static final Logger LOGGER = LoggerFactory.getLogger(PubSubState.class);

    // Global constant for initial pub/sub mode
    private static final boolean DEFAULT_PUBSUB_MODE = false;

    private boolean pubSubMode = DEFAULT_PUBSUB_MODE;
    private final Set<String> channelSubscriptions = new ConcurrentSkipListSet<>();
    private final Set<String> patternSubscriptions = new ConcurrentSkipListSet<>();

    /**
     * Checks if the client is currently in pub/sub mode.
     *
     * @return true if in pub/sub mode, false otherwise
     */
    public boolean isInPubSubMode() {
        return pubSubMode;
    }

    /**
     * Enters pub/sub mode.
     */
    public void enterPubSubMode() {
        pubSubMode = true;
        LOGGER.debug("Entered pub/sub mode.");
    }

    /**
     * Exits pub/sub mode and clears all subscriptions.
     */
    public void exitPubSubMode() {
        pubSubMode = DEFAULT_PUBSUB_MODE;
        channelSubscriptions.clear();
        patternSubscriptions.clear();
        LOGGER.debug("Exited pub/sub mode and cleared all subscriptions.");
    }

    /**
     * Subscribes to a specific channel.
     *
     * @param channel the channel to subscribe to
     */
    public void subscribeChannel(String channel) {
        channelSubscriptions.add(channel);
        pubSubMode = true;
        LOGGER.trace("Subscribed to channel: {}", channel);
    }

    /**
     * Unsubscribes from a specific channel.
     * Exits pub/sub mode if no subscriptions remain.
     *
     * @param channel the channel to unsubscribe from
     */
    public void unsubscribeChannel(String channel) {
        channelSubscriptions.remove(channel);
        LOGGER.trace("Unsubscribed from channel: {}", channel);
        checkAndExitPubSubMode();
    }

    /**
     * Subscribes to a pattern.
     *
     * @param pattern the pattern to subscribe to
     */
    public void subscribePattern(String pattern) {
        patternSubscriptions.add(pattern);
        pubSubMode = true;
        LOGGER.trace("Subscribed to pattern: {}", pattern);
    }

    /**
     * Unsubscribes from a pattern.
     * Exits pub/sub mode if no subscriptions remain.
     *
     * @param pattern the pattern to unsubscribe from
     */
    public void unsubscribePattern(String pattern) {
        patternSubscriptions.remove(pattern);
        LOGGER.trace("Unsubscribed from pattern: {}", pattern);
        checkAndExitPubSubMode();
    }

    /**
     * Returns a copy of the current channel subscriptions.
     *
     * @return set of subscribed channels
     */
    public Set<String> getSubscribedChannels() {
        return Set.copyOf(channelSubscriptions);
    }

    /**
     * Returns a copy of the current pattern subscriptions.
     *
     * @return set of subscribed patterns
     */
    public Set<String> getSubscribedPatterns() {
        return Set.copyOf(patternSubscriptions);
    }

    /**
     * Checks if there are no subscriptions left and exits pub/sub mode if needed.
     */
    private void checkAndExitPubSubMode() {
        if (channelSubscriptions.isEmpty() && patternSubscriptions.isEmpty()) {
            exitPubSubMode();
        }
    }
}
