package com.app.management.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.management.model.purchase.Purchase;
import com.app.management.model.purchase.PurchaseDetails;
import com.app.management.model.purchase.PurchaseStatus;
import com.app.management.model.user.User;
import com.app.management.service.PurchaseService;
import com.app.management.service.SupplierService;

import jakarta.servlet.http.HttpSession;

import com.app.management.service.ProductService;

import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/purchase")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private ProductService productService;

    private String getDashboardUrl(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            return "/admin/dashboard";
        } else {
            return "/access/staff/dashboard";
        }
    }

    @GetMapping("/list")
    public String listPurchase(
            @RequestParam String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "date_desc") String sort,
            Model model) {

        // ===== dashboardUrl FIX (DITAMBAHKAN) =====
        String dashboardUrl;
        if ("admin".equalsIgnoreCase(role)) {
            dashboardUrl = "/admin/dashboard";
        } else {
            dashboardUrl = "/staff/dashboard";
        }

        // ===== logic lama (TIDAK DIUBAH) =====
        List<Purchase> purchases = purchaseService.searchAndSort(keyword, sort);

        model.addAttribute("purchases", purchases);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", dashboardUrl); // ← FIX DI SINI

        return "purchase/list-purchase";
    }

    @GetMapping("/add")
    public String addPurchase(
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/access/login";
        }

        Purchase purchase = new Purchase();
        purchase.addPurchaseDetail(new PurchaseDetails());

        model.addAttribute("purchase", purchase);
        model.addAttribute("suppliers", supplierService.getAllSuppliers());

        // 🔒 PERBAIKAN UTAMA DI SINI
        model.addAttribute("products",
                productService.getActiveProducts(user));

        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));

        return "purchase/add-purchase";
    }

    @PostMapping("/add")
    public String savePurchase(
            @ModelAttribute Purchase purchase,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/access/login";
        }

        try {
            // === VALIDASI DASAR (sementara di controller) ===
            if (purchase.getSupplier() == null || purchase.getSupplier().getId() == null) {
                throw new IllegalArgumentException("Supplier harus dipilih");
            }

            if (purchase.getPurchaseDetails() == null || purchase.getPurchaseDetails().isEmpty()) {
                throw new IllegalArgumentException("Minimal satu produk harus ditambahkan");
            }

            purchase.setStatus(PurchaseStatus.CREATED);

            BigDecimal total = BigDecimal.ZERO;

            for (PurchaseDetails detail : purchase.getPurchaseDetails()) {

                if (detail.getProduct() == null || detail.getProduct().getId() == null) {
                    continue;
                }

                if (detail.getQuantity() == null || detail.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Quantity harus lebih dari 0");
                }

                if (detail.getUnitPurchasePrice() == null
                        || detail.getUnitPurchasePrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Harga beli harus lebih dari 0");
                }

                detail.setPurchase(purchase);

                BigDecimal subtotal = detail.getUnitPurchasePrice()
                        .multiply(BigDecimal.valueOf(detail.getQuantity()));

                detail.setSubtotal(subtotal);
                total = total.add(subtotal);
            }

            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Total pembelian harus lebih dari 0");
            }

            purchase.setTotalPurchase(total);

            purchaseService.createPurchase(purchase);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Pembelian berhasil disimpan!");

            return "redirect:/purchase/list?role=" + role;

        } catch (Exception e) {
            model.addAttribute("error", "Gagal menyimpan: " + e.getMessage());
            model.addAttribute("suppliers", supplierService.getAllSuppliers());

            // 🔒 PERBAIKAN UTAMA JUGA DI SINI
            model.addAttribute("products",
                    productService.getActiveProducts(user));

            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("backUrl", "/purchase/list?role=" + role);

            return "purchase/add-purchase";
        }
    }

    @GetMapping("/view/{id}")
    public String viewPurchase(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            Model model) {

        Purchase purchase = purchaseService.getPurchaseByid(id);
        if (purchase == null) {
            return "redirect:/purchase/list?role=" + role;
        }

        model.addAttribute("purchase", purchase);
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));
        model.addAttribute("backUrl", "/purchase/list?role=" + role);
        return "purchase/purchase-detail";
    }

    @GetMapping("/complete/{id}")
    public String completePurchase(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            RedirectAttributes redirectAttributes) {

        boolean completed = purchaseService.completePurchase(id);
        if (completed) {
            redirectAttributes.addFlashAttribute("successMessage", "Pembelian selesai!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal menyelesaikan pembelian");
        }
        return "redirect:/purchase/list?role=" + role;
    }

    @GetMapping("/cancel/{id}")
    public String cancelPurchase(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            RedirectAttributes redirectAttributes) {

        boolean cancelled = purchaseService.cancelPurchase(id);
        if (cancelled) {
            redirectAttributes.addFlashAttribute("successMessage", "Pembelian dibatalkan!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal membatalkan pembelian");
        }
        return "redirect:/purchase/list?role=" + role;
    }
}