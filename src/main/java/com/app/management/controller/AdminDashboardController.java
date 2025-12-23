package com.app.management.controller;

// Import enum Role untuk validasi hak akses (ADMIN / USER / dll)
import com.app.management.model.user.Role;

// Import entity User yang disimpan di session
import com.app.management.model.user.User;

// Service layer untuk seluruh business logic user
import com.app.management.service.UserService;

// Dependency Injection & MVC annotations
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

// HttpSession digunakan untuk session-based authentication
import jakarta.servlet.http.HttpSession;

// Menandakan class ini adalah Spring MVC Controller (bukan REST)
@Controller

// Base URL untuk seluruh endpoint di controller ini
@RequestMapping("/admin")
public class AdminDashboardController {

    // Inject UserService untuk memisahkan controller & business logic
    // Controller hanya orchestration, bukan logic
    @Autowired
    private UserService userService;

    // Endpoint GET untuk halaman dashboard admin
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {

        // Ambil object user dari session (login-based authentication)
        User admin = (User) session.getAttribute("user");

        // Validasi keamanan:
        // - jika belum login (admin == null)
        // - atau role bukan ADMIN
        // maka redirect ke halaman login
        if (admin == null || admin.getRole() != Role.ADMIN) {
            return "redirect:/access/login";
        }

        // Ambil daftar user yang statusnya masih pending approval
        // Query & filtering dilakukan di service layer (best practice)
        var pendingUsers = userService.getPendingUsers();

        // Kirim data admin ke view (untuk greeting / navbar)
        model.addAttribute("user", admin);

        // Kirim daftar user pending ke tabel dashboard
        model.addAttribute("users", pendingUsers);

        // Statistik total user (biasanya query COUNT)
        model.addAttribute("totalUsers", userService.getTotalUserCount());

        // Hitung pending user dari hasil query (lebih efisien dari query ulang)
        model.addAttribute("pendingUsersCount", pendingUsers.size());

        // Statistik user aktif
        model.addAttribute("activeUsers", userService.getActiveUserCount());

        // Return ke template Thymeleaf admin-dashboard.html
        return "access/admin-dashboard";
    }

    // Endpoint untuk menyetujui satu user berdasarkan ID
    // Menggunakan GET karena dipanggil dari tombol / link (catatan ada di bawah)
    @GetMapping("/users/approve/{id}")
    public String approve(@PathVariable Long id) {

        // Delegasi proses approval ke service
        // Biasanya update status user menjadi ACTIVE
        userService.approveUser(id);

        // Redirect agar tidak terjadi double submit saat refresh
        return "redirect:/admin/dashboard";
    }

    // Endpoint untuk menolak satu user berdasarkan ID
    @GetMapping("/users/reject/{id}")
    public String reject(@PathVariable Long id) {

        // Delegasi proses reject ke service
        // Bisa berupa update status atau soft delete
        userService.rejectUser(id);

        // Redirect kembali ke dashboard
        return "redirect:/admin/dashboard";
    }

    // Endpoint untuk approve semua user pending sekaligus
    // Menggunakan POST karena ini operasi bulk (state-changing)
    @PostMapping("/users/approve-all")
    public String approveAll() {

        // Service akan melakukan bulk update (biasanya satu query)
        userService.approveAllPending();

        // Redirect ke dashboard untuk refresh data
        return "redirect:/admin/dashboard";
    }
}
