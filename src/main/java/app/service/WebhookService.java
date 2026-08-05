package app.service;

import app.constants.Constants;
import app.dto.GmailNotification;
import app.repository.GmailNotificationRepository;
import app.repository.GmailStateRepository;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public WebhookService(Gmail gmail, GmailStateRepository stateRepository, GmailNotificationRepository notificationRepository) {
        this.gmail = gmail;
        this.stateRepository = stateRepository;
        this.notificationRepository = notificationRepository;
    }

    private record EmailData(String date, String sender, String subject) {}

    public void processMessage(GmailNotification notification) throws IOException {

        BigInteger historyId = stateRepository.getHistoryId() != null ?
                BigInteger.valueOf(stateRepository.getHistoryId()) : notification.historyId();
        log.info("HistoryID: {}", historyId);

        ListHistoryResponse historyResponse = gmail.users()
                .history()
                .list(Constants.USER)
                .setStartHistoryId(historyId)
                .execute();

        List<History> historyList = historyResponse.getHistory();

        if (historyList == null || historyList.isEmpty()) {
            log.warn("No history records returned.");
            return;
        }

        for (History history : historyResponse.getHistory()) {

            if (history.getMessagesAdded() != null) {

                for (HistoryMessageAdded added : history.getMessagesAdded()) {

                    if (messageLabelsDontIncludeInbox(added)) continue;

                    String messageId = added.getMessage().getId();
                    String threadId = added.getMessage().getThreadId();

                    log.info("Processing: {}", messageId);
                    Message message = getMessage(messageId);
                    if (message == null) continue;

                    EmailData emailData = extractEmailData(message);

                    saveDataInDatabase(history, messageId, threadId, emailData);
                    log.info("Saved notification: {}", messageId);
                }
            }
        }

        log.info("Updating HistoryID: {}", historyResponse.getHistory());
        stateRepository.saveHistoryId(
                historyResponse.getHistoryId().longValue()
        );
    }

    private void saveDataInDatabase(History history, String messageId, String threadId, EmailData emailData) {
        Long historyRecordId = Long.parseLong(history.getId().toString());
        notificationRepository.saveEmail(
                messageId,
                threadId,
                emailData.subject(),
                emailData.sender(),
                emailData.date(),
                historyRecordId
        );
    }

    private static @NonNull EmailData extractEmailData(Message message) {
        String date = "";
        String sender = "";
        String subject = "";
        for (MessagePartHeader header : message.getPayload().getHeaders()) {
            switch (header.getName()) {
                case "Subject" -> subject = header.getValue();
                case "From" -> sender = header.getValue();
                case "Date" -> date = header.getValue();
            }
        }
        return new EmailData(date, sender, subject);
    }

    private @Nullable Message getMessage(String messageId) throws IOException {
        Message message = null;
        try {
            message = gmail.users()
                    .messages()
                    .get(Constants.USER, messageId)
                    .setFormat("metadata")
                    .setMetadataHeaders(List.of("Subject", "From", "Date"))
                    .execute();

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.warn("Message {} no longer exists. Skipping.", messageId);
                return null;
            }
            throw e;
        }
        return message;
    }

    private static boolean messageLabelsDontIncludeInbox(HistoryMessageAdded added) {
        Message historyMessage = added.getMessage();

        List<String> labels = historyMessage.getLabelIds();

        return labels == null || !labels.contains("INBOX");
    }

}
