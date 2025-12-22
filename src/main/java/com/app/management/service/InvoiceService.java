package com.app.management.service;

import java.util.List;
import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.management.repository.SalesInvoiceRepository;
import com.app.management.repository.ProductRepository;
import com.app.management.model.product.Product;
import com.app.management.model.sales.InvoiceDetails;
import com.app.management.model.sales.SalesInvoice;
import com.app.management.model.sales.SalesStatus;
import org.springframework.data.domain.Sort;

import jakarta.transaction.Transactional;

@Service
public class InvoiceService {

    @Autowired
    private SalesInvoiceRepository salesInvoiceRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<SalesInvoice> getAllInvoices() {
        return salesInvoiceRepository.findAll();
    }

    public SalesInvoice getSalesInvoiceByid(Long id) {
        return salesInvoiceRepository.findById(id).orElse(null);
    }

    // ===============================
    // CREATE INVOICE (SERVER AUTHORITY)
    // ===============================
    @Transactional
    public SalesInvoice createInvoice(SalesInvoice invoice) {

        if (invoice.getInvoiceDetails() == null || invoice.getInvoiceDetails().isEmpty()) {
            throw new IllegalStateException("Invoice harus memiliki minimal 1 produk");
        }

        BigDecimal total = BigDecimal.ZERO;

        for (InvoiceDetails d : invoice.getInvoiceDetails()) {

            if (d.getProduct() == null || d.getProduct().getId() == null) {
                throw new IllegalStateException("Produk wajib dipilih");
            }

            if (d.getQuantity() == null || d.getQuantity() <= 0) {
                throw new IllegalStateException("Quantity harus lebih dari 0");
            }

            // 🔥 AMBIL PRODUK & HARGA DARI DATABASE (BUKAN DARI CLIENT)
            Product product = productRepository.findById(d.getProduct().getId())
                    .orElseThrow(() -> new IllegalStateException("Produk tidak ditemukan"));

            BigDecimal unitPrice = product.getStandardSellingPrice();
            if (unitPrice == null) {
                throw new IllegalStateException(
                        "Harga jual belum ditentukan untuk produk: " + product.getProductName());
            }

            d.setSalesInvoice(invoice);
            d.setProduct(product);
            d.setUnitSellingPrice(unitPrice);

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(d.getQuantity()));

            d.setSubtotal(subtotal);
            total = total.add(subtotal);
        }

        // VAT (BUSINESS RULE)
        if (invoice.getVatPercentage() != null
                && invoice.getVatPercentage().compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal vat = total
                    .multiply(invoice.getVatPercentage())
                    .divide(BigDecimal.valueOf(100));

            total = total.add(vat);
        }

        invoice.setTotalSales(total);
        invoice.setSalesStatus(SalesStatus.CREATED);

        return salesInvoiceRepository.save(invoice);
    }

    // ===============================
    // COMPLETE SALES
    // ===============================
    @Transactional
    public void completeSales(Long id) {

        SalesInvoice invoice = getSalesInvoiceByid(id);

        if (invoice == null) {
            throw new IllegalStateException("Invoice tidak ditemukan");
        }

        if (invoice.getSalesStatus() != SalesStatus.CREATED) {
            throw new IllegalStateException("Invoice tidak valid untuk diselesaikan");
        }

        for (InvoiceDetails d : invoice.getInvoiceDetails()) {
            Product p = d.getProduct();

            if (p.getCurrentStock() < d.getQuantity()) {
                throw new IllegalStateException(
                        "Stok tidak cukup: " + p.getProductName());
            }

            p.setCurrentStock(p.getCurrentStock() - d.getQuantity());
        }

        invoice.setSalesStatus(SalesStatus.COMPLETED);
    }

    // ===============================
    // CANCEL SALES
    // ===============================
    @Transactional
    public boolean cancelSales(Long id) {

        SalesInvoice salesInvoice = getSalesInvoiceByid(id);

        if (salesInvoice.getSalesStatus() == SalesStatus.CANCELLED) {
            throw new IllegalStateException("Invoice sudah dibatalkan");
        }

        if (salesInvoice.getSalesStatus() == SalesStatus.COMPLETED) {
            for (InvoiceDetails detail : salesInvoice.getInvoiceDetails()) {
                Product product = detail.getProduct();
                product.setCurrentStock(
                        product.getCurrentStock() + detail.getQuantity());
            }
        }

        salesInvoice.setSalesStatus(SalesStatus.CANCELLED);
        salesInvoiceRepository.save(salesInvoice);
        return true;
    }

    public List<SalesInvoice> searchAndSort(String keyword, String sort) {

        Sort sortOrder;

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
                sortOrder = Sort.by("invoiceDate").descending(); // default
        }

        List<SalesInvoice> invoices = salesInvoiceRepository.findAll(sortOrder);

        // 🔎 SEARCH (AMAN, TANPA JPQL)
        if (keyword != null && !keyword.isBlank()) {
            String key = keyword.toLowerCase();
            invoices = invoices.stream()
                    .filter(i -> i.getInvoiceNumber().toLowerCase().contains(key) ||
                            (i.getCustomerName() != null &&
                                    i.getCustomerName().toLowerCase().contains(key)))
                    .toList();
        }

        return invoices;
    }
}
