package com.app.management.controller;

// Import untuk handling tanggal (filter laporan)
import java.time.LocalDate;

// Map digunakan untuk menampung data dinamis hasil perhitungan laporan
import java.util.Map;

// Spring dependency injection & MVC
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Import role & user untuk validasi hak akses
import com.app.management.model.user.Role;
import com.app.management.model.user.User;

// Service khusus untuk business logic laporan keuangan
import com.app.management.service.FinanceReportService;

// HTTP response untuk streaming file PDF
import jakarta.servlet.http.HttpServletResponse;

// HttpSession untuk session-based authentication
import jakarta.servlet.http.HttpSession;

// Menandakan class ini adalah MVC Controller
@Controller

// Base URL untuk seluruh endpoint laporan
@RequestMapping("/report")
public class FinanceReportController {

    // Inject service laporan keuangan
    // Controller hanya bertugas orchestration
    @Autowired
    private FinanceReportService financeReportService;

    // ======================
    // PREVIEW FINANCE REPORT
    // ======================

    // Endpoint untuk preview laporan keuangan sebelum dicetak
    @GetMapping("/finance/preview")
    public String previewFinanceReport(
            // Parameter tanggal awal (opsional)
            @RequestParam(required = false) LocalDate startDate,

            // Parameter tanggal akhir (opsional)
            @RequestParam(required = false) LocalDate endDate,

            // Model untuk mengirim data ke view
            Model model,

            // Session untuk cek user login
            HttpSession session) {

        // Ambil user dari session
        User user = (User) session.getAttribute("user");

        // Validasi keamanan:
        // - user belum login
        // - atau bukan ADMIN
        if (user == null || user.getRole() != Role.ADMIN) {
            return "redirect:/access/login";
        }

        // Kirim kembali tanggal ke view agar tetap tampil di form
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        // Preview hanya diproses jika kedua tanggal tersedia
        if (startDate != null && endDate != null) {
            try {
                // Ambil data preview dari service
                // Biasanya berisi:
                // - total pemasukan
                // - total pengeluaran
                // - laba / rugi
                // - rekap transaksi
                Map<String, Object> previewData =
                        financeReportService.getPreviewData(startDate, endDate);

                // Masukkan seluruh data preview ke model
                model.addAllAttributes(previewData);

                // Flag untuk menandakan preview siap ditampilkan
                model.addAttribute("previewReady", true);

            } catch (IllegalArgumentException e) {
                // Tangkap error validasi tanggal
                // Contoh: startDate > endDate
                model.addAttribute("errorMessage", e.getMessage());
            }
        }

        // Return ke halaman preview laporan keuangan
        return "report/finance-preview";
    }

    // ==========================
    // CETAK SETELAH PREVIEW DULU
    // ==========================

    // Endpoint untuk generate & download PDF laporan keuangan
    @GetMapping("/finance/pdf")
    public void exportFinancePdf(
            // Parameter tanggal awal (wajib)
            @RequestParam LocalDate startDate,

            // Parameter tanggal akhir (wajib)
            @RequestParam LocalDate endDate,

            // Response digunakan untuk streaming file PDF
            HttpServletResponse response,

            // Session untuk validasi admin
            HttpSession session) throws Exception {

        // Ambil user dari session
        User user = (User) session.getAttribute("user");

        // Validasi keamanan:
        // Endpoint ini tidak return view,
        // jadi unauthorized dilempar sebagai exception
        if (user == null || user.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Unauthorized");
        }

        // Set MIME type sebagai PDF
        response.setContentType("application/pdf");

        // Header agar browser langsung download file
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=finance-report.pdf");

        // Delegasi pembuatan PDF ke service
        // OutputStream langsung diarahkan ke response
        financeReportService.generatePdf(
                startDate,
                endDate,
                response.getOutputStream());
    }
}
