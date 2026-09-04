package com.coworkhub.api.repository;

import com.coworkhub.api.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByIdAndDeletedFalse(Long id);

  Page<User> findByDeletedFalse(Pageable pageable);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByMembershipPlan_IdAndDeletedFalse(Long membershipPlanId);
}
