package app.service;

import app.constants.Constants;
import app.dto.GmailNotification;
import app.repository.GmailNotificationRepository;
import app.repository.GmailStateRepository;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

@Slf4j
@Service
public class WebhookService {

    private final Gmail gmail;
    private final GmailStateRepository stateRepository;
    private final GmailNotificationRepository notificationRepository;

    public WebhookService(
            Gmail gmail,
            GmailStateRepository stateRepository,
            GmailNotificationRepository notificationRepository) {

        this.gmail = gmail;
        this.stateRepository = stateRepository;
        this.notificationRepository = notificationRepository;
    }

    private record EmailData(
            String date,
            String sender,
            String subject) {
    }

    public void processMessage(GmailNotification notification) throws IOException {

        BigInteger startHistoryId = getStartHistoryId(notification);

        log.info("Fetching history from {}", startHistoryId);

        ListHistoryResponse historyResponse = gmail.users()
                .history()
                .list(Constants.USER)
                .setStartHistoryId(startHistoryId)
                .execute();

        List<History> histories = historyResponse.getHistory();

        if (histories == null || histories.isEmpty()) {
            log.info("No new history records.");
            return;
        }

        for (History history : histories) {
            processHistory(history);
        }

        updateStoredHistoryId(historyResponse.getHistoryId());
    }

    private BigInteger getStartHistoryId(GmailNotification notification) {
        Long storedHistoryId = stateRepository.getHistoryId();

        return storedHistoryId != null
                ? BigInteger.valueOf(storedHistoryId)
                : notification.historyId();
    }

    private void processHistory(History history) throws IOException {

        if (history.getMessagesAdded() == null) {
            return;
        }

        for (HistoryMessageAdded added : history.getMessagesAdded()) {
            processAddedMessage(history, added);
        }
    }

    private void processAddedMessage(
            History history,
            HistoryMessageAdded added) throws IOException {

        if (!isInboxMessage(added)) {
            return;
        }

        Message historyMessage = added.getMessage();

        String messageId = historyMessage.getId();
        String threadId = historyMessage.getThreadId();

        log.info("Processing message {}", messageId);

        Message message = fetchMessage(messageId);

        if (message == null) {
            return;
        }

        EmailData emailData = extractEmailData(message);

        notificationRepository.saveEmail(
                messageId,
                threadId,
                emailData.subject(),
                emailData.sender(),
                emailData.date(),
                history.getId().longValue()
        );

        log.info("Saved notification {}", messageId);
    }

    private Message fetchMessage(String messageId) throws IOException {

        try {
            return gmail.users()
                    .messages()
                    .get(Constants.USER, messageId)
                    .setFormat("metadata")
                    .setMetadataHeaders(List.of("Subject", "From", "Date"))
                    .execute();

        } catch (GoogleJsonResponseException e) {

            if (e.getStatusCode() == 404) {
                log.warn("Message {} no longer exists.", messageId);
                return null;
            }

            throw e;
        }
    }

    private EmailData extractEmailData(Message message) {

        String subject = "";
        String sender = "";
        String date = "";

        for (MessagePartHeader header : message.getPayload().getHeaders()) {

            switch (header.getName()) {
                case "Subject" -> subject = header.getValue();
                case "From" -> sender = header.getValue();
                case "Date" -> date = header.getValue();
            }
        }

        return new EmailData(date, sender, subject);
    }

    private boolean isInboxMessage(HistoryMessageAdded added) {

        List<String> labels = added.getMessage().getLabelIds();

        return labels != null && labels.contains("INBOX");
    }

    private void updateStoredHistoryId(BigInteger historyId) {

        log.info("Updating historyId to {}", historyId);

        stateRepository.saveHistoryId(historyId.longValue());
    }
}