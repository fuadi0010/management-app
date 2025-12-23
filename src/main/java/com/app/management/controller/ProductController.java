package com.app.management.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.management.model.product.Product;
import com.app.management.model.product.ProductStatus;
import com.app.management.model.user.User;
import com.app.management.service.ProductService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    private String getDashboardUrl(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            return "/admin/dashboard";
        } else {
            return "/access/staff/dashboard";
        }
    }

    private String getFormAction(Product product, String role) {
        if (product.getId() != null) {
            return "/product/update";
        } else {
            return "/product/add";
        }
    }

    // Endpoint untuk menampilkan daftar produk dengan fitur pencarian, filter status, dan sorting
    @GetMapping("/list")
    public String listProduct(
            @RequestParam String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "name_asc") String sort,
            Model model) {

        List<Product> products =
                productService.searchAndSortProduct(keyword, status, sort);

        String dashboardUrl;
        if ("admin".equalsIgnoreCase(role)) {
            dashboardUrl = "/admin/dashboard";
        } else {
            dashboardUrl = "/staff/dashboard";
        }

        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", dashboardUrl);

        return "product/list-product";
    }

    // Endpoint untuk menampilkan form tambah atau edit produk
    @GetMapping("/add")
    public String showProductForm(
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            @RequestParam(value = "id", required = false) Long id,
            Model model) {

        Product product;

        if (id != null) {
            product = productService.getProductById(id);
            if (product == null) {
                return "redirect:/product/list?role=" + role;
            }
        } else {
            product = new Product();
        }

        model.addAttribute("product", product);
        model.addAttribute("userRole", role);
        model.addAttribute("dashboardUrl", getDashboardUrl(role));
        model.addAttribute("formAction", getFormAction(product, role));

        return "product/add-product";
    }

    // Endpoint untuk menyimpan data produk baru
    @PostMapping("/add")
    public String saveProduct(
            @Valid @ModelAttribute("product") Product product,
            BindingResult result,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("formAction", "/product/add");
            return "product/add-product";
        }

        try {
            productService.saveProduct(product);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Produk berhasil ditambahkan!");

            return "redirect:/product/list?role=" + role;

        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan: " + e.getMessage());
            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("formAction", "/product/add");
            return "product/add-product";
        }
    }

    // Endpoint untuk memperbarui data produk yang sudah ada
    @PostMapping("/update")
    public String updateProduct(
            @Valid @ModelAttribute("product") Product product,
            BindingResult result,
            @RequestParam(value = "role", required = false, defaultValue = "staff") String role,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("formAction", "/product/update");
            return "product/add-product";
        }

        try {
            productService.updateProductInfo(product.getId(), product);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Product berhasil diupdate");

            return "redirect:/product/list?role=" + role;

        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan: " + e.getMessage());
            model.addAttribute("userRole", role);
            model.addAttribute("dashboardUrl", getDashboardUrl(role));
            model.addAttribute("formAction", "/product/update");
            return "product/add-product";
        }
    }

    // Endpoint untuk menampilkan halaman manajemen produk aktif dan tersembunyi
    @GetMapping("/manage")
    public String manageProduct(
            @RequestParam(required = false) String activeKeyword,
            @RequestParam(defaultValue = "name_asc") String activeSort,
            @RequestParam(required = false) String hiddenKeyword,
            @RequestParam(defaultValue = "name_asc") String hiddenSort,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);

        model.addAttribute(
                "activeProducts",
                productService.searchSortByStatus(
                        ProductStatus.ACTIVE,
                        activeKeyword,
                        activeSort));

        model.addAttribute(
                "hiddenProducts",
                productService.searchSortByStatus(
                        ProductStatus.HIDDEN,
                        hiddenKeyword,
                        hiddenSort));

        model.addAttribute("activeKeyword", activeKeyword);
        model.addAttribute("activeSort", activeSort);
        model.addAttribute("hiddenKeyword", hiddenKeyword);
        model.addAttribute("hiddenSort", hiddenSort);

        return "hidden/hidden";
    }

    // Endpoint untuk menyembunyikan produk agar tidak tampil di sistem
    @GetMapping("/hide/{id}")
    public String hideProduct(
            @PathVariable Long id,
            HttpSession session) {

        User admin = (User) session.getAttribute("user");
        productService.hideProduct(id, admin);

        return "redirect:/product/manage";
    }

    // Endpoint untuk menampilkan kembali produk yang sebelumnya disembunyikan
    @GetMapping("/unhide/{id}")
    public String unhideProduct(
            @PathVariable Long id,
            HttpSession session) {

        User admin = (User) session.getAttribute("user");
        productService.unhideProduct(id, admin);

        return "redirect:/product/manage";
    }
}
