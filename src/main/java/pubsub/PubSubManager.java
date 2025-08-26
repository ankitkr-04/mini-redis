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

import protocol.ResponseBuilder;
import server.ServerContext;

public class PubSubManager {
    private final Map<SocketChannel, PubSubState> clientStates = new ConcurrentHashMap<>();

    // Global subscription tracking for efficient publishing
    private final Map<String, Set<SocketChannel>> channelSubscribers = new ConcurrentHashMap<>();
    private final Map<String, Set<SocketChannel>> patternSubscribers = new ConcurrentHashMap<>();
    
    private final ServerContext serverContext;

    public PubSubManager(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    public PubSubState getOrCreateState(SocketChannel client) {
        if (client == null) {
            // For replicated commands, return a new PubSubState that's not in PubSub mode
            return new PubSubState();
        }
        return clientStates.computeIfAbsent(client, _ -> new PubSubState());
    }

    public void clearState(SocketChannel client) {
        if (client == null)
            return;

        PubSubState state = clientStates.remove(client);
        if (state != null) {
            // Clean up global subscription tracking
            for (String channel : state.getSubscribedChannels()) {
                removeFromGlobalSubscribers(channelSubscribers, channel, client);
            }

            for (String pattern : state.getSubscribedPatterns()) {
                removeFromGlobalSubscribers(patternSubscribers, pattern, client);
            }

            // Explicitly exit PubSub mode for correctness
            state.exitPubSubMode();
        }
    }

    /**
     * Helper method to remove client from global subscriber maps and cleanup empty
     * entries
     */
    private void removeFromGlobalSubscribers(Map<String, Set<SocketChannel>> subscriberMap,
            String key, SocketChannel client) {
        Set<SocketChannel> subscribers = subscriberMap.get(key);
        if (subscribers != null) {
            subscribers.remove(client);
            if (subscribers.isEmpty()) {
                subscriberMap.remove(key);
                // Clean up pattern cache if this was a pattern subscription
                if (subscriberMap == patternSubscribers) {
                    compiledPatterns.remove(key);
                }
            }
        }
    }

    public void clearAll() {
        // Exit PubSub mode for all clients before clearing
        for (PubSubState state : clientStates.values()) {
            state.exitPubSubMode();
        }

        clientStates.clear();
        channelSubscribers.clear();
        patternSubscribers.clear();
        compiledPatterns.clear(); // Clear pattern cache too
    }

    public boolean isInPubSubMode(SocketChannel client) {
        if (client == null)
            return false;
        PubSubState state = clientStates.get(client);
        return state != null && state.isInPubSubMode();
    }

    public void subscribe(SocketChannel client, List<String> channels) {
        if (client == null || channels == null || channels.isEmpty())
            return;

        PubSubState state = getOrCreateState(client);

        for (String channel : channels) {
            if (channel != null && !channel.isEmpty()) {
                boolean isNewChannel = !channelSubscribers.containsKey(channel);
                state.subscribeChannel(channel);

                // Update global tracking
                channelSubscribers.computeIfAbsent(channel, _ -> ConcurrentHashMap.newKeySet()).add(client);
                
                // Track active channels metrics
                if (isNewChannel) {
                    serverContext.getMetricsCollector().incrementActiveChannels();
                }
            }
        }
    }

    public void unsubscribe(SocketChannel client, List<String> channels) {
        if (client == null)
            return;

        PubSubState state = clientStates.get(client);
        if (state == null)
            return;

        if (channels == null || channels.isEmpty()) {
            // Unsubscribe from all channels
            Set<String> allChannels = new HashSet<>(state.getSubscribedChannels());
            for (String channel : allChannels) {
                state.unsubscribeChannel(channel);
                removeFromGlobalSubscribers(channelSubscribers, channel, client);
            }
        } else {
            // Unsubscribe from specific channels
            for (String channel : channels) {
                if (channel != null) {
                    state.unsubscribeChannel(channel);
                    removeFromGlobalSubscribers(channelSubscribers, channel, client);
                }
            }
        }
    }

    public void psubscribe(SocketChannel client, List<String> patterns) {
        if (client == null || patterns == null || patterns.isEmpty())
            return;

        PubSubState state = getOrCreateState(client);

        for (String pattern : patterns) {
            if (pattern != null && !pattern.isEmpty()) {
                state.subscribePattern(pattern);

                // Update global tracking
                patternSubscribers.computeIfAbsent(pattern, _ -> ConcurrentHashMap.newKeySet()).add(client);
            }
        }
    }

    public void punsubscribe(SocketChannel client, List<String> patterns) {
        if (client == null)
            return;

        PubSubState state = clientStates.get(client);
        if (state == null)
            return;

        if (patterns == null || patterns.isEmpty()) {
            // Unsubscribe from all patterns
            Set<String> allPatterns = new HashSet<>(state.getSubscribedPatterns());
            for (String pattern : allPatterns) {
                state.unsubscribePattern(pattern);
                removeFromGlobalSubscribers(patternSubscribers, pattern, client);
            }
        } else {
            // Unsubscribe from specific patterns
            for (String pattern : patterns) {
                if (pattern != null) {
                    state.unsubscribePattern(pattern);
                    removeFromGlobalSubscribers(patternSubscribers, pattern, client);
                }
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

    // Pattern cache for performance
    private final Map<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();

    /**
     * Get all clients subscribed to a specific channel (exact match)
     */
    public Set<SocketChannel> getChannelSubscribers(String channel) {
        Set<SocketChannel> subscribers = channelSubscribers.get(channel);
        return subscribers != null ? new HashSet<>(subscribers) : Collections.emptySet();
    }

    /**
     * Get all clients subscribed to patterns that match the given channel
     */
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

    /**
     * Get all subscribers for a channel (both exact matches and pattern matches)
     */
    public Set<SocketChannel> getAllSubscribers(String channel) {
        Set<SocketChannel> allSubscribers = new HashSet<>();
        allSubscribers.addAll(getChannelSubscribers(channel));
        allSubscribers.addAll(getPatternMatchingSubscribers(channel));
        return allSubscribers;
    }

    /**
     * Publish a message to all subscribers of a channel (Redis-compatible)
     */
    public void publish(String channel, String message) {
        if (channel == null || message == null)
            return;

        Set<SocketChannel> processedClients = new HashSet<>();

        // Send to exact channel subscribers
        Set<SocketChannel> channelSubscribers = getChannelSubscribers(channel);
        for (SocketChannel client : channelSubscribers) {
            try {
                sendMessageToChannelSubscriber(client, channel, message);
                processedClients.add(client);
            } catch (Exception e) {
                clearState(client);
            }
        }

        // Send to pattern subscribers (avoid duplicates)
        for (Map.Entry<String, Set<SocketChannel>> entry : patternSubscribers.entrySet()) {
            String pattern = entry.getKey();
            if (matchesPattern(channel, pattern)) {
                for (SocketChannel client : entry.getValue()) {
                    if (!processedClients.contains(client)) {
                        try {
                            sendMessageToPatternSubscriber(client, pattern, channel, message);
                        } catch (Exception e) {
                            clearState(client);
                        }
                    }
                }
            }
        }
        
        // Record metrics for published message
        serverContext.getMetricsCollector().incrementMessagesPublished();
    }

    /**
     * Optimized glob pattern matching with caching and safer regex conversion
     * Supports * (any sequence) and ? (single char)
     */
    private boolean matchesPattern(String channel, String globPattern) {
        Pattern compiledPattern = compiledPatterns.computeIfAbsent(globPattern, pattern -> {
            try {
                // Safer glob to regex conversion: quote everything, then selectively unquote
                // globs
                String regex = Pattern.quote(pattern)
                        .replace("\\*", ".*") // Convert quoted \* back to .*
                        .replace("\\?", "."); // Convert quoted \? back to .

                return Pattern.compile(regex);
            } catch (Exception e) {
                // If pattern is invalid, create a pattern that never matches
                return Pattern.compile("(?!)"); // negative lookahead that always fails
            }
        });

        return compiledPattern.matcher(channel).matches();
    }

    /**
     * Send message to a direct channel subscriber
     * RESP format: ["message", channel, message]
     */
    protected void sendMessageToChannelSubscriber(SocketChannel client, String channel, String message) {
        try {
            ByteBuffer response = ResponseBuilder.array(List.of(
                    "message",
                    channel,
                    message));
            client.write(response);
        } catch (Exception e) {
            // handle broken pipe, closed channel, etc.
            e.printStackTrace();
        }
    }

    /**
     * Send message to a pattern subscriber
     * RESP format: ["pmessage", pattern, channel, message]
     */
    protected void sendMessageToPatternSubscriber(SocketChannel client, String pattern, String channel,
            String message) {
        try {
            ByteBuffer response = ResponseBuilder.array(List.of(
                    "pmessage",
                    pattern,
                    channel,
                    message));
            client.write(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Debug/monitoring methods

    public int getTotalChannelSubscriptions() {
        return channelSubscribers.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    public int getTotalPatternSubscriptions() {
        return patternSubscribers.values().stream()
                .mapToInt(Set::size)
                .sum();
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

    /**
     * Get subscriber counts per channel (like Redis PUBSUB CHANNELS)
     */
    public Map<String, Integer> getChannelSubscriberCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<String, Set<SocketChannel>> entry : channelSubscribers.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        return counts;
    }

    /**
     * Get subscriber counts per pattern (like Redis PUBSUB NUMPAT)
     */
    public Map<String, Integer> getPatternSubscriberCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<String, Set<SocketChannel>> entry : patternSubscribers.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        return counts;
    }

    /**
     * Get all active channels with subscribers
     */
    public Set<String> getActiveChannels() {
        return new HashSet<>(channelSubscribers.keySet());
    }

    /**
     * Get all active patterns with subscribers
     */
    public Set<String> getActivePatterns() {
        return new HashSet<>(patternSubscribers.keySet());
    }
}