package com.app.management.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.management.model.user.Role;
import com.app.management.model.user.User;
import com.app.management.service.FinanceReportService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/report")
public class FinanceReportController {

    @Autowired
    private FinanceReportService financeReportService;

    /*
     * =====================================================
     * PREVIEW PAGE (WAJIB LEWAT SINI SEBELUM CETAK)
     * =====================================================
     */
    @GetMapping("/finance/preview")
    public String previewFinanceReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Model model,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != Role.ADMIN) {
            return "redirect:/access/login";
        }

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        if (startDate != null && endDate != null) {
            try {
                Map<String, Object> previewData = financeReportService.getPreviewData(startDate, endDate);

                model.addAllAttributes(previewData);
                model.addAttribute("previewReady", true);

            } catch (IllegalArgumentException e) {
                // ⬇️ INI KUNCINYA
                model.addAttribute("errorMessage", e.getMessage());
            }
        }

        return "report/finance-preview";
    }

    /*
     * =====================================================
     * CETAK PDF (HANYA DIAKSES DARI PREVIEW)
     * =====================================================
     */
    @GetMapping("/finance/pdf")
    public void exportFinancePdf(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            HttpServletResponse response,
            HttpSession session) throws Exception {

        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Unauthorized");
        }

        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=finance-report.pdf");

        financeReportService.generatePdf(
                startDate,
                endDate,
                response.getOutputStream());
    }
}
