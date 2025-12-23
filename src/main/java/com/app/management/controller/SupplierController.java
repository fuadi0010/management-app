package com.app.management.controller;

// Spring DI & MVC
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

// Validasi form
import org.springframework.validation.BindingResult;

// Mapping HTTP request
import org.springframework.web.bind.annotation.*;

// Flash message setelah redirect
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Entity Supplier
import com.app.management.model.Supplier;

// Entity User untuk session login
import com.app.management.model.user.User;

// Service layer supplier
import com.app.management.service.SupplierService;

// Session handling
import jakarta.servlet.http.HttpSession;

// Bean validation
import jakarta.validation.Valid;

// Menandakan ini MVC Controller
@Controller

// Base URL untuk fitur supplier
@RequestMapping("/supplier")
public class SupplierController {

    // Inject SupplierService (business logic supplier)
    @Autowired
    private SupplierService supplierService;

    // =========================
    // HELPER (TIDAK DIUBAH)
    // =========================

    // Menentukan dashboard berdasarkan role
    private String getDashboardUrl(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            return "/admin/dashboard";
        } else {
            return "/access/staff/dashboard";
        }
    }

    // =========================
    // LIST SUPPLIER
    // =========================

    // Menampilkan daftar supplier dengan fitur search & sort
    @GetMapping("/list")
    public String listSupplier(
            // Role user (admin / staff)
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,

            // Keyword pencarian (opsional)
            @RequestParam(required = false) String keyword,

            // Sorting default berdasarkan ID desc
            @RequestParam(defaultValue = "id_desc") String sort,

            Model model) {

        // Ambil supplier berdasarkan keyword & sorting
        model.addAttribute(
                "supplier",
                supplierService.searchAndSort(keyword, sort));

        // Kirim parameter ke view agar state UI tetap terjaga
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);

        // Role user untuk conditional UI
        model.addAttribute("userRole", role);

        // URL dashboard untuk navigasi
        model.addAttribute("dashboardUrl", getDashboardUrl(role));

        // URL tombol kembali universal
        model.addAttribute("backUrl", "/main/back?role=" + role);

        return "supplier/list-supplier";
    }

    // =========================
    // FORM ADD SUPPLIER
    // =========================

    // Menampilkan form tambah supplier
    @GetMapping("/add")
    public String showAddSupplierForm(
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            Model model) {

        // Kirim object Supplier kosong ke form
        model.addAttribute("supplier", new Supplier());

        // Data routing UI
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));
        model.addAttribute("backUrl", "/supplier/list?role=" + role);

        return "supplier/add-supplier";
    }

    // =========================
    // SAVE SUPPLIER
    // =========================

    // Menyimpan supplier baru ke database
    @PostMapping("/add")
    public String saveSupplier(
            // Validasi entity Supplier
            @Valid @ModelAttribute("supplier") Supplier supplier,

            // Menyimpan hasil validasi
            BindingResult result,

            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,

            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Ambil user dari session (jika ada)
        User user = (User) session.getAttribute("user");

        // Sinkronisasi role dari session (lebih trusted)
        if (user != null) {
            role = user.getRole().name().toLowerCase();
        }

        // Jika validasi gagal → kembali ke form
        if (result.hasErrors()) {
            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("backUrl", "/supplier/list?role=" + role);
            return "supplier/add-supplier";
        }

        // Simpan supplier baru
        supplierService.saveSupplier(supplier);

        // Flash message sukses
        redirectAttributes.addFlashAttribute(
                "successMessage", "Supplier berhasil ditambahkan!");

        // Redirect ke list supplier (PRG pattern)
        return "redirect:/supplier/list?role=" + role;
    }

    // =========================
    // FORM EDIT SUPPLIER (FIX)
    // =========================

    // Menampilkan form edit supplier
    @GetMapping("/edit-supplier/{id}")
    public String showUpdateSupplierForm(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            Model model) {

        // Ambil supplier berdasarkan ID
        Supplier supplier = supplierService.getSupplierById(id);

        // Jika supplier tidak ditemukan → kembali ke list
        if (supplier == null) {
            return "redirect:/supplier/list?role=" + role;
        }

        // Kirim data supplier ke form edit
        model.addAttribute("supplier", supplier);
        model.addAttribute("userRole", role);

        return "supplier/update-supplier";
    }

    // =========================
    // UPDATE SUPPLIER
    // =========================

    // Menyimpan perubahan data supplier
    @PostMapping("/edit-supplier")
    public String updateSupplier(
            // Data supplier hasil edit
            @ModelAttribute("supplier") Supplier supplier,

            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,

            RedirectAttributes redirectAttributes) {

        // Update supplier berdasarkan ID
        supplierService.updateSupplier(supplier.getId(), supplier);

        // Flash message sukses
        redirectAttributes.addFlashAttribute(
                "successMessage", "Supplier berhasil diupdate");

        // Redirect ke list supplier
        return "redirect:/supplier/list?role=" + role;
    }
}
