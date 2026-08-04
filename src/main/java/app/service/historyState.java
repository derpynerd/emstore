package app.service;

import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
public class historyState {

    private BigInteger lastHistoryId;

    public BigInteger getLastHistoryId() {
        return lastHistoryId;
    }

    public void setLastHistoryId(BigInteger historyId) {
        this.lastHistoryId = historyId;
    }

}
