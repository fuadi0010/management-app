package com.app.management.controller;

import com.app.management.model.user.Role;
import com.app.management.model.user.User;
import com.app.management.model.user.UserStatus;
import com.app.management.service.UserService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private UserService userService;

    // Endpoint untuk menampilkan halaman manajemen staff beserta daftar staff aktif
    // dan yang dibanned
    @GetMapping
    public String staffManagement(HttpSession session, Model model) {

        User admin = (User) session.getAttribute("user");

        if (admin == null || admin.getRole() != Role.ADMIN) {
            return "redirect:/access/login";
        }

        model.addAttribute("user", admin);

        model.addAttribute(
                "activeStaffs",
                userService.getStaffByStatus(UserStatus.ACTIVE));

        model.addAttribute(
                "bannedStaffs",
                userService.getStaffByStatuses(
                        UserStatus.BANNED,
                        UserStatus.REJECTED));

        return "hidden/staff-management";
    }

    // Endpoint untuk melakukan ban terhadap staff berdasarkan ID
    @PostMapping("/ban/{id}")
    public String banStaff(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra) {

        User admin = (User) session.getAttribute("user");

        try {
            userService.banStaff(id, admin);
            ra.addFlashAttribute("success", "Staff berhasil diban");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    // Endpoint untuk menghapus staff secara permanen setelah dibanned
    @PostMapping("/delete/{id}")
    public String deleteStaff(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra) {

        User admin = (User) session.getAttribute("user");

        try {
            userService.deleteBannedStaff(id, admin);
            ra.addFlashAttribute("success", "Staff berhasil dihapus permanen");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users";
    }
}
