package com.app.management.controller;

// Dependency Injection & MVC
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

// Mapping HTTP request
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Untuk manipulasi request (session fixation handling)
import jakarta.servlet.http.HttpServletRequest;

// Role & User entity
import com.app.management.model.user.Role;
import com.app.management.model.user.User;

// Service khusus autentikasi & registrasi
import com.app.management.service.LoginService;

// Session handling
import jakarta.servlet.http.HttpSession;

// Menandakan ini MVC Controller
@Controller

// Base path untuk authentication & access
@RequestMapping("/access")
public class LoginController {

    // Inject LoginService (business logic auth)
    @Autowired
    private LoginService loginService;

    // ================= LOGIN =================

    // Menampilkan halaman login
    @GetMapping("/login")
    public String showLoginPage() {
        // Hanya return view login
        return "access/login";
    }

    // Memproses login user
    @PostMapping("/login")
    public String handleLogin(
            // Username / name dari form login
            @RequestParam String name,

            // Password plaintext dari form (akan diverifikasi di service)
            @RequestParam String password,

            // Model untuk kirim error ke view
            Model model,

            // Session lama
            HttpSession session,

            // HttpServletRequest untuk membuat session baru
            HttpServletRequest request) {

        try {
            // Validasi login melalui service
            // Biasanya:
            // - cek user exists
            // - cek password hash
            // - cek status user (ACTIVE / PENDING / BANNED)
            User user = loginService.loginUser(name, password);

            // 🔐 SECURITY: session fixation protection
            // Hapus session lama agar ID session tidak bisa disalahgunakan
            session.invalidate();

            // Buat session baru
            HttpSession newSession = request.getSession(true);

            // Simpan user login ke session
            newSession.setAttribute("user", user);

            // Redirect ke dashboard sesuai role user
            return "redirect:" + loginService.getRedirectUrlByRole(user.getRole());

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Error validasi login (password salah, akun belum aktif, dll)
            model.addAttribute("error", e.getMessage());

            // Kirim kembali name agar user tidak perlu ketik ulang
            model.addAttribute("name", name);

            return "access/login";

        } catch (Exception e) {
            // Error tak terduga (DB down, dll)
            model.addAttribute("error", "Terjadi kesalahan sistem");
            return "access/login";
        }
    }

    // ================= REGISTER =================

    // Menampilkan halaman register
    @GetMapping("/register")
    public String showRegisterPage(Model model) {

        // Kirim object User kosong ke form
        model.addAttribute("user", new User());

        return "access/register";
    }

    // Memproses registrasi user baru
    @PostMapping("/register")
    public String handleRegistration(
            // Data user dari form register
            @ModelAttribute User user,
            Model model) {

        try {
            // Registrasi user melalui service
            // Biasanya:
            // - hash password
            // - set role default STAFF
            // - set status PENDING
            User registeredUser = loginService.registerUser(user);

            // Pesan sukses (akun menunggu approval admin)
            model.addAttribute(
                    "success",
                    "Registrasi berhasil! Akun Anda (" + registeredUser.getEmail() +
                            ") menunggu persetujuan admin.");

            // Kembali ke halaman login
            return "access/login";

        } catch (IllegalArgumentException e) {
            // Error validasi input (email duplicate, password lemah, dll)
            model.addAttribute("error", e.getMessage());

            // Kirim kembali data user agar form tidak kosong
            model.addAttribute("user", user);

            return "access/register";

        } catch (Exception e) {
            // Error sistem
            model.addAttribute("error", "Terjadi kesalahan sistem");
            model.addAttribute("user", user);
            return "access/register";
        }
    }

    // ================= ADMIN DASHBOARD =================

    // Endpoint dashboard admin
    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(HttpSession session, Model model) {

        // Ambil user dari session
        User user = (User) session.getAttribute("user");

        // Validasi login
        if (user == null) {
            return "redirect:/access/login";
        }

        // 🔥 STRICT ROLE CHECK
        // Jika user bukan ADMIN, redirect ke dashboard sesuai role
        if (user.getRole() != Role.ADMIN) {
            return "redirect:" + loginService.getRedirectUrlByRole(user.getRole());
        }

        // Kirim user ke view
        model.addAttribute("user", user);

        return "access/admin-dashboard";
    }

    // ================= STAFF DASHBOARD =================

    // Endpoint dashboard staff
    @GetMapping("/staff/dashboard")
    public String showStaffDashboard(HttpSession session, Model model) {

        // Ambil user dari session
        User user = (User) session.getAttribute("user");

        // Validasi login
        if (user == null) {
            return "redirect:/access/login";
        }

        // 🔥 STRICT ROLE CHECK
        // Staff hanya boleh akses dashboard staff
        if (user.getRole() != Role.STAFF) {
            return "redirect:" + loginService.getRedirectUrlByRole(user.getRole());
        }

        // Kirim user ke view
        model.addAttribute("user", user);

        return "access/staff-dashboard";
    }

    // ================= LOGOUT =================

    // Proses logout
    @GetMapping("/logout")
    public String handleLogout(HttpSession session) {

        // Hapus session user
        session.invalidate();

        // Redirect ke halaman login
        return "redirect:/access/login";
    }
}
