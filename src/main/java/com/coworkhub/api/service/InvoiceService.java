package com.coworkhub.api.service;

import com.coworkhub.api.dto.InvoiceDtos.InvoiceGenerateRequest;
import com.coworkhub.api.dto.InvoiceDtos.InvoiceResponse;
import com.coworkhub.api.entity.Booking;
import com.coworkhub.api.entity.BookingStatus;
import com.coworkhub.api.entity.Invoice;
import com.coworkhub.api.entity.NotificationType;
import com.coworkhub.api.exception.BusinessRuleException;
import com.coworkhub.api.exception.ForbiddenException;
import com.coworkhub.api.exception.ResourceNotFoundException;
import com.coworkhub.api.repository.BookingRepository;
import com.coworkhub.api.repository.InvoiceRepository;
import com.coworkhub.api.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InvoiceService {

  private static final int MIN_YEAR = 2000;
  private static final int MAX_YEAR = 2100;

  private final InvoiceRepository invoiceRepository;
  private final BookingRepository bookingRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  public InvoiceService(
      InvoiceRepository invoiceRepository,
      BookingRepository bookingRepository,
      UserRepository userRepository,
      NotificationService notificationService) {
    this.invoiceRepository = invoiceRepository;
    this.bookingRepository = bookingRepository;
    this.userRepository = userRepository;
    this.notificationService = notificationService;
  }

  public InvoiceResponse generate(InvoiceGenerateRequest request) {
    if (userRepository.findByIdAndDeletedFalse(request.getUserId()).isEmpty()) {
      throw new ResourceNotFoundException(
          "User not found (or has been deleted) with id: " + request.getUserId());
    }

    YearMonth yearMonth = parseMonth(request.getMonth());
    LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
    LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

    List<Booking> bookings =
        bookingRepository.findByUserId(request.getUserId()).stream()
            .filter(b -> !b.getStartTime().isBefore(start) && b.getStartTime().isBefore(end))
            .toList();

    BigDecimal overageCharges =
        bookings.stream()
            .filter(
                b ->
                    b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.COMPLETED)
            .map(Booking::getCostCharged)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal cancellationFees =
        bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
            .map(Booking::getCostCharged)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalAmount = overageCharges.add(cancellationFees).setScale(2, RoundingMode.HALF_UP);

    Invoice invoice =
        invoiceRepository
            .findByUserIdAndMonth(request.getUserId(), request.getMonth())
            .orElseGet(Invoice::new);
    invoice.setUserId(request.getUserId());
    invoice.setMonth(request.getMonth());
    invoice.setTotalAmount(totalAmount);
    invoice.setTotalCreditOverageCharged(overageCharges.setScale(2, RoundingMode.HALF_UP));

    Invoice saved = invoiceRepository.save(invoice);
    notificationService.createSafely(
        saved.getUserId(),
        NotificationType.INVOICE_GENERATED,
        "Monthly invoice generated",
        "Your invoice for " + saved.getMonth() + " has been generated.",
        "INVOICE",
        saved.getId());
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public Page<InvoiceResponse> list(
      Long userId, boolean isAdmin, Long actorUserId, Pageable pageable) {
    Long effectiveUserId = isAdmin ? userId : actorUserId;
    if (effectiveUserId == null) {
      return invoiceRepository.findAll(pageable).map(this::toResponse);
    }
    return invoiceRepository.findByUserId(effectiveUserId, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public InvoiceResponse get(Long id, Long actorUserId, boolean isAdmin) {
    Invoice invoice =
        invoiceRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    if (!isAdmin && !invoice.getUserId().equals(actorUserId)) {
      throw new ForbiddenException("You are not allowed to access this invoice");
    }
    return toResponse(invoice);
  }

  private YearMonth parseMonth(String month) {
    if (month == null || !month.matches("\\d{4}-\\d{2}")) {
      throw new BusinessRuleException("month must be in YYYY-MM format, e.g. 2024-05");
    }
    YearMonth yearMonth;
    try {
      yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
    } catch (DateTimeParseException e) {
      throw new BusinessRuleException("Invalid month value: " + month);
    }
    if (yearMonth.getYear() < MIN_YEAR || yearMonth.getYear() > MAX_YEAR) {
      throw new BusinessRuleException(
          "month year must be between " + MIN_YEAR + " and " + MAX_YEAR);
    }
    return yearMonth;
  }

  private InvoiceResponse toResponse(Invoice invoice) {
    InvoiceResponse response = new InvoiceResponse();
    response.setId(invoice.getId());
    response.setUserId(invoice.getUserId());
    response.setMonth(invoice.getMonth());
    response.setTotalAmount(invoice.getTotalAmount());
    response.setTotalCreditOverageCharged(invoice.getTotalCreditOverageCharged());
    response.setCreatedAt(invoice.getCreatedAt());
    return response;
  }
}
