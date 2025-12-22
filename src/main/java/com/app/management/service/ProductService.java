package com.app.management.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import com.app.management.model.product.Product;
import com.app.management.model.product.ProductStatus;
import com.app.management.model.user.Role;
import com.app.management.model.user.User;
import com.app.management.repository.ProductRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElse(null);
    }

    // ================= READ =================
    public List<Product> getActiveProducts(User user) {
        if (user == null) {
            throw new IllegalStateException("User belum login");
        }
        return productRepository.findByStatus(ProductStatus.ACTIVE);
    }

    public List<Product> getHiddenProducts(User admin) {
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException(
                    "Hanya admin yang boleh melihat produk tersembunyi");
        }
        return productRepository.findByStatus(ProductStatus.HIDDEN);
    }

    // ================= CREATE =================
    public Product saveProduct(Product product) {

        if (product.getProductCode() == null || product.getProductCode().isBlank()) {
            throw new IllegalArgumentException("Kode product wajib diisi");
        }

        if (product.getProductName() == null || product.getProductName().isBlank()) {
            throw new IllegalArgumentException("Nama product wajib diisi");
        }

        if (!product.getProductCode().matches("^PRD-[A-Z0-9]{3,10}$")) {
            throw new IllegalArgumentException(
                    "Kode product harus PRD- diikuti 3–10 huruf/angka besar");
        }

        if (productRepository.existsByProductCodeIgnoreCase(product.getProductCode())) {
            throw new IllegalArgumentException(
                    "Kode product sudah terdaftar: " + product.getProductCode());
        }

        if (product.getCurrentStock() == null) {
            product.setCurrentStock(0);
        }

        if (product.getStandardSellingPrice() == null) {
            product.setStandardSellingPrice(BigDecimal.ZERO);
        }

        if (product.getLastPurchasePrice() == null) {
            product.setLastPurchasePrice(BigDecimal.ZERO);
        }

        if (product.getCurrentStock() < 0) {
            throw new IllegalArgumentException("Stok tidak boleh negatif");
        }

        if (product.getStandardSellingPrice()
                .compareTo(product.getLastPurchasePrice()) <= 0) {
            throw new IllegalArgumentException(
                    "Harga jual harus lebih tinggi dari harga beli");
        }

        product.setStatus(ProductStatus.ACTIVE);
        return productRepository.save(product);
    }

    // ================= UPDATE =================
    public Product updateProductInfo(Long id, Product newData) {

        return productRepository.findById(id)
                .map(existing -> {

                    if (newData.getProductCode() != null) {
                        if (!newData.getProductCode()
                                .matches("^PRD-[A-Z0-9]{3,10}$")) {
                            throw new IllegalArgumentException("Format kode product tidak valid");
                        }
                        existing.setProductCode(newData.getProductCode());
                    }

                    if (newData.getProductName() != null) {
                        existing.setProductName(newData.getProductName());
                    }

                    if (newData.getStandardSellingPrice() != null) {
                        if (existing.getLastPurchasePrice() != null &&
                                newData.getStandardSellingPrice()
                                        .compareTo(existing.getLastPurchasePrice()) <= 0) {
                            throw new IllegalArgumentException(
                                    "Harga jual harus lebih tinggi dari harga beli");
                        }
                        existing.setStandardSellingPrice(
                                newData.getStandardSellingPrice());
                    }

                    return existing;
                })
                .orElseThrow(() -> new RuntimeException("Product tidak ditemukan"));
    }

    // ================= BUSINESS =================
    public Product processPurchase(Long productId, int qty, BigDecimal purchasePrice) {

        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity harus lebih dari 0");
        }

        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Harga beli harus lebih dari 0");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product tidak ditemukan"));

        product.setCurrentStock(product.getCurrentStock() + qty);
        product.setLastPurchasePrice(purchasePrice);

        return product;
    }

    // ================= SEARCH =================
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return productRepository.findByStatus(ProductStatus.ACTIVE);
        }

        return productRepository
                .findByProductNameContainingIgnoreCaseAndStatus(
                        keyword,
                        ProductStatus.ACTIVE);
    }

    // ================= SOFT DELETE =================
    public void hideProduct(Long productId, User admin) {

        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Hanya admin yang boleh menyembunyikan produk");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Produk tidak ditemukan"));

        product.setStatus(ProductStatus.HIDDEN);
    }

    public void unhideProduct(Long productId, User admin) {

        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Hanya admin yang boleh mengaktifkan produk");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Produk tidak ditemukan"));

        product.setStatus(ProductStatus.ACTIVE);
    }

    public List<Product> searchAndSortProduct(
            String keyword,
            ProductStatus status,
            String sortParam) {

        Sort sort;

        switch (sortParam) {
            case "name_asc":
                sort = Sort.by(Sort.Direction.ASC, "productName");
                break;
            case "name_desc":
                sort = Sort.by(Sort.Direction.DESC, "productName");
                break;
            case "stock_asc":
                sort = Sort.by(Sort.Direction.ASC, "currentStock");
                break;
            case "stock_desc":
                sort = Sort.by(Sort.Direction.DESC, "currentStock");
                break;
            default:
                sort = Sort.by(Sort.Direction.ASC, "productName");
        }

        // ===============================
        // FILTERING (TANPA GANGGU REPO)
        // ===============================
        if ((keyword == null || keyword.isBlank()) && status == null) {
            return productRepository.findAll(sort);
        }

        return productRepository.findAll(sort)
                .stream()
                .filter(p -> {
                    boolean matchKeyword = keyword == null || keyword.isBlank()
                            || p.getProductName().toLowerCase()
                                    .contains(keyword.toLowerCase());

                    boolean matchStatus = status == null || p.getStatus() == status;

                    return matchKeyword && matchStatus;
                })
                .toList();
    }

    public List<Product> searchSortByStatus(
            ProductStatus status,
            String keyword,
            String sortParam) {

        // 1. Ambil data berdasarkan status (ACTIVE / HIDDEN)
        List<Product> products = productRepository.findByStatus(status);

        // 2. SEARCH (jika keyword ada)
        if (keyword != null && !keyword.isBlank()) {
            products = products.stream()
                    .filter(p -> p.getProductName()
                            .toLowerCase()
                            .contains(keyword.toLowerCase()))
                    .toList();
        }

        // 3. SORT (manual, tanpa ganggu repo)
        products = products.stream()
                .sorted((a, b) -> {
                    return switch (sortParam) {
                        case "name_desc" ->
                            b.getProductName().compareToIgnoreCase(a.getProductName());
                        case "stock_asc" ->
                            a.getCurrentStock().compareTo(b.getCurrentStock());
                        case "stock_desc" ->
                            b.getCurrentStock().compareTo(a.getCurrentStock());
                        default ->
                            a.getProductName().compareToIgnoreCase(b.getProductName());
                    };
                })
                .toList();

        return products;
    }

}
