package com.app.management.controller;

// Import enum Role untuk memastikan hanya ADMIN yang bisa mengakses fitur ini
import com.app.management.model.user.Role;

// Import entity User yang disimpan di session login
import com.app.management.model.user.User;

// Import enum UserStatus untuk filter status staff (ACTIVE, BANNED, REJECTED)
import com.app.management.model.user.UserStatus;

// Service layer yang menangani seluruh business logic user
import com.app.management.service.UserService;

// HttpSession untuk mengambil data user login
import jakarta.servlet.http.HttpSession;

// Spring MVC & dependency injection
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

// RedirectAttributes digunakan untuk flash message (success / error)
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Menandakan class ini adalah controller MVC (bukan REST API)
@Controller

// Base path khusus manajemen user oleh admin
@RequestMapping("/admin/users")
public class AdminUserController {

    // Inject UserService agar controller tetap tipis (thin controller)
    @Autowired
    private UserService userService;

    // ======================
    // LIST STAFF
    // ======================

    // Endpoint untuk menampilkan halaman manajemen staff
    @GetMapping
    public String staffManagement(HttpSession session, Model model) {

        // Ambil user login dari session
        User admin = (User) session.getAttribute("user");

        // Validasi keamanan:
        // - user belum login
        // - atau bukan ADMIN
        // Maka redirect ke halaman login
        if (admin == null || admin.getRole() != Role.ADMIN) {
            return "redirect:/access/login";
        }

        // Kirim data admin ke view (untuk navbar / greeting)
        model.addAttribute("user", admin);

        // Ambil staff yang masih aktif
        // Filtering dilakukan di service / repository layer
        model.addAttribute("activeStaffs",
                userService.getStaffByStatus(UserStatus.ACTIVE));

        // Ambil staff yang dibanned atau ditolak
        // Digabung dalam satu list untuk kemudahan manajemen
        model.addAttribute("bannedStaffs",
                userService.getStaffByStatuses(
                        UserStatus.BANNED,
                        UserStatus.REJECTED));

        // Return ke template Thymeleaf staff-management.html
        return "hidden/staff-management";
    }

    // ======================
    // BAN STAFF
    // ======================

    // Endpoint untuk membanned staff berdasarkan ID
    // Menggunakan POST karena ini operasi yang mengubah state
    @PostMapping("/ban/{id}")
    public String banStaff(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra) {

        // Ambil admin dari session untuk audit & validasi
        User admin = (User) session.getAttribute("user");

        try {
            // Delegasi proses ban ke service
            // Biasanya akan:
            // - validasi admin
            // - ubah status staff menjadi BANNED
            userService.banStaff(id, admin);

            // Flash message sukses (tersedia 1x setelah redirect)
            ra.addFlashAttribute("success", "Staff berhasil diban");
        } catch (Exception e) {
            // Tangkap error business logic dari service
            // Contoh: staff belum memenuhi syarat diban
            ra.addFlashAttribute("error", e.getMessage());
        }

        // Redirect agar tidak terjadi duplicate action saat refresh
        return "redirect:/admin/users";
    }

    // ======================
    // HARD DELETE STAFF
    // ======================

    // Endpoint untuk menghapus staff secara permanen
    // Biasanya hanya boleh jika status sudah BANNED
    @PostMapping("/delete/{id}")
    public String deleteStaff(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra) {

        // Ambil admin dari session untuk validasi & audit
        User admin = (User) session.getAttribute("user");

        try {
            // Delegasi proses hard delete ke service
            // Service bertanggung jawab memastikan:
            // - status staff memang BANNED
            // - tidak melanggar foreign key
            userService.deleteBannedStaff(id, admin);

            // Flash message sukses
            ra.addFlashAttribute("success", "Staff berhasil dihapus permanen");
        } catch (Exception e) {
            // Tangkap error dari service (misal staff masih ACTIVE)
            ra.addFlashAttribute("error", e.getMessage());
        }

        // Redirect kembali ke halaman manajemen staff
        return "redirect:/admin/users";
    }
}
