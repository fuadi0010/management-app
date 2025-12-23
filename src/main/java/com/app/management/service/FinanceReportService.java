package com.app.management.service;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.management.model.purchase.Purchase;
import com.app.management.model.purchase.PurchaseStatus;
import com.app.management.model.sales.SalesInvoice;
import com.app.management.model.sales.SalesStatus;
import com.app.management.repository.PurchaseRepository;
import com.app.management.repository.SalesInvoiceRepository;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class FinanceReportService {

        @Autowired
        private SalesInvoiceRepository salesInvoiceRepository;

        @Autowired
        private PurchaseRepository purchaseRepository;

        // Method untuk mengambil data ringkasan laporan keuangan berdasarkan rentang tanggal
        public Map<String, Object> getPreviewData(LocalDate start, LocalDate end) {

                if (start.isAfter(end)) {
                        throw new IllegalArgumentException("Start date must be before end date");
                }

                LocalDateTime startDT = start.atStartOfDay();
                LocalDateTime endDT = end.atTime(23, 59, 59);

                List<SalesInvoice> incomes = salesInvoiceRepository
                                .findBySalesStatusAndInvoiceDateBetween(
                                                SalesStatus.COMPLETED,
                                                startDT,
                                                endDT);

                List<Purchase> expenses = purchaseRepository
                                .findByStatusAndPurchaseDateBetween(
                                                PurchaseStatus.COMPLETED,
                                                startDT,
                                                endDT);

                BigDecimal totalIncome = incomes.stream()
                                .map(SalesInvoice::getTotalSales)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalExpense = expenses.stream()
                                .map(Purchase::getTotalPurchase)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<String, Object> map = new HashMap<>();

                map.put("totalIncome", totalIncome);
                map.put("totalExpense", totalExpense);
                map.put("netBalance", totalIncome.subtract(totalExpense));
                map.put("incomeCount", incomes.size());
                map.put("expenseCount", expenses.size());

                return map;
        }

        // Method untuk menghasilkan laporan keuangan dalam bentuk PDF berdasarkan rentang tanggal
        public void generatePdf(
                        LocalDate start,
                        LocalDate end,
                        OutputStream out) throws Exception {

                if (start.isAfter(end)) {
                        throw new IllegalArgumentException("Start date must be before end date");
                }

                LocalDateTime startDT = start.atStartOfDay();
                LocalDateTime endDT = end.atTime(23, 59, 59);

                List<SalesInvoice> incomes = salesInvoiceRepository
                                .findBySalesStatusAndInvoiceDateBetween(
                                                SalesStatus.COMPLETED,
                                                startDT,
                                                endDT);

                List<Purchase> expenses = purchaseRepository
                                .findByStatusAndPurchaseDateBetween(
                                                PurchaseStatus.COMPLETED,
                                                startDT,
                                                endDT);

                BigDecimal totalIncome = incomes.stream()
                                .map(SalesInvoice::getTotalSales)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalExpense = expenses.stream()
                                .map(Purchase::getTotalPurchase)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                Document document = new Document(PageSize.A4);
                PdfWriter.getInstance(document, out);

                document.open();

                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

                document.add(new Paragraph("LAPORAN KEUANGAN", titleFont));
                document.add(new Paragraph(
                                "Periode: " + start + " s/d " + end,
                                normalFont));

                document.add(Chunk.NEWLINE);

                document.add(new Paragraph("PEMASUKAN", headerFont));

                PdfPTable incomeTable = new PdfPTable(3);
                incomeTable.setWidthPercentage(100);

                incomeTable.addCell("Tanggal");
                incomeTable.addCell("Invoice");
                incomeTable.addCell("Total");

                for (SalesInvoice s : incomes) {incomeTable.addCell(s.getInvoiceDate()
                        .toLocalDate()
                        .toString());
                        incomeTable.addCell(s.getInvoiceNumber());
                        incomeTable.addCell("Rp " + s.getTotalSales());
                }

                document.add(incomeTable);

                document.add(new Paragraph( "Total Pemasukan: Rp " + totalIncome,
                                                headerFont));
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("PENGELUARAN", headerFont));

                PdfPTable expenseTable = new PdfPTable(3);
                expenseTable.setWidthPercentage(100);

                expenseTable.addCell("Tanggal");
                expenseTable.addCell("Supplier");
                expenseTable.addCell("Total");

                for (Purchase p : expenses) {expenseTable.addCell(p.getPurchaseDate().toLocalDate().toString());
                        expenseTable.addCell(p.getSupplier().getSupplierName());
                        expenseTable.addCell("Rp " + p.getTotalPurchase());
                }

                document.add(expenseTable);
                document.add(new Paragraph("Total Pengeluaran: Rp " + totalExpense, headerFont));
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("Saldo Bersih: Rp " + totalIncome.subtract(totalExpense),headerFont));
                document.close();
        }
}
