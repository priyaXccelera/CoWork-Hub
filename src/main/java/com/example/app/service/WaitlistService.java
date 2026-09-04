package com.example.app.service;

import com.example.app.dto.WaitlistDtos.WaitlistResponse;
import com.example.app.entity.Waitlist;
import com.example.app.repository.WaitlistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class WaitlistService {

  private final WaitlistRepository waitlistRepository;

  public WaitlistService(WaitlistRepository waitlistRepository) {
    this.waitlistRepository = waitlistRepository;
  }

  public Page<WaitlistResponse> list(Long actorUserId, boolean isAdmin, Pageable pageable) {
    if (isAdmin) {
      return waitlistRepository.findAll(pageable).map(this::toResponse);
    }
    return waitlistRepository.findByUserId(actorUserId, pageable).map(this::toResponse);
  }

  private WaitlistResponse toResponse(Waitlist waitlist) {
    WaitlistResponse response = new WaitlistResponse();
    response.setId(waitlist.getId());
    response.setUserId(waitlist.getUserId());
    response.setSpaceId(waitlist.getSpaceId());
    response.setRequestedStart(waitlist.getRequestedStart());
    response.setRequestedEnd(waitlist.getRequestedEnd());
    response.setStatus(waitlist.getStatus());
    response.setBookingId(waitlist.getBookingId());
    response.setCreatedAt(waitlist.getCreatedAt());
    response.setUpdatedAt(waitlist.getUpdatedAt());
    return response;
  }
}
