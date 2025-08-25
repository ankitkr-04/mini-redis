package pubsub;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public final class PubSubState {
    private boolean inPubSubMode = false;
    private final Set<String> subscribedChannels = new ConcurrentSkipListSet<>();
    private final Set<String> subscribedPatterns = new ConcurrentSkipListSet<>();

    public boolean isInPubSubMode() {
        return inPubSubMode;
    }

    public void enterPubSubMode() {
        this.inPubSubMode = true;
    }

    public void exitPubSubMode() {
        this.inPubSubMode = false;
        this.subscribedChannels.clear();
        this.subscribedPatterns.clear();
    }

    public void subscribeChannel(String channel) {
        subscribedChannels.add(channel);
        inPubSubMode = true;
    }

    public void unsubscribeChannel(String channel) {
        subscribedChannels.remove(channel);
        if (subscribedChannels.isEmpty() && subscribedPatterns.isEmpty()) {
            exitPubSubMode();
        }
    }

    public void subscribePattern(String pattern) {
        subscribedPatterns.add(pattern);
        inPubSubMode = true;
    }

    public void unsubscribePattern(String pattern) {
        subscribedPatterns.remove(pattern);
        if (subscribedChannels.isEmpty() && subscribedPatterns.isEmpty()) {
            exitPubSubMode();
        }
    }

    public Set<String> getSubscribedChannels() {
        return Set.copyOf(subscribedChannels);
    }

    public Set<String> getSubscribedPatterns() {
        return Set.copyOf(subscribedPatterns);
    }
}
