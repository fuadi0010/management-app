package com.app.management.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.management.model.user.Role;
import com.app.management.model.user.User;
import com.app.management.service.LoginService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/access")
public class LoginController {

    @Autowired
    private LoginService loginService;

    // ================= LOGIN =================

    @GetMapping("/login")
    public String showLoginPage() {
        return "access/login";
    }

    @PostMapping("/login")
    public String handleLogin(
            @RequestParam String name,
            @RequestParam String password,
            Model model,
            HttpSession session) {

        try {
            User user = loginService.loginUser(name, password);

            session.setAttribute("user", user);
            session.setAttribute("isLoggedIn", true);

            return "redirect:" + loginService.getRedirectUrlByRole(user.getRole());

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("name", name);
            return "access/login";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan sistem");
            return "access/login";
        }
    }

    // ================= REGISTER =================

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("user", new User());
        return "access/register";
    }

    @PostMapping("/register")
    public String handleRegistration(@ModelAttribute User user, Model model) {
        try {
            User registeredUser = loginService.registerUser(user);

            model.addAttribute(
                "success",
                "Registrasi berhasil! Akun Anda (" + registeredUser.getEmail() +
                ") menunggu persetujuan admin."
            );

            return "access/login";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user);
            return "access/register";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan sistem");
            model.addAttribute("user", user);
            return "access/register";
        }
    }

    // ================= ADMIN DASHBOARD =================

    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/access/login";
        }

        // 🔥 STRICT ROLE CHECK
        if (user.getRole() != Role.ADMIN) {
            // Redirect ke dashboard sesuai role
            return "redirect:" + loginService.getRedirectUrlByRole(user.getRole());
        }

        model.addAttribute("user", user);
        return "access/admin-dashboard";
    }

    // ================= STAFF DASHBOARD =================

    @GetMapping("/staff/dashboard")
    public String showStaffDashboard(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/access/login";
        }

        // 🔥 STRICT ROLE CHECK
        if (user.getRole() != Role.STAFF) {
            // Redirect ke dashboard sesuai role
            return "redirect:" + loginService.getRedirectUrlByRole(user.getRole());
        }

        model.addAttribute("user", user);
        return "access/staff-dashboard";
    }

    // ================= LOGOUT =================

    @GetMapping("/logout")
    public String handleLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/access/login";
    }
}