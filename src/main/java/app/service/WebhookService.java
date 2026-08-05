package app.service;

import app.constants.Constants;
import app.dto.GmailNotification;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;

@Slf4j
@Service
public class WebhookService {

    private final Gmail gmail;
    private final historyState historyState;

    @Autowired
    public WebhookService(Gmail gmail, historyState historyState) {
        this.gmail = gmail;
        this.historyState = historyState;
    }

    public void processMessage(GmailNotification notification) throws IOException {
        System.out.println("Processing: " + notification);

        BigInteger startHistoryId = historyState.getLastHistoryId();

        ListHistoryResponse historyResponse = gmail.users()
                .history()
                .list(Constants.USER)
                .setStartHistoryId(startHistoryId)
                .execute();

        // Process history ...
        for (History history : historyResponse.getHistory()) {
            if (history.getMessagesAdded() != null) {
                for (HistoryMessageAdded added : history.getMessagesAdded()) {
                    String messageId = added.getMessage().getId();

                    Message message = gmail.users()
                            .messages()
                            .get(Constants.USER, messageId)
                            .execute();

                    // TODO: Process the email
                }
            }
        }

        historyState.setLastHistoryId(notification.historyId());
    }

}
