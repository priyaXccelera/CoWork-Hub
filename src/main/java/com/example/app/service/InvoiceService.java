package com.example.app.service;

import com.example.app.dto.InvoiceDtos.InvoiceGenerateRequest;
import com.example.app.dto.InvoiceDtos.InvoiceResponse;
import com.example.app.entity.Booking;
import com.example.app.entity.BookingStatus;
import com.example.app.entity.Invoice;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.repository.BookingRepository;
import com.example.app.repository.InvoiceRepository;
import com.example.app.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

  private final InvoiceRepository invoiceRepository;
  private final BookingRepository bookingRepository;
  private final UserRepository userRepository;

  public InvoiceService(
      InvoiceRepository invoiceRepository,
      BookingRepository bookingRepository,
      UserRepository userRepository) {
    this.invoiceRepository = invoiceRepository;
    this.bookingRepository = bookingRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public InvoiceResponse generate(InvoiceGenerateRequest request) {
    if (!userRepository.existsById(request.getUserId())) {
      throw new ResourceNotFoundException("User not found with id: " + request.getUserId());
    }

    YearMonth yearMonth =
        YearMonth.parse(request.getMonth(), DateTimeFormatter.ofPattern("yyyy-MM"));
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
    return toResponse(saved);
  }

  public Page<InvoiceResponse> list(
      Long userId, boolean isAdmin, Long actorUserId, Pageable pageable) {
    Long effectiveUserId = isAdmin ? userId : actorUserId;
    if (effectiveUserId == null) {
      return invoiceRepository.findAll(pageable).map(this::toResponse);
    }
    return invoiceRepository.findByUserId(effectiveUserId, pageable).map(this::toResponse);
  }

  public InvoiceResponse get(Long id) {
    Invoice invoice =
        invoiceRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    return toResponse(invoice);
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
