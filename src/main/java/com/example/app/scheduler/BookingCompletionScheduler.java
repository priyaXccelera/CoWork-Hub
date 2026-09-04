package com.example.app.scheduler;

import com.example.app.repository.BookingRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodically transitions CONFIRMED bookings whose endTime has already passed into COMPLETED, so
 * that reporting (utilization, revenue, top members) and invoicing reflect reality.
 */
@Component
public class BookingCompletionScheduler {

  private static final Logger log = LoggerFactory.getLogger(BookingCompletionScheduler.class);

  private final BookingRepository bookingRepository;

  public BookingCompletionScheduler(BookingRepository bookingRepository) {
    this.bookingRepository = bookingRepository;
  }

  @Scheduled(fixedRate = 60_000, initialDelay = 15_000)
  @Transactional
  public void completePastBookings() {
    int updated = bookingRepository.completePastBookings(LocalDateTime.now());
    if (updated > 0) {
      log.info("Marked {} booking(s) as COMPLETED after their end time passed", updated);
    }
  }
}
