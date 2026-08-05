package app.dto;

import java.math.BigInteger;

public record GmailNotification(
        String emailAddress,
        BigInteger historyId
) {}
