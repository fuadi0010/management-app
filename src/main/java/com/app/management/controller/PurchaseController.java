package com.app.management.controller;

// Spring DI & MVC
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

// Flash message setelah redirect
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Entity pembelian
import com.app.management.model.purchase.Purchase;
import com.app.management.model.purchase.PurchaseDetails;
import com.app.management.model.purchase.PurchaseStatus;

// Entity user untuk session
import com.app.management.model.user.User;

// Service layer
import com.app.management.service.PurchaseService;
import com.app.management.service.SupplierService;
import com.app.management.service.ProductService;

// Session handling
import jakarta.servlet.http.HttpSession;

// Model untuk view
import org.springframework.ui.Model;

// BigDecimal untuk perhitungan uang (precision-safe)
import java.math.BigDecimal;

// Collection
import java.util.List;

// Controller MVC
@Controller

// Base URL untuk fitur pembelian
@RequestMapping("/purchase")
public class PurchaseController {

    // Service pembelian (business logic utama)
    @Autowired
    private PurchaseService purchaseService;

    // Service supplier (dropdown supplier)
    @Autowired
    private SupplierService supplierService;

    // Service product (ambil produk aktif)
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
    // LIST PURCHASE
    // ======================

    @GetMapping("/list")
    public String listPurchase(
            // Role user (admin / staff)
            @RequestParam String role,

            // Keyword pencarian (opsional)
            @RequestParam(required = false) String keyword,

            // Sorting default berdasarkan tanggal desc
            @RequestParam(defaultValue = "date_desc") String sort,

            Model model) {

        // ===== dashboardUrl FIX (DITAMBAHKAN) =====
        // Menentukan tujuan tombol kembali
        String dashboardUrl;
        if ("admin".equalsIgnoreCase(role)) {
            dashboardUrl = "/admin/dashboard";
        } else {
            dashboardUrl = "/staff/dashboard";
        }

        // ===== logic lama (TIDAK DIUBAH) =====
        // Ambil list pembelian dengan search & sort
        List<Purchase> purchases =
                purchaseService.searchAndSort(keyword, sort);

        // ===== data ke view =====
        model.addAttribute("purchases", purchases);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", dashboardUrl);

        return "purchase/list-purchase";
    }

    // ======================
    // ADD PURCHASE FORM
    // ======================

    @GetMapping("/add")
    public String addPurchase(
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            HttpSession session,
            Model model) {

        // Ambil user login
        User user = (User) session.getAttribute("user");

        // Validasi login
        if (user == null) {
            return "redirect:/access/login";
        }

        // Buat object purchase baru
        Purchase purchase = new Purchase();

        // Tambahkan 1 detail kosong agar form punya minimal 1 baris
        purchase.addPurchaseDetail(new PurchaseDetails());

        // Kirim object purchase ke form
        model.addAttribute("purchase", purchase);

        // Dropdown supplier
        model.addAttribute("suppliers",
                supplierService.getAllSuppliers());

        // 🔒 PERBAIKAN UTAMA
        // Hanya tampilkan produk yang aktif & sesuai role user
        model.addAttribute("products",
                productService.getActiveProducts(user));

        // Data routing UI
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));

        return "purchase/add-purchase";
    }

    // ======================
    // SAVE PURCHASE
    // ======================

    @PostMapping("/add")
    public String savePurchase(
            // Data purchase dari form (header + detail)
            @ModelAttribute Purchase purchase,

            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,

            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Ambil user login
        User user = (User) session.getAttribute("user");

        // Validasi login
        if (user == null) {
            return "redirect:/access/login";
        }

        try {
            // ======================
            // VALIDASI DASAR
            // ======================

            // Supplier wajib dipilih
            if (purchase.getSupplier() == null
                    || purchase.getSupplier().getId() == null) {
                throw new IllegalArgumentException("Supplier harus dipilih");
            }

            // Minimal harus ada 1 detail produk
            if (purchase.getPurchaseDetails() == null
                    || purchase.getPurchaseDetails().isEmpty()) {
                throw new IllegalArgumentException("Minimal satu produk harus ditambahkan");
            }

            // Set status awal purchase
            purchase.setStatus(PurchaseStatus.CREATED);

            // Total pembelian
            BigDecimal total = BigDecimal.ZERO;

            // Loop setiap detail pembelian
            for (PurchaseDetails detail : purchase.getPurchaseDetails()) {

                // Jika product kosong → skip baris
                if (detail.getProduct() == null
                        || detail.getProduct().getId() == null) {
                    continue;
                }

                // Quantity harus valid
                if (detail.getQuantity() == null
                        || detail.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Quantity harus lebih dari 0");
                }

                // Harga beli harus valid
                if (detail.getUnitPurchasePrice() == null
                        || detail.getUnitPurchasePrice()
                                .compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Harga beli harus lebih dari 0");
                }

                // Set relasi detail → purchase
                detail.setPurchase(purchase);

                // Hitung subtotal per item
                BigDecimal subtotal =
                        detail.getUnitPurchasePrice()
                                .multiply(
                                        BigDecimal.valueOf(detail.getQuantity()));

                // Simpan subtotal ke detail
                detail.setSubtotal(subtotal);

                // Akumulasi total
                total = total.add(subtotal);
            }

            // Total pembelian harus > 0
            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Total pembelian harus lebih dari 0");
            }

            // Set total ke header purchase
            purchase.setTotalPurchase(total);

            // Simpan purchase (biasanya transactional di service)
            purchaseService.createPurchase(purchase);

            // Flash message sukses
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Pembelian berhasil disimpan!");

            return "redirect:/purchase/list?role=" + role;

        } catch (Exception e) {
            // Tangani error validasi / business logic
            model.addAttribute("error", "Gagal menyimpan: " + e.getMessage());

            // Reload data pendukung form
            model.addAttribute("suppliers",
                    supplierService.getAllSuppliers());

            // 🔒 PERBAIKAN UTAMA JUGA DI SINI
            model.addAttribute("products",
                    productService.getActiveProducts(user));

            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("backUrl", "/purchase/list?role=" + role);

            return "purchase/add-purchase";
        }
    }

    // ======================
    // VIEW PURCHASE
    // ======================

    @GetMapping("/view/{id}")
    public String viewPurchase(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            Model model) {

        // Ambil purchase berdasarkan ID
        Purchase purchase = purchaseService.getPurchaseByid(id);

        // Jika tidak ditemukan → kembali ke list
        if (purchase == null) {
            return "redirect:/purchase/list?role=" + role;
        }

        // Kirim data ke view
        model.addAttribute("purchase", purchase);
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));
        model.addAttribute("backUrl", "/purchase/list?role=" + role);

        return "purchase/purchase-detail";
    }

    // ======================
    // COMPLETE PURCHASE
    // ======================

    @GetMapping("/complete/{id}")
    public String completePurchase(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            RedirectAttributes redirectAttributes) {

        // Selesaikan pembelian (biasanya update status + update stok)
        boolean completed = purchaseService.completePurchase(id);

        if (completed) {
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Pembelian selesai!");
        } else {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Gagal menyelesaikan pembelian");
        }

        return "redirect:/purchase/list?role=" + role;
    }

    // ======================
    // CANCEL PURCHASE
    // ======================

    @GetMapping("/cancel/{id}")
    public String cancelPurchase(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            RedirectAttributes redirectAttributes) {

        // Batalkan pembelian
        boolean cancelled = purchaseService.cancelPurchase(id);

        if (cancelled) {
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Pembelian dibatalkan!");
        } else {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Gagal membatalkan pembelian");
        }

        return "redirect:/purchase/list?role=" + role;
    }
}
