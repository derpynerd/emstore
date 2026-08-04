package app;

import app.auth.GmailClient;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

public class Main {

    private static final String USER = "me";

    public static void main(String... args) throws IOException, GeneralSecurityException {

        GmailClient gmailClient = new GmailClient();
        Gmail service = gmailClient.getGmailService();

        ListMessagesResponse listResponse = service.users().messages().list(USER).execute();
        List<Message> messages = listResponse.getMessages();
        if (messages == null) {
            return;
        }

        int count = messages.size();
        System.out.println("Message Count: " + count);

        for (Message message : messages) {
            Message fullMessage = service.users().messages().get(USER, message.getId()).execute();
            if (fullMessage.getPayload() != null && fullMessage.getPayload().getHeaders() != null) {
                Optional<String> subjectLine = fullMessage.getPayload().getHeaders().stream()
                        .filter(header -> header.getName().equalsIgnoreCase("Subject"))
                        .map(MessagePartHeader::getValue)
                        .findFirst();

                subjectLine.ifPresent(s -> System.out.printf("- %s\n", s));
            }

        }
    }

}
