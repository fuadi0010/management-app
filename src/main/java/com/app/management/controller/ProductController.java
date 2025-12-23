package com.app.management.controller;

// Collection untuk menampung hasil query product
import java.util.List;

// Spring Dependency Injection & MVC
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

// Untuk validasi form
import org.springframework.validation.BindingResult;

// Mapping request HTTP
import org.springframework.web.bind.annotation.*;

// Flash message setelah redirect
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Entity Product
import com.app.management.model.product.Product;

// Enum status produk (ACTIVE / HIDDEN)
import com.app.management.model.product.ProductStatus;

// Role & User entity
import com.app.management.model.user.Role;
import com.app.management.model.user.User;

// Service layer product (business logic)
import com.app.management.service.ProductService;

// Session handling
import jakarta.servlet.http.HttpSession;

// Bean validation
import jakarta.validation.Valid;

// Menandakan ini MVC Controller
@Controller

// Base URL untuk fitur product
@RequestMapping("/product")
public class ProductController {

    // Inject ProductService
    @Autowired
    private ProductService productService;

    // ======================
    // DASHBOARD URL HELPER
    // ======================

    // Menentukan dashboard berdasarkan role
    private String getDashboardUrl(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            return "/admin/dashboard";
        } else {
            return "/access/staff/dashboard";
        }
    }

    // ======================
    // FORM ACTION HELPER
    // ======================

    // Menentukan action form (add / update)
    // Digunakan agar satu form bisa dipakai untuk add & edit
    private String getFormAction(Product product, String role) {
        // Jika product punya ID → berarti edit
        if (product.getId() != null) {
            return "/product/update";
        } else {
            // Jika belum ada ID → tambah produk baru
            return "/product/add";
        }
    }

    // ======================
    // LIST PRODUCT
    // ======================

    @GetMapping("/list")
    public String listProduct(
            // Role wajib dikirim untuk routing UI
            @RequestParam String role,

            // Keyword pencarian (opsional)
            @RequestParam(required = false) String keyword,

            // Filter status produk (ACTIVE / HIDDEN)
            @RequestParam(required = false) ProductStatus status,

            // Sorting default berdasarkan nama
            @RequestParam(defaultValue = "name_asc") String sort,

            Model model) {

        // ===== LOGIC LAMA (TIDAK DIGANGGU) =====
        // Ambil produk berdasarkan keyword, status, dan sort
        List<Product> products =
                productService.searchAndSortProduct(keyword, status, sort);

        // ===== BACK BUTTON FIX =====
        // Tentukan dashboard berdasarkan role
        String dashboardUrl;
        if ("admin".equalsIgnoreCase(role)) {
            dashboardUrl = "/admin/dashboard";
        } else {
            dashboardUrl = "/staff/dashboard";
        }

        // ===== MODEL KE VIEW =====
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);

        // Role user (penting untuk conditional UI)
        model.addAttribute("userRole", role);

        // URL dashboard untuk tombol kembali
        model.addAttribute("dashboardUrl", dashboardUrl);

        return "product/list-product";
    }

    // ======================
    // ADD / EDIT FORM
    // ======================

    // Satu form dipakai untuk add & edit product
    @GetMapping("/add")
    public String showProductForm(
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,

            // Jika id ada → edit mode
            @RequestParam(value = "id", required = false) Long id,

            Model model) {

        Product product;

        if (id != null) {
            // Ambil product berdasarkan ID (edit)
            product = productService.getProductById(id);

            // Jika product tidak ditemukan → kembali ke list
            if (product == null) {
                return "redirect:/product/list?role=" + role;
            }
        } else {
            // Jika tidak ada ID → tambah product baru
            product = new Product();
        }

        // Kirim product ke form
        model.addAttribute("product", product);

        // Data routing UI
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));

        // Action form dinamis (add / update)
        model.addAttribute("formAction", getFormAction(product, role));

        // Template yang sama untuk add & edit
        return "product/add-product";
    }

    // ======================
    // ADD PRODUCT
    // ======================

    @PostMapping("/add")
    public String saveProduct(
            // Validasi entity Product
            @Valid @ModelAttribute("product") Product product,

            // Menyimpan hasil validasi
            BindingResult result,

            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,

            RedirectAttributes redirectAttributes,
            Model model) {

        // Jika validasi gagal → kembali ke form
        if (result.hasErrors()) {
            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("formAction", "/product/add");
            return "product/add-product";
        }

        try {
            // Simpan produk baru
            productService.saveProduct(product);

            // Flash message sukses
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Produk berhasil ditambahkan!");

            return "redirect:/product/list?role=" + role;

        } catch (Exception e) {
            // Tangani error dari service / DB
            model.addAttribute("error", "Terjadi kesalahan: " + e.getMessage());
            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("formAction", "/product/add");
            return "product/add-product";
        }
    }

    // ======================
    // UPDATE PRODUCT
    // ======================

    @PostMapping("/update")
    public String updateProduct(
            @Valid @ModelAttribute("product") Product product,
            BindingResult result,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Jika validasi gagal → kembali ke form
        if (result.hasErrors()) {
            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("formAction", "/product/update");
            return "product/add-product";
        }

        try {
            // Update data product berdasarkan ID
            productService.updateProductInfo(product.getId(), product);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Product berhasil diupdate");

            return "redirect:/product/list?role=" + role;

        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan: " + e.getMessage());
            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("formAction", "/product/update");
            return "product/add-product";
        }
    }

    // ======================
    // MANAGE PAGE (HIDE / UNHIDE)
    // ======================

    @GetMapping("/manage")
    public String manageProduct(
            // Filter & sort produk aktif
            @RequestParam(required = false) String activeKeyword,
            @RequestParam(defaultValue = "name_asc") String activeSort,

            // Filter & sort produk tersembunyi
            @RequestParam(required = false) String hiddenKeyword,
            @RequestParam(defaultValue = "name_asc") String hiddenSort,

            HttpSession session,
            Model model) {

        // Ambil user dari session (biasanya admin)
        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);

        // Ambil produk aktif
        model.addAttribute(
                "activeProducts",
                productService.searchSortByStatus(
                        ProductStatus.ACTIVE,
                        activeKeyword,
                        activeSort));

        // Ambil produk tersembunyi
        model.addAttribute(
                "hiddenProducts",
                productService.searchSortByStatus(
                        ProductStatus.HIDDEN,
                        hiddenKeyword,
                        hiddenSort));

        // Kirim kembali parameter ke view
        model.addAttribute("activeKeyword", activeKeyword);
        model.addAttribute("activeSort", activeSort);
        model.addAttribute("hiddenKeyword", hiddenKeyword);
        model.addAttribute("hiddenSort", hiddenSort);

        return "hidden/hidden";
    }

    // ======================
    // HIDE PRODUCT
    // ======================

    @GetMapping("/hide/{id}")
    public String hideProduct(
            @PathVariable Long id,
            HttpSession session) {

        // Ambil admin dari session
        User admin = (User) session.getAttribute("user");

        // Ubah status produk menjadi HIDDEN
        productService.hideProduct(id, admin);

        return "redirect:/product/manage";
    }

    // ======================
    // UNHIDE PRODUCT
    // ======================

    @GetMapping("/unhide/{id}")
    public String unhideProduct(
            @PathVariable Long id,
            HttpSession session) {

        // Ambil admin dari session
        User admin = (User) session.getAttribute("user");

        // Kembalikan status produk menjadi ACTIVE
        productService.unhideProduct(id, admin);

        return "redirect:/product/manage";
    }

}
