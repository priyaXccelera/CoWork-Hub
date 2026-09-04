package com.example.app.repository;

import com.example.app.entity.Waitlist;
import com.example.app.entity.WaitlistStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

  Page<Waitlist> findByUserId(Long userId, Pageable pageable);

  List<Waitlist> findBySpaceIdAndStatusOrderByCreatedAtAsc(Long spaceId, WaitlistStatus status);

  Optional<Waitlist> findFirstBySpaceIdAndStatusOrderByCreatedAtAsc(
      Long spaceId, WaitlistStatus status);
}
