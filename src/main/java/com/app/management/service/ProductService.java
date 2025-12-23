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

        // Method untuk mengambil satu product berdasarkan ID
        public Product getProductById(Long id) {
                return productRepository.findById(id)
                                .orElse(null);
        }

        // Method untuk mengambil daftar product aktif yang bisa diakses user
        public List<Product> getActiveProducts(User user) {

                if (user == null) {
                        throw new IllegalStateException("User belum login");
                }

                return productRepository.findByStatus(ProductStatus.ACTIVE);
        }

        // Method untuk mengambil daftar product tersembunyi khusus admin
        public List<Product> getHiddenProducts(User admin) {

                if (admin == null || admin.getRole() != Role.ADMIN) {
                        throw new IllegalStateException(
                                        "Hanya admin yang boleh melihat produk tersembunyi");
                }

                return productRepository.findByStatus(ProductStatus.HIDDEN);
        }

        // Method untuk menyimpan product baru dengan validasi dan default value
        public Product saveProduct(Product product) {

                if (product.getProductCode() == null
                                || product.getProductCode().isBlank()) {
                        throw new IllegalArgumentException("Kode product wajib diisi");
                }

                if (product.getProductName() == null
                                || product.getProductName().isBlank()) {
                        throw new IllegalArgumentException("Nama product wajib diisi");
                }

                if (!product.getProductCode()
                                .matches("^PRD-[A-Z0-9]{3,10}$")) {
                        throw new IllegalArgumentException(
                                        "Kode product harus PRD- diikuti 3–10 huruf/angka besar");
                }

                if (productRepository
                                .existsByProductCodeIgnoreCase(
                                                product.getProductCode())) {
                        throw new IllegalArgumentException(
                                        "Kode product sudah terdaftar: "
                                                        + product.getProductCode());
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

        // Method untuk memperbarui sebagian data product berdasarkan ID
        public Product updateProductInfo(Long id, Product newData) {

                return productRepository.findById(id)
                                .map(existing -> {

                                        if (newData.getProductCode() != null) {

                                                if (!newData.getProductCode()
                                                                .matches("^PRD-[A-Z0-9]{3,10}$")) {
                                                        throw new IllegalArgumentException(
                                                                        "Format kode product tidak valid");
                                                }

                                                existing.setProductCode(
                                                                newData.getProductCode());
                                        }

                                        if (newData.getProductName() != null) {
                                                existing.setProductName(
                                                                newData.getProductName());
                                        }

                                        if (newData.getStandardSellingPrice() != null) {

                                                if (existing.getLastPurchasePrice() != null
                                                                && newData.getStandardSellingPrice()
                                                                                .compareTo(
                                                                                                existing.getLastPurchasePrice()) <= 0) {
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

        // Method untuk memproses pembelian product (penambahan stok dan update harga
        // beli)
        public Product processPurchase(
                        Long productId,
                        int qty,
                        BigDecimal purchasePrice) {

                if (qty <= 0) {
                        throw new IllegalArgumentException(
                                        "Quantity harus lebih dari 0");
                }

                if (purchasePrice == null
                                || purchasePrice.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException(
                                        "Harga beli harus lebih dari 0");
                }

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("Product tidak ditemukan"));

                product.setCurrentStock(
                                product.getCurrentStock() + qty);

                product.setLastPurchasePrice(purchasePrice);

                return product;
        }

        // Method untuk mencari product aktif berdasarkan keyword
        public List<Product> searchProducts(String keyword) {

                if (keyword == null || keyword.isBlank()) {
                        return productRepository.findByStatus(ProductStatus.ACTIVE);
                }

                return productRepository
                                .findByProductNameContainingIgnoreCaseAndStatus(
                                                keyword,
                                                ProductStatus.ACTIVE);
        }

        // Method untuk menyembunyikan product (soft delete) oleh admin
        public void hideProduct(Long productId, User admin) {

                if (admin == null || admin.getRole() != Role.ADMIN) {
                        throw new IllegalStateException(
                                        "Hanya admin yang boleh menyembunyikan produk");
                }

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new IllegalStateException("Produk tidak ditemukan"));

                product.setStatus(ProductStatus.HIDDEN);
        }

        // Method untuk mengaktifkan kembali product yang disembunyikan
        public void unhideProduct(Long productId, User admin) {

                if (admin == null || admin.getRole() != Role.ADMIN) {
                        throw new IllegalStateException(
                                        "Hanya admin yang boleh mengaktifkan produk");
                }

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new IllegalStateException("Produk tidak ditemukan"));

                product.setStatus(ProductStatus.ACTIVE);
        }

        // Method untuk mencari dan mengurutkan product berdasarkan keyword, status, dan
        // parameter sorting
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
                ProductStatus finalStatus = (status == null) ? ProductStatus.ACTIVE : status;

                if (keyword == null || keyword.isBlank()) {
                        return productRepository.findByStatus(finalStatus)
                                        .stream()
                                        .sorted(sort.getOrderFor(sort.iterator().next().getProperty())
                                                        .isAscending()
                                                                        ? (a, b) -> a.getProductName()
                                                                                        .compareToIgnoreCase(b
                                                                                                        .getProductName())
                                                                        : (a, b) -> b.getProductName()
                                                                                        .compareToIgnoreCase(a
                                                                                                        .getProductName()))
                                        .toList();
                }

                return productRepository
                                .findByProductNameContainingIgnoreCaseAndStatus(
                                                keyword,
                                                finalStatus);
        }

        // Method untuk mencari dan mengurutkan product berdasarkan status tertentu
        public List<Product> searchSortByStatus(
                        ProductStatus status,
                        String keyword,
                        String sortParam) {

                List<Product> products = productRepository.findByStatus(status);

                if (keyword != null && !keyword.isBlank()) {
                        products = products.stream()
                                        .filter(p -> p.getProductName()
                                                        .toLowerCase()
                                                        .contains(
                                                                        keyword.toLowerCase()))
                                        .toList();
                }

                products = products.stream()
                                .sorted((a, b) -> {
                                        return switch (sortParam) {
                                                case "name_desc" ->
                                                        b.getProductName()
                                                                        .compareToIgnoreCase(
                                                                                        a.getProductName());
                                                case "stock_asc" ->
                                                        a.getCurrentStock()
                                                                        .compareTo(
                                                                                        b.getCurrentStock());
                                                case "stock_desc" ->
                                                        b.getCurrentStock()
                                                                        .compareTo(
                                                                                        a.getCurrentStock());
                                                default ->
                                                        a.getProductName()
                                                                        .compareToIgnoreCase(
                                                                                        b.getProductName());
                                        };
                                })
                                .toList();

                return products;
        }
}
