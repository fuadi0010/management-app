package com.app.management.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.management.model.sales.SalesInvoice;
import com.app.management.model.sales.SalesStatus;

public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, Long> {

    List<SalesInvoice> findBySalesStatusAndInvoiceDateBetween(
            SalesStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
}
