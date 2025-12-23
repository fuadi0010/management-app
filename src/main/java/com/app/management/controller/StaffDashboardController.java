package com.app.management.controller;

// Spring MVC annotations
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Menandakan class ini adalah MVC Controller
@Controller

// Base path khusus untuk staff
@RequestMapping("/staff")
public class StaffDashboardController {

    // ======================
    // STAFF DASHBOARD
    // ======================

    // Endpoint untuk menampilkan dashboard staff
    @GetMapping("/dashboard")
    public String staffDashboard(Model model) {

        // Judul halaman (digunakan di UI)
        model.addAttribute("pageTitle", "Staff Dashboard");

        // Subtitle halaman
        model.addAttribute("pageSubtitle", "Operational System");

        // Role user (digunakan untuk conditional rendering UI)
        model.addAttribute("userRole", "staff");

        // URL dashboard (untuk navigasi & tombol home)
        model.addAttribute("dashboardUrl", "/staff/dashboard");

        // URL tombol kembali universal
        model.addAttribute("backUrl", "/main/back");

        // Return ke template dashboard staff
        return "access/staff-dashboard";
    }
}
