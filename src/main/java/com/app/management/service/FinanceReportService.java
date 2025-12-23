package com.app.management.service;

// OutputStream digunakan untuk streaming PDF ke HTTP response
import java.io.OutputStream;

// BigDecimal untuk perhitungan uang (precision-safe)
import java.math.BigDecimal;

// LocalDate & LocalDateTime untuk filter tanggal
import java.time.LocalDate;
import java.time.LocalDateTime;

// Collection & Map untuk menampung data preview
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Spring DI
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Entity purchase & status
import com.app.management.model.purchase.Purchase;
import com.app.management.model.purchase.PurchaseStatus;

// Entity sales & status
import com.app.management.model.sales.SalesInvoice;
import com.app.management.model.sales.SalesStatus;

// Repository layer
import com.app.management.repository.PurchaseRepository;
import com.app.management.repository.SalesInvoiceRepository;

// Library PDF (iText / lowagie)
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

// Menandakan class ini adalah Service (business logic)
@Service
public class FinanceReportService {

        // Repository invoice penjualan
        @Autowired
        private SalesInvoiceRepository salesInvoiceRepository;

        // Repository pembelian
        @Autowired
        private PurchaseRepository purchaseRepository;

        /*
         * =====================================================
         * PREVIEW DATA (TANPA DTO)
         * =====================================================
         */
        public Map<String, Object> getPreviewData(LocalDate start, LocalDate end) {

                // Validasi range tanggal
                if (start.isAfter(end)) {
                        throw new IllegalArgumentException("Start date must be before end date");
                }

                // Konversi LocalDate → LocalDateTime (full day range)
                LocalDateTime startDT = start.atStartOfDay();
                LocalDateTime endDT = end.atTime(23, 59, 59);

                // Ambil invoice penjualan yang sudah COMPLETED
                List<SalesInvoice> incomes = salesInvoiceRepository
                                .findBySalesStatusAndInvoiceDateBetween(
                                                SalesStatus.COMPLETED,
                                                startDT,
                                                endDT);

                // Ambil purchase yang sudah COMPLETED
                List<Purchase> expenses = purchaseRepository
                                .findByStatusAndPurchaseDateBetween(
                                                PurchaseStatus.COMPLETED,
                                                startDT,
                                                endDT);

                // Hitung total pemasukan (SUM totalSales)
                BigDecimal totalIncome = incomes.stream()
                                .map(SalesInvoice::getTotalSales)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Hitung total pengeluaran (SUM totalPurchase)
                BigDecimal totalExpense = expenses.stream()
                                .map(Purchase::getTotalPurchase)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Gunakan Map agar fleksibel dikirim ke view (tanpa DTO)
                Map<String, Object> map = new HashMap<>();

                map.put("totalIncome", totalIncome);
                map.put("totalExpense", totalExpense);

                // Saldo bersih = pemasukan - pengeluaran
                map.put("netBalance", totalIncome.subtract(totalExpense));

                // Statistik jumlah transaksi
                map.put("incomeCount", incomes.size());
                map.put("expenseCount", expenses.size());

                return map;
        }

        /*
         * =====================================================
         * GENERATE PDF (FULL JAVA)
         * =====================================================
         */
        public void generatePdf(
                        LocalDate start,
                        LocalDate end,
                        OutputStream out) throws Exception {

                // Validasi range tanggal
                if (start.isAfter(end)) {
                        throw new IllegalArgumentException("Start date must be before end date");
                }

                // Konversi tanggal ke full datetime range
                LocalDateTime startDT = start.atStartOfDay();
                LocalDateTime endDT = end.atTime(23, 59, 59);

                // Ambil data pemasukan
                List<SalesInvoice> incomes = salesInvoiceRepository
                                .findBySalesStatusAndInvoiceDateBetween(
                                                SalesStatus.COMPLETED,
                                                startDT,
                                                endDT);

                // Ambil data pengeluaran
                List<Purchase> expenses = purchaseRepository
                                .findByStatusAndPurchaseDateBetween(
                                                PurchaseStatus.COMPLETED,
                                                startDT,
                                                endDT);

                // Hitung total pemasukan
                BigDecimal totalIncome = incomes.stream()
                                .map(SalesInvoice::getTotalSales)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Hitung total pengeluaran
                BigDecimal totalExpense = expenses.stream()
                                .map(Purchase::getTotalPurchase)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Inisialisasi dokumen PDF ukuran A4
                Document document = new Document(PageSize.A4);

                // Hubungkan document dengan output stream
                PdfWriter.getInstance(document, out);

                document.open();

                // Definisi font
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

                // Judul laporan
                document.add(new Paragraph("LAPORAN KEUANGAN", titleFont));
                document.add(new Paragraph(
                                "Periode: " + start + " s/d " + end,
                                normalFont));

                document.add(Chunk.NEWLINE);

                // ===== PEMASUKAN =====
                document.add(new Paragraph("PEMASUKAN", headerFont));

                PdfPTable incomeTable = new PdfPTable(3);
                incomeTable.setWidthPercentage(100);

                // Header tabel pemasukan
                incomeTable.addCell("Tanggal");
                incomeTable.addCell("Invoice");
                incomeTable.addCell("Total");

                // Data pemasukan per invoice
                for (SalesInvoice s : incomes) {
                        incomeTable.addCell(
                                        s.getInvoiceDate()
                                                        .toLocalDate()
                                                        .toString());
                        incomeTable.addCell(s.getInvoiceNumber());
                        incomeTable.addCell("Rp " + s.getTotalSales());
                }

                document.add(incomeTable);

                // Total pemasukan
                document.add(
                                new Paragraph(
                                                "Total Pemasukan: Rp " + totalIncome,
                                                headerFont));

                document.add(Chunk.NEWLINE);

                // ===== PENGELUARAN =====
                document.add(new Paragraph("PENGELUARAN", headerFont));

                PdfPTable expenseTable = new PdfPTable(3);
                expenseTable.setWidthPercentage(100);

                // Header tabel pengeluaran
                expenseTable.addCell("Tanggal");
                expenseTable.addCell("Supplier");
                expenseTable.addCell("Total");

                // Data pengeluaran per purchase
                for (Purchase p : expenses) {
                        expenseTable.addCell(
                                        p.getPurchaseDate()
                                                        .toLocalDate()
                                                        .toString());
                        expenseTable.addCell(
                                        p.getSupplier()
                                                        .getSupplierName());
                        expenseTable.addCell(
                                        "Rp " + p.getTotalPurchase());
                }

                document.add(expenseTable);

                // Total pengeluaran
                document.add(
                                new Paragraph(
                                                "Total Pengeluaran: Rp " + totalExpense,
                                                headerFont));

                document.add(Chunk.NEWLINE);

                // Saldo bersih
                document.add(
                                new Paragraph(
                                                "Saldo Bersih: Rp "
                                                                + totalIncome.subtract(totalExpense),
                                                headerFont));

                // Tutup document (flush ke OutputStream)
                document.close();
        }
}
