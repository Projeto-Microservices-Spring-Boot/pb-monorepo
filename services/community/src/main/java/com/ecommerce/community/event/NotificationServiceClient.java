package com.ecommerce.community.event;


public interface NotificationServiceClient {

    void sendNotification(String userId, String message);
}
