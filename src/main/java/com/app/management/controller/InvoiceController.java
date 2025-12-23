package com.app.management.controller;

// Dependency Injection & MVC annotation
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

// Untuk flash message setelah redirect (success / error)
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Entity detail invoice penjualan
import com.app.management.model.sales.InvoiceDetails;

// Entity utama invoice penjualan
import com.app.management.model.sales.SalesInvoice;

// Entity user untuk session login
import com.app.management.model.user.User;

// Service layer untuk invoice (business logic)
import com.app.management.service.InvoiceService;

// Service layer untuk product (ambil produk aktif)
import com.app.management.service.ProductService;

// Session handling
import jakarta.servlet.http.HttpSession;

// Menandakan class ini adalah MVC Controller
@Controller

// Base URL untuk seluruh fitur invoice penjualan
@RequestMapping("/sales")
public class InvoiceController {

    // Inject InvoiceService (logic create, complete, cancel, dll)
    @Autowired
    private InvoiceService invoiceService;

    // Inject ProductService (ambil produk aktif)
    @Autowired
    private ProductService productService;

    // ======================
    // UNTUK MENDAPATKAN ROLE
    // ======================

    // Helper method untuk menentukan URL dashboard berdasarkan role
    // Tidak mengandung logic bisnis, hanya routing
    private String getDashboardUrl(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            return "/admin/dashboard";
        } else {
            return "/access/staff/dashboard";
        }
    }

    // ================= LIST =================

    // Menampilkan daftar invoice dengan fitur search & sort
    @GetMapping("/list")
    public String listInvoice(
            // Role dikirim dari URL (admin / staff)
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,

            // Keyword pencarian (opsional)
            @RequestParam(required = false) String keyword,

            // Parameter sorting (default: tanggal desc)
            @RequestParam(defaultValue = "date_desc") String sort,

            // Session untuk cek login
            HttpSession session,

            // Model untuk data ke view
            Model model) {

        // Ambil user login dari session
        User user = (User) session.getAttribute("user");

        // Jika belum login → redirect ke login
        if (user == null) {
            return "redirect:/access/login";
        }

        // Ambil invoice berdasarkan keyword & sorting
        // Filtering dilakukan di service (best practice)
        model.addAttribute(
                "invoices",
                invoiceService.searchAndSort(keyword, sort));

        // Kirim parameter agar tetap tampil di UI
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);

        // Kirim role user ke view
        model.addAttribute("userRole", role);

        // URL dashboard sesuai role
        model.addAttribute("dashboardUrl", getDashboardUrl(role));

        // URL tombol kembali
        model.addAttribute("backUrl", "/main/back?role=" + role);

        // Return ke halaman list invoice
        return "sales/list-invoice";
    }

    // ================= ADD FORM =================

    // Menampilkan form tambah invoice
    @GetMapping("/add")
    public String showForm(
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            HttpSession session,
            Model model) {

        // Ambil user dari session
        User user = (User) session.getAttribute("user");

        // Validasi login
        if (user == null) {
            return "redirect:/access/login";
        }

        // Buat object invoice baru
        SalesInvoice invoice = new SalesInvoice();

        // Tambahkan satu InvoiceDetails kosong
        // Agar form minimal punya satu baris produk
        invoice.addInvoiceDetail(new InvoiceDetails());

        // Kirim object invoice ke form
        model.addAttribute("salesInvoice", invoice);

        // Ambil hanya produk aktif sesuai hak akses user
        model.addAttribute("products",
                productService.getActiveProducts(user));

        // Data routing UI
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));
        model.addAttribute("backUrl", "/sales/list?role=" + role);

        // Return ke form tambah invoice
        return "sales/add-invoice";
    }

    // ================= SAVE =================

    // Menyimpan invoice baru ke database
    @PostMapping("/add")
    public String saveInvoice(
            // Data invoice dari form (header + detail)
            @ModelAttribute SalesInvoice salesInvoice,

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
            // Validasi minimal harus ada 1 detail produk
            if (salesInvoice.getInvoiceDetails() == null
                    || salesInvoice.getInvoiceDetails().isEmpty()) {

                // Error ditampilkan tanpa redirect
                model.addAttribute("error", "Minimal 1 produk harus dipilih");

                // Reload data pendukung form
                model.addAttribute("products",
                        productService.getActiveProducts(user));
                model.addAttribute("userRole", role);
                model.addAttribute("dashboardUrl", getDashboardUrl(role));
                model.addAttribute("backUrl", "/sales/list?role=" + role);

                return "sales/add-invoice";
            }

            // Simpan invoice (biasanya transactional di service)
            invoiceService.createInvoice(salesInvoice);

            // Flash message sukses
            redirectAttributes.addFlashAttribute(
                    "success", "Invoice berhasil dibuat!");

            // Redirect ke list (PRG pattern)
            return "redirect:/sales/list?role=" + role;

        } catch (Exception e) {
            // Tangkap error dari business logic / DB
            model.addAttribute("error", "Gagal: " + e.getMessage());

            // Reload data agar form tidak kosong
            model.addAttribute("products",
                    productService.getActiveProducts(user));
            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("backUrl", "/sales/list?role=" + role);

            return "sales/add-invoice";
        }
    }

    // ================= VIEW =================

    // Menampilkan detail satu invoice
    @GetMapping("/view/{id}")
    public String viewInvoice(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            HttpSession session,
            Model model) {

        // Validasi login
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/access/login";
        }

        // Ambil invoice berdasarkan ID
        SalesInvoice invoice = invoiceService.getSalesInvoiceByid(id);

        // Jika tidak ditemukan → kembali ke list
        if (invoice == null) {
            return "redirect:/sales/list?role=" + role;
        }

        // Kirim data ke view
        model.addAttribute("invoice", invoice);
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));
        model.addAttribute("backUrl", "/sales/list?role=" + role);

        return "sales/view-invoice";
    }

    // ================= COMPLETE =================

    // Menyelesaikan invoice (misal: update status COMPLETED)
    @GetMapping("/complete/{id}")
    public String completeInvoice(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            RedirectAttributes redirectAttributes) {

        try {
            // Delegasi logic ke service
            invoiceService.completeSales(id);

            redirectAttributes.addFlashAttribute(
                    "success", "Invoice berhasil diselesaikan!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "Gagal: " + e.getMessage());
        }

        return "redirect:/sales/list?role=" + role;
    }

    // ================= CANCEL =================

    // Membatalkan invoice
    @GetMapping("/cancel/{id}")
    public String cancelInvoice(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            RedirectAttributes redirectAttributes) {

        try {
            // Delegasi logic pembatalan ke service
            invoiceService.cancelSales(id);

            redirectAttributes.addFlashAttribute(
                    "success", "Invoice berhasil dibatalkan!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "Gagal: " + e.getMessage());
        }

        return "redirect:/sales/list?role=" + role;
    }
}
