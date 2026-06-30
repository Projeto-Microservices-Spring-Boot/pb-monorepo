package com.ecommerce.community.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class LoggingNotificationServiceClient implements NotificationServiceClient {

    @Override
    public void sendNotification(String userId, String message) {
        log.info("Notification to userId={}: {}", userId, message);
    }
}
