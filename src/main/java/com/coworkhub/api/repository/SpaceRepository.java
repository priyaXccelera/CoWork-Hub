package com.coworkhub.api.repository;

import com.coworkhub.api.entity.Space;
import com.coworkhub.api.entity.SpaceType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpaceRepository extends JpaRepository<Space, Long> {

  Optional<Space> findByIdAndDeletedFalse(Long id);

  Page<Space> findByDeletedFalse(Pageable pageable);

  Page<Space> findByDeletedFalseAndType(SpaceType type, Pageable pageable);

  List<Space> findByDeletedFalseAndType(SpaceType type);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Space s where s.id = :id")
  Optional<Space> findByIdForUpdate(@Param("id") Long id);
}
