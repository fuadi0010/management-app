package com.app.management.service;

// Collection & Stream API
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

// BigDecimal untuk perhitungan uang yang presisi
import java.math.BigDecimal;

// Spring DI & Service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Repository layer
import com.app.management.repository.SalesInvoiceRepository;
import com.app.management.repository.ProductRepository;

// Entity product & sales
import com.app.management.model.product.Product;
import com.app.management.model.sales.InvoiceDetails;
import com.app.management.model.sales.SalesInvoice;
import com.app.management.model.sales.SalesStatus;

// Sorting JPA
import org.springframework.data.domain.Sort;

// Transaction management
import jakarta.transaction.Transactional;

// Menandakan class ini adalah Service (business logic utama invoice)
@Service
public class InvoiceService {

    // Repository invoice
    @Autowired
    private SalesInvoiceRepository salesInvoiceRepository;

    // Repository product (untuk ambil harga & stok valid)
    @Autowired
    private ProductRepository productRepository;

    // Ambil semua invoice (digunakan di list sederhana)
    public List<SalesInvoice> getAllInvoices() {
        return salesInvoiceRepository.findAll();
    }

    // Ambil invoice berdasarkan ID
    // Jika tidak ditemukan → langsung exception
    public SalesInvoice getSalesInvoiceByid(Long id) {
        return salesInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice tidak ditemukan"));
    }

    // ===============================
    // CREATE INVOICE (SERVER AUTHORITY)
    // ===============================

    // @Transactional agar:
    // - perhitungan
    // - set detail
    // - save invoice
    // bersifat atomic
    @Transactional
    public SalesInvoice createInvoice(SalesInvoice invoice) {

        // Validasi dasar: invoice wajib punya detail
        if (invoice.getInvoiceDetails() == null
                || invoice.getInvoiceDetails().isEmpty()) {
            throw new IllegalStateException("Invoice harus memiliki minimal 1 produk");
        }

        // 🔥 Ambil semua product ID dari detail invoice
        // Tujuan: batch fetch → hindari N+1 query
        List<Long> productIds = invoice.getInvoiceDetails().stream()
                .map(d -> d.getProduct().getId())
                .toList();

        // Ambil semua product sekaligus lalu mapping ke Map<ID, Product>
        Map<Long, Product> productMap =
                productRepository.findAllById(productIds)
                        .stream()
                        .collect(Collectors.toMap(
                                Product::getId,
                                p -> p));

        BigDecimal total = BigDecimal.ZERO;

        // Loop setiap detail invoice
        for (InvoiceDetails d : invoice.getInvoiceDetails()) {

            // Ambil product valid dari database
            Product product = productMap.get(d.getProduct().getId());
            if (product == null) {
                throw new IllegalStateException("Produk tidak ditemukan");
            }

            // Quantity wajib > 0
            if (d.getQuantity() == null || d.getQuantity() <= 0) {
                throw new IllegalStateException("Quantity harus lebih dari 0");
            }

            // Harga jual diambil dari database (SERVER AUTHORITY)
            BigDecimal unitPrice = product.getStandardSellingPrice();
            if (unitPrice == null) {
                throw new IllegalStateException(
                        "Harga jual belum ditentukan untuk produk: "
                                + product.getProductName());
            }

            // Set relasi detail → invoice
            d.setSalesInvoice(invoice);

            // Set product dari database (bukan dari client)
            d.setProduct(product);

            // Set harga jual final
            d.setUnitSellingPrice(unitPrice);

            // Hitung subtotal
            BigDecimal subtotal =
                    unitPrice.multiply(BigDecimal.valueOf(d.getQuantity()));

            d.setSubtotal(subtotal);

            // Akumulasi total
            total = total.add(subtotal);
        }

        // ===============================
        // HITUNG VAT (JIKA ADA)
        // ===============================

        // VAT tetap optional
        if (invoice.getVatPercentage() != null
                && invoice.getVatPercentage()
                        .compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal vat =
                    total.multiply(invoice.getVatPercentage())
                            .divide(BigDecimal.valueOf(100));

            total = total.add(vat);
        }

        // Set total final
        invoice.setTotalSales(total);

        // Status awal invoice
        invoice.setSalesStatus(SalesStatus.CREATED);

        // Simpan invoice + detail (cascade)
        return salesInvoiceRepository.save(invoice);
    }

    // ===============================
    // COMPLETE SALES
    // ===============================

    // Menyelesaikan invoice & mengurangi stok
    @Transactional
    public void completeSales(Long id) {

        // Ambil invoice valid
        SalesInvoice invoice = getSalesInvoiceByid(id);

        // Redundant check (aman, tapi sebenarnya sudah throw di atas)
        if (invoice == null) {
            throw new IllegalStateException("Invoice tidak ditemukan");
        }

        // Hanya invoice CREATED yang boleh diselesaikan
        if (invoice.getSalesStatus() != SalesStatus.CREATED) {
            throw new IllegalStateException(
                    "Invoice tidak valid untuk diselesaikan");
        }

        // Kurangi stok per produk
        for (InvoiceDetails d : invoice.getInvoiceDetails()) {

            Product p = d.getProduct();

            // Validasi stok cukup
            if (p.getCurrentStock() < d.getQuantity()) {
                throw new IllegalStateException(
                        "Stok tidak cukup: " + p.getProductName());
            }

            // Kurangi stok
            p.setCurrentStock(
                    p.getCurrentStock() - d.getQuantity());
        }

        // Update status invoice
        invoice.setSalesStatus(SalesStatus.COMPLETED);
    }

    // ===============================
    // CANCEL SALES
    // ===============================

    // Membatalkan invoice
    @Transactional
    public boolean cancelSales(Long id) {

        // Ambil invoice
        SalesInvoice salesInvoice = getSalesInvoiceByid(id);

        // Jika sudah cancelled → tidak boleh ulang
        if (salesInvoice.getSalesStatus() == SalesStatus.CANCELLED) {
            throw new IllegalStateException("Invoice sudah dibatalkan");
        }

        // Jika invoice sudah completed
        // → stok harus dikembalikan
        if (salesInvoice.getSalesStatus() == SalesStatus.COMPLETED) {

            for (InvoiceDetails detail
                    : salesInvoice.getInvoiceDetails()) {

                Product product = detail.getProduct();

                product.setCurrentStock(
                        product.getCurrentStock()
                                + detail.getQuantity());
            }
        }

        // Set status cancelled
        salesInvoice.setSalesStatus(SalesStatus.CANCELLED);

        // Simpan perubahan status
        salesInvoiceRepository.save(salesInvoice);

        return true;
    }

    // ===============================
    // SEARCH & SORT
    // ===============================

    // Digunakan di halaman list invoice
    public List<SalesInvoice> searchAndSort(
            String keyword,
            String sort) {

        Sort sortOrder;

        // Tentukan sorting
        switch (sort) {
            case "date_asc":
                sortOrder = Sort.by("invoiceDate").ascending();
                break;
            case "total_desc":
                sortOrder = Sort.by("totalSales").descending();
                break;
            case "total_asc":
                sortOrder = Sort.by("totalSales").ascending();
                break;
            default:
                sortOrder = Sort.by("invoiceDate").descending();
        }

        // Jika keyword ada → search by invoice number atau customer name
        if (keyword != null && !keyword.isBlank()) {
            return salesInvoiceRepository
                    .findByInvoiceNumberContainingIgnoreCaseOrCustomerNameContainingIgnoreCase(
                            keyword,
                            keyword,
                            sortOrder);
        }

        // Jika tanpa keyword → ambil semua
        return salesInvoiceRepository.findAll(sortOrder);
    }

}
