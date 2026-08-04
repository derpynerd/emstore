package app.service;

import app.constants.Constants;
import app.dto.GmailNotification;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;

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
