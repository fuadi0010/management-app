package com.app.management.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.management.model.Supplier;
import com.app.management.model.user.User;
import com.app.management.service.SupplierService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/supplier")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    // =========================
    // HELPER (TIDAK DIUBAH)
    // =========================
    private String getDashboardUrl(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            return "/admin/dashboard";
        } else {
            return "/access/staff/dashboard";
        }
    }

    // =========================
    // LIST SUPPLIER
    // =========================
    @GetMapping("/list")
    public String listSupplier(
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "id_desc") String sort,
            Model model) {

        model.addAttribute(
                "supplier",
                supplierService.searchAndSort(keyword, sort));

        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));
        model.addAttribute("backUrl", "/main/back?role=" + role);

        return "supplier/list-supplier";
    }

    // =========================
    // FORM ADD SUPPLIER
    // =========================
    @GetMapping("/add")
    public String showAddSupplierForm(
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            Model model) {

        model.addAttribute("supplier", new Supplier());
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));
        model.addAttribute("backUrl", "/supplier/list?role=" + role);
        return "supplier/add-supplier";
    }

    // =========================
    // SAVE SUPPLIER
    // =========================
    @PostMapping("/add")
    public String saveSupplier(
            @Valid @ModelAttribute("supplier") Supplier supplier,
            BindingResult result,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        User user = (User) session.getAttribute("user");
        if (user != null) {
            role = user.getRole().name().toLowerCase();
        }

        if (result.hasErrors()) {
            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("backUrl", "/supplier/list?role=" + role);
            return "supplier/add-supplier";
        }

        supplierService.saveSupplier(supplier);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Supplier berhasil ditambahkan!");

        return "redirect:/supplier/list?role=" + role;
    }

    // =========================
    // FORM EDIT SUPPLIER (FIX)
    // =========================
    @GetMapping("/edit-supplier/{id}")
    public String showUpdateSupplierForm(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            Model model) {

        Supplier supplier = supplierService.getSupplierById(id);
        if (supplier == null) {
            return "redirect:/supplier/list?role=" + role;
        }

        model.addAttribute("supplier", supplier);
        model.addAttribute("userRole", role);
        return "supplier/update-supplier";
    }

    // =========================
    // UPDATE SUPPLIER (INI YANG HILANG SEBELUMNYA)
    // =========================
    @PostMapping("/edit-supplier")
    public String updateSupplier(
            @ModelAttribute("supplier") Supplier supplier,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            RedirectAttributes redirectAttributes) {

        supplierService.updateSupplier(supplier.getId(), supplier);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Supplier berhasil diupdate");

        return "redirect:/supplier/list?role=" + role;
    }
}
