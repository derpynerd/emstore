package app.service;

import app.constants.Constants;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;


@Service
public class GmailWatchService {

    private final Gmail gmail;
    private final historyState historyState;

    @Autowired
    public GmailWatchService(Gmail gmail, historyState historyState) {
        this.gmail = gmail;
        this.historyState = historyState;
    }

    public void startWatch() throws IOException {
        WatchRequest request = new WatchRequest()
                .setTopicName(Constants.TOPIC_NAME)
                .setLabelIds(List.of("INBOX"));

        WatchResponse response = gmail.users()
                .watch(Constants.USER, request)
                .execute();

        historyState.setLastHistoryId(response.getHistoryId());

        System.out.println("Watch started!");
        System.out.println("HistoryId: " + response.getHistoryId());
        System.out.println("Expiration: " + response.getExpiration());
    }

}
