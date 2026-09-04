package com.coworkhub.api.service;

import com.coworkhub.api.dto.NotificationDtos.NotificationResponse;
import com.coworkhub.api.entity.Notification;
import com.coworkhub.api.entity.NotificationType;
import com.coworkhub.api.exception.ForbiddenException;
import com.coworkhub.api.exception.ResourceNotFoundException;
import com.coworkhub.api.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
  private final NotificationRepository notificationRepository;
  private final TransactionTemplate notificationTransaction;

  public NotificationService(
      NotificationRepository notificationRepository, PlatformTransactionManager transactionManager) {
    this.notificationRepository = notificationRepository;
    this.notificationTransaction = new TransactionTemplate(transactionManager);
    this.notificationTransaction.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
  }

  /**
   * Creates an in-app notification in a separate transaction. Failures are intentionally swallowed so
   * notification persistence can never block or roll back the action that caused it.
   */
  public void createSafely(
      Long userId,
      NotificationType type,
      String title,
      String message,
      String referenceType,
      Long referenceId) {
    try {
      notificationTransaction.executeWithoutResult(
          status -> {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setReferenceType(referenceType);
            notification.setReferenceId(referenceId);
            notificationRepository.save(notification);
          });
    } catch (RuntimeException exception) {
      log.warn("Unable to create {} notification for user {}", type, userId, exception);
    }
  }

  @Transactional(readOnly = true)
  public Page<NotificationResponse> list(Long userId, Boolean isRead, Pageable pageable) {
    Page<Notification> notifications =
        isRead == null
            ? notificationRepository.findByUserId(userId, pageable)
            : notificationRepository.findByUserIdAndRead(userId, isRead, pageable);
    return notifications.map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public long unreadCount(Long userId) {
    return notificationRepository.countByUserIdAndReadFalse(userId);
  }

  @Transactional
  public NotificationResponse markRead(Long id, Long actorUserId) {
    Notification notification = findOwned(id, actorUserId);
    notification.setRead(true);
    return toResponse(notificationRepository.save(notification));
  }

  @Transactional
  public void markAllRead(Long actorUserId) {
    notificationRepository.findByUserIdAndRead(actorUserId, false, Pageable.unpaged())
        .forEach(notification -> notification.setRead(true));
  }

  private Notification findOwned(Long id, Long actorUserId) {
    Notification notification =
        notificationRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
    if (!notification.getUserId().equals(actorUserId)) {
      throw new ForbiddenException("You are not allowed to access this notification");
    }
    return notification;
  }

  private NotificationResponse toResponse(Notification notification) {
    NotificationResponse response = new NotificationResponse();
    response.setId(notification.getId());
    response.setUserId(notification.getUserId());
    response.setType(notification.getType());
    response.setTitle(notification.getTitle());
    response.setMessage(notification.getMessage());
    response.setReferenceType(notification.getReferenceType());
    response.setReferenceId(notification.getReferenceId());
    response.setRead(notification.isRead());
    response.setCreatedAt(notification.getCreatedAt());
    return response;
  }
}
