package com.example.app.controller;

import com.example.app.dto.InvoiceDtos.InvoiceGenerateRequest;
import com.example.app.dto.InvoiceDtos.InvoiceResponse;
import com.example.app.security.AccessGuard;
import com.example.app.security.CurrentActor;
import com.example.app.service.InvoiceService;
import com.example.app.util.OffsetPageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoices", description = "Monthly invoice generation and listing")
public class InvoiceController {

  private final InvoiceService invoiceService;

  public InvoiceController(InvoiceService invoiceService) {
    this.invoiceService = invoiceService;
  }

  @PostMapping("/generate")
  @Operation(summary = "Generate/regenerate a monthly invoice for a user (Admin only)")
  public ResponseEntity<InvoiceResponse> generate(
      @Valid @RequestBody InvoiceGenerateRequest request) {
    AccessGuard.requireAdmin();
    return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.generate(request));
  }

  @GetMapping
  @Operation(summary = "List invoices (own for Members, filterable by userId for Admin)")
  public ResponseEntity<Page<InvoiceResponse>> list(
      @RequestParam(required = false) Long userId,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit) {
    boolean isAdmin = CurrentActor.isAdmin();
    Long actorUserId =
        isAdmin ? CurrentActor.currentUserId() : AccessGuard.requireAuthenticatedUserId();
    return ResponseEntity.ok(
        invoiceService.list(
            userId,
            isAdmin,
            actorUserId,
            OffsetPageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "id"))));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get an invoice by id")
  public ResponseEntity<InvoiceResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(invoiceService.get(id));
  }
}
