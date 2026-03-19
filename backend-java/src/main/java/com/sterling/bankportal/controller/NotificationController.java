package com.sterling.bankportal.controller;

import com.sterling.bankportal.model.Notification;
import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.NotificationRepository;
import com.sterling.bankportal.repo.UserRepository;
import com.sterling.bankportal.util.ApiResponse;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController extends BaseController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Object> list(Principal principal) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }
        List<Map<String, Object>> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::payload)
                .toList();
        Map<String, Object> response = ApiResponse.success();
        response.put("notifications", notifications);
        response.put("unread_count", notificationRepository.countByUserIdAndReadFalse(user.getId()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{notificationId}/read")
    @Transactional
    public ResponseEntity<Object> markRead(Principal principal, @PathVariable String notificationId) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.getId()).orElse(null);
        if (notification == null) {
            return error(HttpStatus.NOT_FOUND, "Notification not found");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(ApiResponse.successMessage("Notification marked as read"));
    }

    @PostMapping("/read-all")
    @Transactional
    public ResponseEntity<Object> markAllRead(Principal principal) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok(ApiResponse.successMessage("All notifications marked as read"));
    }

    private Map<String, Object> payload(Notification notification) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", notification.getId());
        data.put("type", notification.getType());
        data.put("title", notification.getTitle());
        data.put("message", notification.getMessage());
        data.put("is_read", notification.isRead());
        data.put("created_at", notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : null);
        return data;
    }
}
