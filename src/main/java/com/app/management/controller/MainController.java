package com.app.management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/main")
public class MainController {

    // Endpoint untuk mengarahkan user ke dashboard sesuai role
    @GetMapping("/dashboard")
    public String redirectToDashboard(
            @RequestParam(value = "role", required = false) String role) {

        if ("admin".equals(role)) {
            return "redirect:/admin/dashboard";
        } else {
            return "redirect:/staff/dashboard";
        }
    }

    // Endpoint untuk mengarahkan user ke halaman produk sesuai role
    @GetMapping("/products")
    public String redirectToProducts(
            @RequestParam(value = "role", required = false) String role) {

        if ("admin".equals(role)) {
            return "redirect:/product/list?role=admin";
        } else {
            return "redirect:/product/list?role=staff";
        }
    }

    // Endpoint untuk mengarahkan user ke halaman invoice sesuai role
    @GetMapping("/invoices")
    public String redirectToInvoices(
            @RequestParam(value = "role", required = false) String role) {

        if ("admin".equals(role)) {
            return "redirect:/sales/list?role=admin";
        } else {
            return "redirect:/sales/list?role=staff";
        }
    }

    // Endpoint untuk mengarahkan user ke halaman pembelian sesuai role
    @GetMapping("/purchases")
    public String redirectToPurchases(
            @RequestParam(value = "role", required = false) String role) {

        if ("admin".equals(role)) {
            return "redirect:/purchase/list?role=admin";
        } else {
            return "redirect:/purchase/list?role=staff";
        }
    }

    // Endpoint untuk mengarahkan user ke halaman supplier sesuai role
    @GetMapping("/suppliers")
    public String redirectToSuppliers(
            @RequestParam(value = "role", required = false) String role) {

        if ("admin".equals(role)) {
            return "redirect:/supplier/list?role=admin";
        } else {
            return "redirect:/supplier/list?role=staff";
        }
    }

    // Endpoint untuk mengembalikan user ke dashboard berdasarkan halaman sebelumnya
    @GetMapping("/back")
    public String backToDashboard(HttpServletRequest request) {

        String referer = request.getHeader("Referer");

        if (referer != null && referer.contains("role=")) {

            if (referer.contains("role=admin")) {
                return "redirect:/admin/dashboard";

            } else if (referer.contains("role=staff")) {
                return "redirect:/access/staff/dashboard";
            }
        }

        return "redirect:/staff/dashboard";
    }
}
