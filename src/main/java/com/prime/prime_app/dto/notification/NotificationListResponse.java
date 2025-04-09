package com.prime.prime_app.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponse {
    private List<NotificationDto> notifications;
    private int unreadCount;
} 