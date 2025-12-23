package com.app.management.controller;

// Spring MVC annotations
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// HttpServletRequest digunakan untuk membaca header HTTP (Referer)
import jakarta.servlet.http.HttpServletRequest;

// Menandakan class ini adalah MVC Controller
@Controller

// Base path untuk routing utama (navigation controller)
@RequestMapping("/main")
public class MainController {

    // ================= DASHBOARD =================

    // Redirect ke dashboard sesuai role
    @GetMapping("/dashboard")
    public String redirectToDashboard(
            // Role dikirim melalui query parameter
            @RequestParam(value = "role", required = false) String role) {

        // Jika role admin → dashboard admin
        if ("admin".equals(role)) {
            return "redirect:/admin/dashboard";
        } else {
            // Default ke dashboard staff
            return "redirect:/staff/dashboard";
        }
    }

    // ================= PRODUCTS =================

    // Redirect ke halaman produk sesuai role
    @GetMapping("/products")
    public String redirectToProducts(
            @RequestParam(value = "role", required = false) String role) {

        // Admin memiliki akses penuh
        if ("admin".equals(role)) {
            return "redirect:/product/list?role=admin";
        } else {
            // Staff dibatasi sesuai role
            return "redirect:/product/list?role=staff";
        }
    }

    // ================= INVOICES =================

    // Redirect ke halaman invoice sesuai role
    @GetMapping("/invoices")
    public String redirectToInvoices(
            @RequestParam(value = "role", required = false) String role) {

        if ("admin".equals(role)) {
            return "redirect:/sales/list?role=admin";
        } else {
            return "redirect:/sales/list?role=staff";
        }
    }

    // ================= PURCHASES =================

    // Redirect ke halaman pembelian sesuai role
    @GetMapping("/purchases")
    public String redirectToPurchases(
            @RequestParam(value = "role", required = false) String role) {

        if ("admin".equals(role)) {
            return "redirect:/purchase/list?role=admin";
        } else {
            return "redirect:/purchase/list?role=staff";
        }
    }

    // ================= SUPPLIERS =================

    // Redirect ke halaman supplier sesuai role
    @GetMapping("/suppliers")
    public String redirectToSuppliers(
            @RequestParam(value = "role", required = false) String role) {

        if ("admin".equals(role)) {
            return "redirect:/supplier/list?role=admin";
        } else {
            return "redirect:/supplier/list?role=staff";
        }
    }

    // ================= UNIVERSAL BACK BUTTON =================

    // Tombol kembali universal (digunakan di banyak halaman)
    @GetMapping("/back")
    public String backToDashboard(HttpServletRequest request) {

        // Ambil URL halaman sebelumnya dari HTTP header "Referer"
        String referer = request.getHeader("Referer");

        // Jika referer mengandung informasi role
        if (referer != null && referer.contains("role=")) {

            // Jika role admin ditemukan di URL sebelumnya
            if (referer.contains("role=admin")) {
                return "redirect:/admin/dashboard";

            // Jika role staff ditemukan di URL sebelumnya
            } else if (referer.contains("role=staff")) {
                return "redirect:/access/staff/dashboard";
            }
        }

        // Fallback default jika referer tidak tersedia
        return "redirect:/staff/dashboard";
    }
}
