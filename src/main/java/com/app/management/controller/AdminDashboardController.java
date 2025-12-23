package com.app.management.controller;

import com.app.management.model.user.Role;
import com.app.management.model.user.User;
import com.app.management.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private UserService userService;

    // Endpoint untuk menampilkan dashboard admin beserta data statistik dan user yang menunggu persetujuan
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {

        User admin = (User) session.getAttribute("user");

        if (admin == null || admin.getRole() != Role.ADMIN) {
            return "redirect:/access/login";
        }

        var pendingUsers = userService.getPendingUsers();

        model.addAttribute("user", admin);
        model.addAttribute("users", pendingUsers);
        model.addAttribute("totalUsers", userService.getTotalUserCount());
        model.addAttribute("pendingUsersCount", pendingUsers.size());
        model.addAttribute("activeUsers", userService.getActiveUserCount());

        return "access/admin-dashboard";
    }

    // Endpoint untuk menyetujui user yang masih berstatus pending agar menjadi active
    @GetMapping("/users/approve/{id}")
    public String approve(@PathVariable Long id) {
        userService.approveUser(id);
        return "redirect:/admin/dashboard";
    }

    // Endpoint untuk menolak user yang tidak memenuhi syarat pendaftaran
    @GetMapping("/users/reject/{id}")
    public String reject(@PathVariable Long id) {
        userService.rejectUser(id);
        return "redirect:/admin/dashboard";
    }
}
