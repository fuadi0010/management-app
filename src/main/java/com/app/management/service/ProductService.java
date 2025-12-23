package com.app.management.service;

// BigDecimal untuk perhitungan finansial yang presisi
import java.math.BigDecimal;

// Collection
import java.util.List;

// Spring DI & Service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Sorting JPA
import org.springframework.data.domain.Sort;

// Entity & enum product
import com.app.management.model.product.Product;
import com.app.management.model.product.ProductStatus;

// Entity user & role
import com.app.management.model.user.Role;
import com.app.management.model.user.User;

// Repository layer
import com.app.management.repository.ProductRepository;

// Transaction management
import jakarta.transaction.Transactional;

// Menandakan class ini adalah Service
// @Transactional di level class → semua method bersifat transactional
@Service
@Transactional
public class ProductService {

    // Repository product
    @Autowired
    private ProductRepository productRepository;

    // Ambil product berdasarkan ID
    // Return null jika tidak ditemukan (dipakai controller untuk redirect)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElse(null);
    }

    // ================= READ =================

    // Ambil semua product ACTIVE
    public List<Product> getActiveProducts(User user) {

        // Validasi login (basic security)
        if (user == null) {
            throw new IllegalStateException("User belum login");
        }

        return productRepository.findByStatus(ProductStatus.ACTIVE);
    }

    // Ambil product tersembunyi (HIDDEN)
    // Hanya admin yang boleh
    public List<Product> getHiddenProducts(User admin) {

        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException(
                    "Hanya admin yang boleh melihat produk tersembunyi");
        }

        return productRepository.findByStatus(ProductStatus.HIDDEN);
    }

    // ================= CREATE =================

    // Menyimpan product baru
    public Product saveProduct(Product product) {

        // ================= VALIDASI INPUT =================

        // Kode product wajib
        if (product.getProductCode() == null
                || product.getProductCode().isBlank()) {
            throw new IllegalArgumentException("Kode product wajib diisi");
        }

        // Nama product wajib
        if (product.getProductName() == null
                || product.getProductName().isBlank()) {
            throw new IllegalArgumentException("Nama product wajib diisi");
        }

        // Format kode product harus sesuai standar
        if (!product.getProductCode()
                .matches("^PRD-[A-Z0-9]{3,10}$")) {
            throw new IllegalArgumentException(
                    "Kode product harus PRD- diikuti 3–10 huruf/angka besar");
        }

        // Kode product tidak boleh duplikat
        if (productRepository
                .existsByProductCodeIgnoreCase(
                        product.getProductCode())) {
            throw new IllegalArgumentException(
                    "Kode product sudah terdaftar: "
                            + product.getProductCode());
        }

        // ================= DEFAULT VALUE =================

        // Default stok jika null
        if (product.getCurrentStock() == null) {
            product.setCurrentStock(0);
        }

        // Default harga jual
        if (product.getStandardSellingPrice() == null) {
            product.setStandardSellingPrice(BigDecimal.ZERO);
        }

        // Default harga beli terakhir
        if (product.getLastPurchasePrice() == null) {
            product.setLastPurchasePrice(BigDecimal.ZERO);
        }

        // ================= BUSINESS RULE =================

        // Stok tidak boleh negatif
        if (product.getCurrentStock() < 0) {
            throw new IllegalArgumentException("Stok tidak boleh negatif");
        }

        // Harga jual harus lebih tinggi dari harga beli
        if (product.getStandardSellingPrice()
                .compareTo(product.getLastPurchasePrice()) <= 0) {
            throw new IllegalArgumentException(
                    "Harga jual harus lebih tinggi dari harga beli");
        }

        // Status awal product adalah ACTIVE
        product.setStatus(ProductStatus.ACTIVE);

        // Simpan ke database
        return productRepository.save(product);
    }

    // ================= UPDATE =================

    // Update sebagian data product
    public Product updateProductInfo(Long id, Product newData) {

        return productRepository.findById(id)
                .map(existing -> {

                    // Update kode product jika diisi
                    if (newData.getProductCode() != null) {

                        // Validasi format kode
                        if (!newData.getProductCode()
                                .matches("^PRD-[A-Z0-9]{3,10}$")) {
                            throw new IllegalArgumentException(
                                    "Format kode product tidak valid");
                        }

                        existing.setProductCode(
                                newData.getProductCode());
                    }

                    // Update nama product
                    if (newData.getProductName() != null) {
                        existing.setProductName(
                                newData.getProductName());
                    }

                    // Update harga jual
                    if (newData.getStandardSellingPrice() != null) {

                        // Harga jual harus di atas harga beli terakhir
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
                .orElseThrow(() ->
                        new RuntimeException("Product tidak ditemukan"));
    }

    // ================= BUSINESS =================

    // Proses pembelian (tambah stok & update harga beli)
    public Product processPurchase(
            Long productId,
            int qty,
            BigDecimal purchasePrice) {

        // Quantity harus valid
        if (qty <= 0) {
            throw new IllegalArgumentException(
                    "Quantity harus lebih dari 0");
        }

        // Harga beli harus valid
        if (purchasePrice == null
                || purchasePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Harga beli harus lebih dari 0");
        }

        // Ambil product
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product tidak ditemukan"));

        // Tambah stok
        product.setCurrentStock(
                product.getCurrentStock() + qty);

        // Update harga beli terakhir
        product.setLastPurchasePrice(purchasePrice);

        return product;
    }

    // ================= SEARCH =================

    // Search product ACTIVE berdasarkan keyword
    public List<Product> searchProducts(String keyword) {

        // Jika keyword kosong → ambil semua ACTIVE
        if (keyword == null || keyword.isBlank()) {
            return productRepository.findByStatus(ProductStatus.ACTIVE);
        }

        return productRepository
                .findByProductNameContainingIgnoreCaseAndStatus(
                        keyword,
                        ProductStatus.ACTIVE);
    }

    // ================= SOFT DELETE =================

    // Sembunyikan product (soft delete)
    public void hideProduct(Long productId, User admin) {

        // Hanya admin yang boleh
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException(
                    "Hanya admin yang boleh menyembunyikan produk");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new IllegalStateException("Produk tidak ditemukan"));

        // Ubah status menjadi HIDDEN
        product.setStatus(ProductStatus.HIDDEN);
    }

    // Aktifkan kembali product
    public void unhideProduct(Long productId, User admin) {

        // Hanya admin yang boleh
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException(
                    "Hanya admin yang boleh mengaktifkan produk");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new IllegalStateException("Produk tidak ditemukan"));

        // Ubah status menjadi ACTIVE
        product.setStatus(ProductStatus.ACTIVE);
    }

    // ================= SEARCH + SORT =================

    // Search & sort product (dipakai di halaman list)
    public List<Product> searchAndSortProduct(
            String keyword,
            ProductStatus status,
            String sortParam) {

        Sort sort;

        // Tentukan sorting berdasarkan parameter
        switch (sortParam) {
            case "name_asc":
                sort = Sort.by(
                        Sort.Direction.ASC,
                        "productName");
                break;
            case "name_desc":
                sort = Sort.by(
                        Sort.Direction.DESC,
                        "productName");
                break;
            case "stock_asc":
                sort = Sort.by(
                        Sort.Direction.ASC,
                        "currentStock");
                break;
            case "stock_desc":
                sort = Sort.by(
                        Sort.Direction.DESC,
                        "currentStock");
                break;
            default:
                sort = Sort.by(
                        Sort.Direction.ASC,
                        "productName");
        }

        // ===============================
        // FILTERING (TANPA GANGGU REPO)
        // ===============================

        // Jika tanpa keyword & status → ambil semua
        if ((keyword == null || keyword.isBlank())
                && status == null) {
            return productRepository.findAll(sort);
        }

        // Filtering manual di memory
        return productRepository.findAll(sort)
                .stream()
                .filter(p -> {

                    boolean matchKeyword =
                            keyword == null
                                    || keyword.isBlank()
                                    || p.getProductName()
                                            .toLowerCase()
                                            .contains(
                                                    keyword.toLowerCase());

                    boolean matchStatus =
                            status == null
                                    || p.getStatus() == status;

                    return matchKeyword && matchStatus;
                })
                .toList();
    }

    // Search & sort khusus berdasarkan status
    public List<Product> searchSortByStatus(
            ProductStatus status,
            String keyword,
            String sortParam) {

        // 1. Ambil data berdasarkan status
        List<Product> products =
                productRepository.findByStatus(status);

        // 2. SEARCH berdasarkan keyword
        if (keyword != null && !keyword.isBlank()) {
            products = products.stream()
                    .filter(p ->
                            p.getProductName()
                                    .toLowerCase()
                                    .contains(
                                            keyword.toLowerCase()))
                    .toList();
        }

        // 3. SORT manual
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
