package com.app.management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/main")
public class MainController {

    @GetMapping("/dashboard")
    public String redirectToDashboard(@RequestParam(value = "role", required = false) String role) {
        if ("admin".equals(role)) {
            return "redirect:/admin/dashboard";
        } else {
            return "redirect:/staff/dashboard";
        }
    }

    @GetMapping("/products")
    public String redirectToProducts(@RequestParam(value = "role", required = false) String role) {
        if ("admin".equals(role)) {
            return "redirect:/product/list?role=admin";
        } else {
            return "redirect:/product/list?role=staff";
        }
    }

    @GetMapping("/invoices")
    public String redirectToInvoices(@RequestParam(value = "role", required = false) String role) {
        if ("admin".equals(role)) {
            return "redirect:/sales/list?role=admin";
        } else {
            return "redirect:/sales/list?role=staff";
        }
    }

    @GetMapping("/purchases")
    public String redirectToPurchases(@RequestParam(value = "role", required = false) String role) {
        if ("admin".equals(role)) {
            return "redirect:/purchase/list?role=admin";
        } else {
            return "redirect:/purchase/list?role=staff";
        }
    }

    @GetMapping("/suppliers")
    public String redirectToSuppliers(@RequestParam(value = "role", required = false) String role) {
        if ("admin".equals(role)) {
            return "redirect:/supplier/list?role=admin";
        } else {
            return "redirect:/supplier/list?role=staff";
        }
    }

    // Tombol kembali universal
    @GetMapping("/back")
    public String backToDashboard(HttpServletRequest request) {
        // Ambil role dari referer atau session
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