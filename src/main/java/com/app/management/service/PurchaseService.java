package com.app.management.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.app.management.model.product.Product;
import com.app.management.model.purchase.Purchase;
import com.app.management.model.purchase.PurchaseDetails;
import com.app.management.model.purchase.PurchaseStatus;
import com.app.management.repository.PurchaseRepository;
import jakarta.transaction.Transactional;

@Service
public class PurchaseService {
    @Autowired
    private PurchaseRepository purchaseRepository;

    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAll();
    }

    public Purchase getPurchaseByid(Long id) {
        return purchaseRepository.findById(id).orElse(null);
    }

    @Transactional
    public Purchase createPurchase(Purchase purchase) {

        if (purchaseRepository.existsByReferenceNumber(purchase.getReferenceNumber())) {
            throw new IllegalArgumentException(
                    "No. Referensi sudah digunakan, silakan gunakan yang lain");
        }

        purchase.setStatus(PurchaseStatus.CREATED);

        purchase.getPurchaseDetails()
                .removeIf(d -> d.getProduct() == null || d.getQuantity() == null || d.getQuantity() <= 0);

        for (PurchaseDetails d : purchase.getPurchaseDetails()) {
            d.setPurchase(purchase);
            d.setSubtotal(
                    d.getUnitPurchasePrice()
                            .multiply(BigDecimal.valueOf(d.getQuantity())));
        }

        return purchaseRepository.save(purchase);
    }

    @Transactional
    public boolean completePurchase(Long id) {
        Purchase purchase = getPurchaseByid(id);

        if (purchase.getStatus() != PurchaseStatus.CREATED) {
            throw new IllegalStateException("Invalid state transition");
        }

        for (PurchaseDetails detail : purchase.getPurchaseDetails()) {
            Product product = detail.getProduct();

            detail.setPurchasePriceBefore(product.getLastPurchasePrice());

            product.setCurrentStock(product.getCurrentStock() + detail.getQuantity());
            product.setLastPurchasePrice(detail.getUnitPurchasePrice());
        }
        purchase.setStatus(PurchaseStatus.COMPLETED);
        return true;
    }

    @Transactional
    public boolean cancelPurchase(Long id) {

        Purchase purchase = getPurchaseByid(id);

        if (purchase.getStatus() == PurchaseStatus.CANCELLED) {
            throw new IllegalStateException("Purchase already cancelled");
        }

        if (purchase.getStatus() != PurchaseStatus.CREATED) {
            throw new IllegalStateException(
                    "Only purchase with status CREATED can be cancelled");
        }

        for (PurchaseDetails detail : purchase.getPurchaseDetails()) {

            Product product = detail.getProduct();

            // rollback stock
            product.setCurrentStock(
                    product.getCurrentStock() - detail.getQuantity());

            // 🔐 SAFE rollback price
            if (detail.getPurchasePriceBefore() != null) {
                product.setLastPurchasePrice(detail.getPurchasePriceBefore());
            }
            // else: BIARKAN harga terakhir tetap (jangan di-null-kan)
        }

        purchase.setStatus(PurchaseStatus.CANCELLED);
        return true;
    }

    public List<Purchase> searchAndSort(String keyword, String sort) {

        Sort sortOrder;

        switch (sort) {
            case "date_asc":
                sortOrder = Sort.by("purchaseDate").ascending();
                break;
            case "total_desc":
                sortOrder = Sort.by("totalPurchase").descending();
                break;
            case "total_asc":
                sortOrder = Sort.by("totalPurchase").ascending();
                break;
            default:
                sortOrder = Sort.by("purchaseDate").descending(); // date_desc (default)
        }

        // 🔎 SEARCH
        if (keyword != null && !keyword.isBlank()) {
            return purchaseRepository.findAll(sortOrder)
                    .stream()
                    .filter(p -> p.getReferenceNumber().toLowerCase().contains(keyword.toLowerCase()) ||
                            (p.getSupplier() != null &&
                                    p.getSupplier().getSupplierName().toLowerCase().contains(keyword.toLowerCase())))
                    .toList();
        }

        // 📦 NORMAL LIST
        return purchaseRepository.findAll(sortOrder);
    }

}
