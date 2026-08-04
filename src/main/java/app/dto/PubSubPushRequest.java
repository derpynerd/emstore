package app.dto;

import java.math.BigInteger;
import java.util.Map;

public record PubSubPushRequest(
        PubSubMessage message,
        String subscription
) {
    public record PubSubMessage(
            String messageId,
            String data,
            Map<String, String> attributes
    ) {}
}
