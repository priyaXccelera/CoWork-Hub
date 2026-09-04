package com.example.app.repository;

import com.example.app.entity.Invoice;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

  Page<Invoice> findByUserId(Long userId, Pageable pageable);

  Optional<Invoice> findByUserIdAndMonth(Long userId, String month);
}
