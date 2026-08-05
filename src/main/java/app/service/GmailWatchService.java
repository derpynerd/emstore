package app.service;

import app.constants.Constants;
import app.repository.GmailStateRepository;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class GmailWatchService {

    private final Gmail gmail;
    private final GmailStateRepository stateRepository;

    @Autowired
    public GmailWatchService(Gmail gmail, GmailStateRepository stateRepository) {
        this.gmail = gmail;
        this.stateRepository = stateRepository;
    }

    public void startWatch() throws IOException {
        WatchRequest request = new WatchRequest()
                .setTopicName(Constants.TOPIC_NAME)
                .setLabelIds(List.of("INBOX"));

        WatchResponse response = gmail.users()
                .watch(Constants.USER, request)
                .execute();

        stateRepository.saveHistoryId(
                response.getHistoryId().longValue()
        );

        log.info("Watch started! :: HistoryId: {}, Expiration: {}", response.getHistoryId(), response.getExpiration());
    }

}
