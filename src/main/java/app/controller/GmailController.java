package app.controller;

import app.dto.GmailNotification;
import app.dto.PubSubPushRequest;
import app.service.GmailWatchService;
import app.service.WebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/gmail")
public class GmailController {

    private final ObjectMapper objectMapper;
    private final WebhookService webhookService;
    private final GmailWatchService watchService;

    @Autowired
    public GmailController(ObjectMapper objectMapper, GmailWatchService watchService, WebhookService webhookService) {
        this.objectMapper = objectMapper;
        this.watchService = watchService;
        this.webhookService = webhookService;
    }

    @PostMapping("/watch")
    public ResponseEntity<String> watch() throws IOException {
        watchService.startWatch();
        return ResponseEntity.ok("Watch started");
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveMessage(
            @RequestBody PubSubPushRequest request
    ) {
        try {
            if (request.message() == null || request.message().data() == null) {
                return ResponseEntity.badRequest().build();
            }

            String decodedJson = new String(
                    Base64.getDecoder().decode(request.message().data()),
                    StandardCharsets.UTF_8);

            GmailNotification notification =
                    objectMapper.readValue(decodedJson, GmailNotification.class);

            System.out.println("Email: " + notification.emailAddress());
            System.out.println("History ID: " + notification.historyId());

            // Business logic
            webhookService.processMessage(notification);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            System.out.println("Caught Exception in WebhookController: " + e);
            return ResponseEntity.internalServerError().build();
        }
    }

}
