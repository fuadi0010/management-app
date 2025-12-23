package com.app.management.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.management.model.sales.InvoiceDetails;
import com.app.management.model.sales.SalesInvoice;
import com.app.management.model.user.User;
import com.app.management.service.InvoiceService;
import com.app.management.service.ProductService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/sales")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ProductService productService;

    private String getDashboardUrl(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            return "/admin/dashboard";
        } else {
            return "/access/staff/dashboard";
        }
    }

    // Endpoint untuk menampilkan daftar invoice dengan fitur pencarian dan pengurutan
    @GetMapping("/list")
    public String listInvoice(
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "date_desc") String sort,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/access/login";
        }

        model.addAttribute(
                "invoices",
                invoiceService.searchAndSort(keyword, sort));

        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));
        model.addAttribute("backUrl", "/main/back?role=" + role);

        return "sales/list-invoice";
    }

    // Endpoint untuk menampilkan form pembuatan invoice penjualan baru
    @GetMapping("/add")
    public String showForm(
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/access/login";
        }

        SalesInvoice invoice = new SalesInvoice();
        invoice.addInvoiceDetail(new InvoiceDetails());

        model.addAttribute("salesInvoice", invoice);
        model.addAttribute("products",
                productService.getActiveProducts(user));
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));
        model.addAttribute("backUrl", "/sales/list?role=" + role);

        return "sales/add-invoice";
    }

    // Endpoint untuk menyimpan invoice penjualan baru ke dalam sistem
    @PostMapping("/add")
    public String saveInvoice(
            @ModelAttribute SalesInvoice salesInvoice,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/access/login";
        }

        try {
            if (salesInvoice.getInvoiceDetails() == null
                    || salesInvoice.getInvoiceDetails().isEmpty()) {

                model.addAttribute("error", "Minimal 1 produk harus dipilih");
                model.addAttribute("products",
                        productService.getActiveProducts(user));
                model.addAttribute("userRole", role);
                model.addAttribute("dashboardUrl", getDashboardUrl(role));
                model.addAttribute("backUrl", "/sales/list?role=" + role);

                return "sales/add-invoice";
            }

            invoiceService.createInvoice(salesInvoice);

            redirectAttributes.addFlashAttribute(
                    "success", "Invoice berhasil dibuat!");

            return "redirect:/sales/list?role=" + role;

        } catch (Exception e) {
            model.addAttribute("error", "Gagal: " + e.getMessage());
            model.addAttribute("products",
                    productService.getActiveProducts(user));
            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("backUrl", "/sales/list?role=" + role);

            return "sales/add-invoice";
        }
    }

    // Endpoint untuk menampilkan detail satu invoice penjualan
    @GetMapping("/view/{id}")
    public String viewInvoice(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/access/login";
        }

        SalesInvoice invoice = invoiceService.getSalesInvoiceByid(id);

        if (invoice == null) {
            return "redirect:/sales/list?role=" + role;
        }

        model.addAttribute("invoice", invoice);
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));
        model.addAttribute("backUrl", "/sales/list?role=" + role);

        return "sales/view-invoice";
    }

    // Endpoint untuk menyelesaikan proses invoice penjualan
    @GetMapping("/complete/{id}")
    public String completeInvoice(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            RedirectAttributes redirectAttributes) {

        try {
            invoiceService.completeSales(id);
            redirectAttributes.addFlashAttribute(
                    "success", "Invoice berhasil diselesaikan!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "Gagal: " + e.getMessage());
        }

        return "redirect:/sales/list?role=" + role;
    }

    // Endpoint untuk membatalkan invoice penjualan yang sudah dibuat
    @GetMapping("/cancel/{id}")
    public String cancelInvoice(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            RedirectAttributes redirectAttributes) {

        try {
            invoiceService.cancelSales(id);
            redirectAttributes.addFlashAttribute(
                    "success", "Invoice berhasil dibatalkan!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "Gagal: " + e.getMessage());
        }

        return "redirect:/sales/list?role=" + role;
    }
}
