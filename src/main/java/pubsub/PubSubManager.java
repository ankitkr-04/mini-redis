package pubsub;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import protocol.ResponseBuilder;
import server.ServerContext;

/**
 * Manages Redis-like Pub/Sub functionality:
 * - Tracks per-client PubSub state.
 * - Manages subscriptions to channels and patterns.
 * - Publishes messages to subscribers.
 *
 * Supports exact channel subscriptions and glob-style pattern subscriptions
 * with caching for performance.
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public class PubSubManager {

    /** Logger instance for error/debug logging */
    private static final Logger LOGGER = LoggerFactory.getLogger(PubSubManager.class);

    /** RESP message type keywords */
    private static final String MESSAGE_TYPE = "message";
    private static final String PMESSAGE_TYPE = "pmessage";

    /** Regex fallback for invalid patterns (never matches) */
    private static final String NEVER_MATCH_REGEX = "(?!)";

    /** Glob wildcards */
    private static final String WILDCARD_STAR = "\\*";
    private static final String WILDCARD_QMARK = "\\?";

    /** Compiled regex replacements */
    private static final String REGEX_STAR = ".*";
    private static final String REGEX_QMARK = ".";

    private final Map<SocketChannel, PubSubState> clientStates = new ConcurrentHashMap<>();
    private final Map<String, Set<SocketChannel>> channelSubscribers = new ConcurrentHashMap<>();
    private final Map<String, Set<SocketChannel>> patternSubscribers = new ConcurrentHashMap<>();
    private final Map<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();

    private final ServerContext serverContext;

    public PubSubManager(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    /**
     * Get or create a PubSub state for a client.
     * If the client is null (replication case), returns a new empty state.
     */
    public PubSubState getOrCreateState(SocketChannel client) {
        if (client == null) {
            return new PubSubState();
        }
        return clientStates.computeIfAbsent(client, _ -> new PubSubState());
    }

    /**
     * Clear PubSub state for a client and remove global subscriptions.
     */
    public void clearState(SocketChannel client) {
        if (client == null)
            return;

        PubSubState state = clientStates.remove(client);
        if (state != null) {
            for (String channel : state.getSubscribedChannels()) {
                removeFromGlobalSubscribers(channelSubscribers, channel, client);
            }
            for (String pattern : state.getSubscribedPatterns()) {
                removeFromGlobalSubscribers(patternSubscribers, pattern, client);
            }
            state.exitPubSubMode();
        }
    }

    private void removeFromGlobalSubscribers(
            Map<String, Set<SocketChannel>> subscriberMap,
            String subscriptionKey,
            SocketChannel client) {

        Set<SocketChannel> subscribers = subscriberMap.get(subscriptionKey);
        if (subscribers != null) {
            subscribers.remove(client);
            if (subscribers.isEmpty()) {
                subscriberMap.remove(subscriptionKey);
                if (subscriberMap == patternSubscribers) {
                    compiledPatterns.remove(subscriptionKey);
                }
            }
        }
    }

    /**
     * Clear all PubSub states and subscriptions.
     */
    public void clearAll() {
        clientStates.values().forEach(PubSubState::exitPubSubMode);
        clientStates.clear();
        channelSubscribers.clear();
        patternSubscribers.clear();
        compiledPatterns.clear();
    }

    public boolean isInPubSubMode(SocketChannel client) {
        if (client == null)
            return false;
        PubSubState state = clientStates.get(client);
        return state != null && state.isInPubSubMode();
    }

    /**
     * Subscribe client to one or more channels.
     */
    public void subscribe(SocketChannel client, List<String> channels) {
        if (client == null || channels == null || channels.isEmpty())
            return;

        PubSubState state = getOrCreateState(client);

        for (String channel : channels) {
            if (channel != null && !channel.isEmpty()) {
                boolean isNewChannel = !channelSubscribers.containsKey(channel);
                state.subscribeChannel(channel);
                channelSubscribers.computeIfAbsent(channel, _ -> ConcurrentHashMap.newKeySet()).add(client);

                if (isNewChannel) {
                    serverContext.getMetricsCollector().incrementActiveChannels();
                }
            }
        }
    }

    /**
     * Unsubscribe client from specific or all channels.
     */
    public void unsubscribe(SocketChannel client, List<String> channels) {
        if (client == null)
            return;
        PubSubState state = clientStates.get(client);
        if (state == null)
            return;

        Set<String> targetChannels = (channels == null || channels.isEmpty())
                ? new HashSet<>(state.getSubscribedChannels())
                : new HashSet<>(channels);

        for (String channel : targetChannels) {
            if (channel != null) {
                state.unsubscribeChannel(channel);
                removeFromGlobalSubscribers(channelSubscribers, channel, client);
            }
        }
    }

    /**
     * Subscribe client to patterns.
     */
    public void psubscribe(SocketChannel client, List<String> patterns) {
        if (client == null || patterns == null || patterns.isEmpty())
            return;

        PubSubState state = getOrCreateState(client);

        for (String pattern : patterns) {
            if (pattern != null && !pattern.isEmpty()) {
                state.subscribePattern(pattern);
                patternSubscribers.computeIfAbsent(pattern, _ -> ConcurrentHashMap.newKeySet()).add(client);
            }
        }
    }

    /**
     * Unsubscribe client from specific or all patterns.
     */
    public void punsubscribe(SocketChannel client, List<String> patterns) {
        if (client == null)
            return;
        PubSubState state = clientStates.get(client);
        if (state == null)
            return;

        Set<String> targetPatterns = (patterns == null || patterns.isEmpty())
                ? new HashSet<>(state.getSubscribedPatterns())
                : new HashSet<>(patterns);

        for (String pattern : targetPatterns) {
            if (pattern != null) {
                state.unsubscribePattern(pattern);
                removeFromGlobalSubscribers(patternSubscribers, pattern, client);
            }
        }
    }

    public int subscriptionCount(SocketChannel client) {
        if (client == null)
            return 0;
        PubSubState state = clientStates.get(client);
        if (state == null)
            return 0;
        return state.getSubscribedChannels().size() + state.getSubscribedPatterns().size();
    }

    public Set<SocketChannel> getChannelSubscribers(String channel) {
        Set<SocketChannel> subscribers = channelSubscribers.get(channel);
        return subscribers != null ? new HashSet<>(subscribers) : Collections.emptySet();
    }

    public Set<SocketChannel> getPatternMatchingSubscribers(String channel) {
        Set<SocketChannel> matchingClients = new HashSet<>();
        for (Map.Entry<String, Set<SocketChannel>> entry : patternSubscribers.entrySet()) {
            String pattern = entry.getKey();
            if (matchesPattern(channel, pattern)) {
                matchingClients.addAll(entry.getValue());
            }
        }
        return matchingClients;
    }

    public Set<SocketChannel> getAllSubscribers(String channel) {
        Set<SocketChannel> allSubscribers = new HashSet<>(getChannelSubscribers(channel));
        allSubscribers.addAll(getPatternMatchingSubscribers(channel));
        return allSubscribers;
    }

    /**
     * Publish a message to all subscribers of a channel.
     */
    public void publish(String channel, String message) {
        if (channel == null || message == null)
            return;

        Set<SocketChannel> processedClients = new HashSet<>();

        // Channel subscribers
        for (SocketChannel client : getChannelSubscribers(channel)) {
            try {
                sendMessageToChannelSubscriber(client, channel, message);
                processedClients.add(client);
            } catch (Exception e) {
                LOGGER.error("Failed to send message to channel subscriber: {}", client, e);
                clearState(client);
            }
        }

        // Pattern subscribers
        for (Map.Entry<String, Set<SocketChannel>> entry : patternSubscribers.entrySet()) {
            String pattern = entry.getKey();
            if (matchesPattern(channel, pattern)) {
                for (SocketChannel client : entry.getValue()) {
                    if (!processedClients.contains(client)) {
                        try {
                            sendMessageToPatternSubscriber(client, pattern, channel, message);
                        } catch (Exception e) {
                            LOGGER.error("Failed to send message to pattern subscriber: {}", client, e);
                            clearState(client);
                        }
                    }
                }
            }
        }

        serverContext.getMetricsCollector().incrementMessagesPublished();
    }

    private boolean matchesPattern(String channel, String globPattern) {
        Pattern compiledPattern = compiledPatterns.computeIfAbsent(globPattern, pattern -> {
            try {
                String regex = Pattern.quote(pattern)
                        .replace(WILDCARD_STAR, REGEX_STAR)
                        .replace(WILDCARD_QMARK, REGEX_QMARK);
                return Pattern.compile(regex);
            } catch (Exception e) {
                LOGGER.warn("Invalid pattern {}, using fallback never-match regex", globPattern, e);
                return Pattern.compile(NEVER_MATCH_REGEX);
            }
        });
        return compiledPattern.matcher(channel).matches();
    }

    protected void sendMessageToChannelSubscriber(SocketChannel client, String channel, String message)
            throws Exception {
        ByteBuffer response = ResponseBuilder.array(List.of(MESSAGE_TYPE, channel, message));
        client.write(response);
    }

    protected void sendMessageToPatternSubscriber(SocketChannel client, String pattern, String channel, String message)
            throws Exception {
        ByteBuffer response = ResponseBuilder.array(List.of(PMESSAGE_TYPE, pattern, channel, message));
        client.write(response);
    }

    // ---- Monitoring / Debugging ----

    public int getTotalChannelSubscriptions() {
        return channelSubscribers.values().stream().mapToInt(Set::size).sum();
    }

    public int getTotalPatternSubscriptions() {
        return patternSubscribers.values().stream().mapToInt(Set::size).sum();
    }

    public int getTotalUniqueChannels() {
        return channelSubscribers.size();
    }

    public int getTotalUniquePatterns() {
        return patternSubscribers.size();
    }

    public int getTotalClients() {
        return clientStates.size();
    }

    public int getCompiledPatternCacheSize() {
        return compiledPatterns.size();
    }

    public Map<String, Integer> getChannelSubscriberCounts() {
        Map<String, Integer> counts = new HashMap<>();
        channelSubscribers.forEach((ch, subs) -> counts.put(ch, subs.size()));
        return counts;
    }

    public Map<String, Integer> getPatternSubscriberCounts() {
        Map<String, Integer> counts = new HashMap<>();
        patternSubscribers.forEach((pat, subs) -> counts.put(pat, subs.size()));
        return counts;
    }

    public Set<String> getActiveChannels() {
        return new HashSet<>(channelSubscribers.keySet());
    }

    public Set<String> getActivePatterns() {
        return new HashSet<>(patternSubscribers.keySet());
    }
}
