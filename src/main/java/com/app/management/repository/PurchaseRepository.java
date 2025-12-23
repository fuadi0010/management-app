package com.app.management.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.management.model.purchase.Purchase;
import com.app.management.model.purchase.PurchaseStatus;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    boolean existsByReferenceNumber(String referenceNumber);

    List<Purchase> findByStatusAndPurchaseDateBetween(
            PurchaseStatus status,
            LocalDateTime start,
            LocalDateTime end);
}
