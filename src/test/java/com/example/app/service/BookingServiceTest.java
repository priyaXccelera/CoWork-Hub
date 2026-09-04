package com.example.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.example.app.dto.BookingDtos.BookingRequest;
import com.example.app.dto.BookingDtos.BookingResponse;
import com.example.app.entity.Booking;
import com.example.app.entity.BookingStatus;
import com.example.app.entity.MembershipPlan;
import com.example.app.entity.Space;
import com.example.app.entity.SpaceType;
import com.example.app.entity.User;
import com.example.app.exception.ConflictException;
import com.example.app.repository.BookingRepository;
import com.example.app.repository.SpaceRepository;
import com.example.app.repository.UserRepository;
import com.example.app.repository.WaitlistRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

  @Mock private BookingRepository bookingRepository;
  @Mock private SpaceRepository spaceRepository;
  @Mock private UserRepository userRepository;
  @Mock private WaitlistRepository waitlistRepository;

  private BookingService bookingService;

  @BeforeEach
  void setUp() {
    bookingService =
        new BookingService(bookingRepository, spaceRepository, userRepository, waitlistRepository);
  }

  private Space desk(int capacity) {
    Space space = new Space();
    space.setId(1L);
    space.setName("Hot Desk A1");
    space.setType(SpaceType.DESK);
    space.setCapacity(capacity);
    space.setHourlyRate(new BigDecimal("5.00"));
    space.setActive(true);
    space.setDeleted(false);
    return space;
  }

  private User memberWithPlan(double creditHours) {
    User user = new User();
    user.setId(2L);
    user.setName("Bob Member");
    user.setEmail("bob@example.com");
    MembershipPlan plan = new MembershipPlan();
    plan.setId(1L);
    plan.setOverageRatePerHour(new BigDecimal("5.00"));
    plan.setIncludedCreditHours(10.0);
    user.setMembershipPlan(plan);
    user.setCreditHoursRemaining(creditHours);
    return user;
  }

  @Test
  void createBooking_rejectsWithConflictWhenSpaceAtCapacity() {
    Space space = desk(1);
    when(spaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(space));
    when(userRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(memberWithPlan(10.0)));

    Booking existing = new Booking();
    existing.setId(99L);
    when(bookingRepository.findOverlappingConfirmed(any(), any(), any()))
        .thenReturn(List.of(existing));

    BookingRequest request = new BookingRequest();
    request.setSpaceId(1L);
    LocalDateTime start = LocalDateTime.now().plusDays(1);
    request.setStartTime(start);
    request.setEndTime(start.plusHours(2));

    assertThatThrownBy(() -> bookingService.createBooking(request, 2L, false))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void createBooking_allowsBookingWhenUnderCapacity() {
    Space space = desk(2);
    when(spaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(space));
    User user = memberWithPlan(10.0);
    when(userRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(user));

    Booking existing = new Booking();
    existing.setId(99L);
    when(bookingRepository.findOverlappingConfirmed(any(), any(), any()))
        .thenReturn(List.of(existing));
    when(bookingRepository.save(any(Booking.class)))
        .thenAnswer(
            invocation -> {
              Booking b = invocation.getArgument(0);
              b.setId(100L);
              return b;
            });

    BookingRequest request = new BookingRequest();
    request.setSpaceId(1L);
    LocalDateTime start = LocalDateTime.now().plusDays(1);
    request.setStartTime(start);
    request.setEndTime(start.plusHours(2));

    BookingResponse response = bookingService.createBooking(request, 2L, false);

    assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    // 2 hours fully covered by membership credit hours -> $0 cash cost.
    assertThat(response.getCostCharged()).isEqualByComparingTo("0.00");
    // But the original market value of the booking is preserved regardless of payment method.
    assertThat(response.getOriginalCost()).isEqualByComparingTo("10.00");
  }

  @Test
  void cancelBooking_lateCancellation_feeIsBasedOnOriginalCostEvenWhenPaidWithCredit() {
    Booking booking = new Booking();
    booking.setId(5L);
    booking.setUserId(2L);
    booking.setSpaceId(1L);
    booking.setStatus(BookingStatus.CONFIRMED);
    // Fully paid via credit hours: cash cost is $0, but the booking was worth $10.
    booking.setCostCharged(BigDecimal.ZERO);
    booking.setOriginalCost(new BigDecimal("10.00"));
    booking.setCreditHoursUsed(2.0);
    // Starting in 30 minutes: within the 2-hour free-cancellation window, so a fee applies.
    booking.setStartTime(LocalDateTime.now().plusMinutes(30));
    booking.setEndTime(LocalDateTime.now().plusMinutes(150));

    when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
    User user = memberWithPlan(0.0);
    when(userRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(user));
    when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    when(waitlistRepository.findBySpaceIdAndStatusOrderByCreatedAtAsc(anyLong(), any()))
        .thenReturn(Collections.emptyList());

    BookingResponse response = bookingService.cancelBooking(5L, 2L, false);

    assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    // 25% of the $10 original value = $2.50, NOT 25% of the $0 cash amount.
    assertThat(response.getCostCharged()).isEqualByComparingTo("2.50");
    // Credit hours used for the booking are refunded since the space was released.
    assertThat(user.getCreditHoursRemaining()).isEqualTo(2.0);
  }

  @Test
  void cancelBooking_freeCancellation_refundsCreditHoursAndChargesNothing() {
    Booking booking = new Booking();
    booking.setId(6L);
    booking.setUserId(2L);
    booking.setSpaceId(1L);
    booking.setStatus(BookingStatus.CONFIRMED);
    booking.setCostCharged(BigDecimal.ZERO);
    booking.setOriginalCost(new BigDecimal("10.00"));
    booking.setCreditHoursUsed(2.0);
    // Starting in 3 days: well outside the 2-hour cancellation window.
    booking.setStartTime(LocalDateTime.now().plusDays(3));
    booking.setEndTime(LocalDateTime.now().plusDays(3).plusHours(2));

    when(bookingRepository.findById(6L)).thenReturn(Optional.of(booking));
    User user = memberWithPlan(0.0);
    when(userRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(user));
    when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    when(waitlistRepository.findBySpaceIdAndStatusOrderByCreatedAtAsc(anyLong(), any()))
        .thenReturn(Collections.emptyList());

    BookingResponse response = bookingService.cancelBooking(6L, 2L, false);

    assertThat(response.getCostCharged()).isEqualByComparingTo("0.00");
    assertThat(user.getCreditHoursRemaining()).isEqualTo(2.0);
  }
}
