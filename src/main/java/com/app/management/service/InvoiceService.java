package com.app.management.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import com.app.management.repository.SalesInvoiceRepository;
import com.app.management.repository.ProductRepository;

import com.app.management.model.product.Product;
import com.app.management.model.sales.InvoiceDetails;
import com.app.management.model.sales.SalesInvoice;
import com.app.management.model.sales.SalesStatus;

import jakarta.transaction.Transactional;

@Service
public class InvoiceService {

    @Autowired
    private SalesInvoiceRepository salesInvoiceRepository;

    @Autowired
    private ProductRepository productRepository;

    // Method untuk mengambil seluruh invoice tanpa filter
    public List<SalesInvoice> getAllInvoices() {
        return salesInvoiceRepository.findAll();
    }

    // Method untuk mengambil satu invoice berdasarkan ID dengan validasi keberadaan data
    public SalesInvoice getSalesInvoiceByid(Long id) {
        return salesInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice tidak ditemukan"));
    }

    // Method untuk membuat invoice penjualan baru dengan perhitungan harga dan validasi server-side
    @Transactional
    public SalesInvoice createInvoice(SalesInvoice invoice) {

        if (invoice.getInvoiceDetails() == null
                || invoice.getInvoiceDetails().isEmpty()) {
            throw new IllegalStateException("Invoice harus memiliki minimal 1 produk");
        }

        List<Long> productIds = invoice.getInvoiceDetails().stream()
                .map(d -> d.getProduct().getId())
                .toList();

        Map<Long, Product> productMap =
                productRepository.findAllById(productIds)
                        .stream()
                        .collect(Collectors.toMap(
                                Product::getId,
                                p -> p));

        BigDecimal total = BigDecimal.ZERO;

        for (InvoiceDetails d : invoice.getInvoiceDetails()) {

            Product product = productMap.get(d.getProduct().getId());
            if (product == null) {
                throw new IllegalStateException("Produk tidak ditemukan");
            }

            if (d.getQuantity() == null || d.getQuantity() <= 0) {
                throw new IllegalStateException("Quantity harus lebih dari 0");
            }

            BigDecimal unitPrice = product.getStandardSellingPrice();
            if (unitPrice == null) {
                throw new IllegalStateException(
                        "Harga jual belum ditentukan untuk produk: "
                                + product.getProductName());
            }

            d.setSalesInvoice(invoice);
            d.setProduct(product);
            d.setUnitSellingPrice(unitPrice);

            BigDecimal subtotal =
                    unitPrice.multiply(BigDecimal.valueOf(d.getQuantity()));

            d.setSubtotal(subtotal);
            total = total.add(subtotal);
        }

        if (invoice.getVatPercentage() != null
                && invoice.getVatPercentage()
                        .compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal vat =
                    total.multiply(invoice.getVatPercentage())
                            .divide(BigDecimal.valueOf(100));

            total = total.add(vat);
        }

        invoice.setTotalSales(total);
        invoice.setSalesStatus(SalesStatus.CREATED);

        return salesInvoiceRepository.save(invoice);
    }

    // Method untuk menyelesaikan invoice dan mengurangi stok produk terkait
    @Transactional
    public void completeSales(Long id) {

        SalesInvoice invoice = getSalesInvoiceByid(id);

        if (invoice.getSalesStatus() != SalesStatus.CREATED) {
            throw new IllegalStateException(
                    "Invoice tidak valid untuk diselesaikan");
        }

        for (InvoiceDetails d : invoice.getInvoiceDetails()) {

            Product p = d.getProduct();

            if (p.getCurrentStock() < d.getQuantity()) {
                throw new IllegalStateException(
                        "Stok tidak cukup: " + p.getProductName());
            }

            p.setCurrentStock(
                    p.getCurrentStock() - d.getQuantity());
        }

        invoice.setSalesStatus(SalesStatus.COMPLETED);
    }

    // Method untuk membatalkan invoice dan mengembalikan stok jika sudah completed
    @Transactional
    public boolean cancelSales(Long id) {

        SalesInvoice salesInvoice = getSalesInvoiceByid(id);

        if (salesInvoice.getSalesStatus() == SalesStatus.CANCELLED) {
            throw new IllegalStateException("Invoice sudah dibatalkan");
        }

        if (salesInvoice.getSalesStatus() == SalesStatus.COMPLETED) {

            for (InvoiceDetails detail
                    : salesInvoice.getInvoiceDetails()) {

                Product product = detail.getProduct();

                product.setCurrentStock(
                        product.getCurrentStock()
                                + detail.getQuantity());
            }
        }

        salesInvoice.setSalesStatus(SalesStatus.CANCELLED);
        salesInvoiceRepository.save(salesInvoice);

        return true;
    }

    // Method untuk mencari dan mengurutkan invoice berdasarkan keyword dan parameter sorting
    public List<SalesInvoice> searchAndSort(
            String keyword,
            String sort) {

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
                sortOrder = Sort.by("invoiceDate").descending();
        }

        if (keyword != null && !keyword.isBlank()) {
            return salesInvoiceRepository
                    .findByInvoiceNumberContainingIgnoreCaseOrCustomerNameContainingIgnoreCase(
                            keyword,
                            keyword,
                            sortOrder);
        }

        return salesInvoiceRepository.findAll(sortOrder);
    }
}
