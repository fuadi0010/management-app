package com.app.management.service;

// BigDecimal untuk perhitungan uang yang presisi
import java.math.BigDecimal;

// Collection
import java.util.List;

// Spring DI & Service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

// Entity
import com.app.management.model.product.Product;
import com.app.management.model.purchase.Purchase;
import com.app.management.model.purchase.PurchaseDetails;
import com.app.management.model.purchase.PurchaseStatus;

// Repository
import com.app.management.repository.PurchaseRepository;

// Transaction management
import jakarta.transaction.Transactional;

// Menandakan class ini adalah Service
@Service
public class PurchaseService {

    // Repository pembelian
    @Autowired
    private PurchaseRepository purchaseRepository;

    // Ambil semua purchase (digunakan untuk list sederhana)
    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAll();
    }

    // Ambil purchase berdasarkan ID
    // Jika tidak ditemukan → exception
    public Purchase getPurchaseByid(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Purchase tidak ditemukan"));
    }

    // ===============================
    // CREATE PURCHASE
    // ===============================

    // @Transactional agar seluruh proses save bersifat atomic
    @Transactional
    public Purchase createPurchase(Purchase purchase) {

        // Validasi reference number harus unik
        if (purchaseRepository
                .existsByReferenceNumber(
                        purchase.getReferenceNumber())) {

            throw new IllegalArgumentException(
                    "No. Referensi sudah digunakan, silakan gunakan yang lain");
        }

        // Status awal purchase
        purchase.setStatus(PurchaseStatus.CREATED);

        // Hapus detail yang tidak valid:
        // - product null
        // - quantity null / <= 0
        purchase.getPurchaseDetails()
                .removeIf(d ->
                        d.getProduct() == null
                                || d.getQuantity() == null
                                || d.getQuantity() <= 0);

        // Loop setiap detail yang valid
        for (PurchaseDetails d : purchase.getPurchaseDetails()) {

            // Set relasi detail → purchase
            d.setPurchase(purchase);

            // Hitung subtotal per item
            d.setSubtotal(
                    d.getUnitPurchasePrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            d.getQuantity())));
        }

        // Simpan purchase + detail (cascade)
        return purchaseRepository.save(purchase);
    }

    // ===============================
    // COMPLETE PURCHASE
    // ===============================

    // Menyelesaikan purchase & menambah stok
    @Transactional
    public boolean completePurchase(Long id) {

        // Ambil purchase
        Purchase purchase = getPurchaseByid(id);

        // Validasi state transition
        if (purchase.getStatus() != PurchaseStatus.CREATED) {
            throw new IllegalStateException("Invalid state transition");
        }

        // Update stok & harga beli
        for (PurchaseDetails detail : purchase.getPurchaseDetails()) {

            Product product = detail.getProduct();

            // Simpan harga beli sebelumnya (untuk audit / rollback)
            detail.setPurchasePriceBefore(
                    product.getLastPurchasePrice());

            // Tambah stok
            product.setCurrentStock(
                    product.getCurrentStock()
                            + detail.getQuantity());

            // Update harga beli terakhir
            product.setLastPurchasePrice(
                    detail.getUnitPurchasePrice());
        }

        // Update status purchase
        purchase.setStatus(PurchaseStatus.COMPLETED);

        return true;
    }

    // ===============================
    // CANCEL PURCHASE
    // ===============================

    // Membatalkan purchase
    @Transactional
    public boolean cancelPurchase(Long id) {

        // Ambil purchase
        Purchase purchase = getPurchaseByid(id);

        // Redundant check (aman, meskipun sudah throw di atas)
        if (purchase == null) {
            throw new IllegalArgumentException("Purchase not found");
        }

        // Jika sudah cancelled → tidak boleh ulang
        if (purchase.getStatus() == PurchaseStatus.CANCELLED) {
            throw new IllegalStateException("Purchase already cancelled");
        }

        // ❗ PENTING:
        // Jika purchase belum COMPLETED → jangan sentuh stok
        if (purchase.getStatus() == PurchaseStatus.CREATED) {
            purchase.setStatus(PurchaseStatus.CANCELLED);
            return true;
        }

        // Jika suatu hari diizinkan cancel dari COMPLETED
        if (purchase.getStatus() == PurchaseStatus.COMPLETED) {

            // Rollback stok & harga beli
            for (PurchaseDetails detail : purchase.getPurchaseDetails()) {

                Product product = detail.getProduct();

                // Kurangi stok yang sebelumnya ditambahkan
                product.setCurrentStock(
                        product.getCurrentStock()
                                - detail.getQuantity());

                // Kembalikan harga beli sebelumnya (jika ada)
                if (detail.getPurchasePriceBefore() != null) {
                    product.setLastPurchasePrice(
                            detail.getPurchasePriceBefore());
                }
            }

            purchase.setStatus(PurchaseStatus.CANCELLED);
            return true;
        }

        // State tidak valid
        throw new IllegalStateException("Invalid purchase state");
    }

    // ===============================
    // SEARCH & SORT
    // ===============================

    // Digunakan di halaman list purchase
    public List<Purchase> searchAndSort(
            String keyword,
            String sort) {

        Sort sortOrder;

        // Tentukan sorting
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
                // Default: date_desc
                sortOrder =
                        Sort.by("purchaseDate").descending();
        }

        // 🔎 SEARCH
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

        // 📦 NORMAL LIST
        return purchaseRepository.findAll(sortOrder);
    }

}
