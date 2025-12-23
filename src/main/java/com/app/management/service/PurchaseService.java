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

    // Method untuk mengambil seluruh data pembelian
    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAll();
    }
    
    // Method untuk mengambil satu pembelian berdasarkan ID dengan validasi keberadaan data
    public Purchase getPurchaseByid(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Purchase tidak ditemukan"));
    }

    // Method untuk membuat transaksi pembelian baru beserta detailnya secara atomic
    @Transactional
    public Purchase createPurchase(Purchase purchase) {

        if (purchaseRepository
                .existsByReferenceNumber(
                        purchase.getReferenceNumber())) {

            throw new IllegalArgumentException(
                    "No. Referensi sudah digunakan, silakan gunakan yang lain");
        }

        purchase.setStatus(PurchaseStatus.CREATED);

        purchase.getPurchaseDetails()
                .removeIf(d ->
                        d.getProduct() == null
                                || d.getQuantity() == null
                                || d.getQuantity() <= 0);

        for (PurchaseDetails d : purchase.getPurchaseDetails()) {

            d.setPurchase(purchase);

            d.setSubtotal(
                    d.getUnitPurchasePrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            d.getQuantity())));
        }

        return purchaseRepository.save(purchase);
    }

    // Method untuk menyelesaikan transaksi pembelian dan menambah stok produk
    @Transactional
    public boolean completePurchase(Long id) {

        Purchase purchase = getPurchaseByid(id);

        if (purchase.getStatus() != PurchaseStatus.CREATED) {
            throw new IllegalStateException("Invalid state transition");
        }

        for (PurchaseDetails detail : purchase.getPurchaseDetails()) {

            Product product = detail.getProduct();

            detail.setPurchasePriceBefore(
                    product.getLastPurchasePrice());

            product.setCurrentStock(
                    product.getCurrentStock()
                            + detail.getQuantity());

            product.setLastPurchasePrice(
                    detail.getUnitPurchasePrice());
        }

        purchase.setStatus(PurchaseStatus.COMPLETED);

        return true;
    }

    // Method untuk membatalkan transaksi pembelian dengan penanganan rollback stok jika diperlukan
    @Transactional
    public boolean cancelPurchase(Long id) {

        Purchase purchase = getPurchaseByid(id);

        if (purchase.getStatus() == PurchaseStatus.CANCELLED) {
            throw new IllegalStateException("Purchase already cancelled");
        }

        if (purchase.getStatus() == PurchaseStatus.CREATED) {
            purchase.setStatus(PurchaseStatus.CANCELLED);
            return true;
        }

        if (purchase.getStatus() == PurchaseStatus.COMPLETED) {

            for (PurchaseDetails detail : purchase.getPurchaseDetails()) {

                Product product = detail.getProduct();

                product.setCurrentStock(
                        product.getCurrentStock()
                                - detail.getQuantity());

                if (detail.getPurchasePriceBefore() != null) {
                    product.setLastPurchasePrice(
                            detail.getPurchasePriceBefore());
                }
            }

            purchase.setStatus(PurchaseStatus.CANCELLED);
            return true;
        }

        throw new IllegalStateException("Invalid purchase state");
    }

    // Method untuk mencari dan mengurutkan data pembelian berdasarkan keyword dan parameter sorting
    public List<Purchase> searchAndSort(
            String keyword,
            String sort) {

        Sort sortOrder;

        switch (sort) {
            case "date_asc":
                sortOrder =
                        Sort.by("purchaseDate").ascending();
                break;
            case "total_desc":
                sortOrder =
                        Sort.by("totalPurchase").descending();
                break;
            case "total_asc":
                sortOrder =
                        Sort.by("totalPurchase").ascending();
                break;
            default:
                sortOrder =
                        Sort.by("purchaseDate").descending();
        }

        if (keyword != null && !keyword.isBlank()) {

            return purchaseRepository
                    .findAll(sortOrder)
                    .stream()
                    .filter(p ->
                            p.getReferenceNumber()
                                    .toLowerCase()
                                    .contains(keyword.toLowerCase())
                                    ||
                                    (p.getSupplier() != null
                                            &&
                                            p.getSupplier()
                                                    .getSupplierName()
                                                    .toLowerCase()
                                                    .contains(
                                                            keyword.toLowerCase())))
                    .toList();
        }

        return purchaseRepository.findAll(sortOrder);
    }
}
