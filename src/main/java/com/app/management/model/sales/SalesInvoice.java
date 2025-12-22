package com.app.management.model.sales;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "sales_invoice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalesInvoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @CreationTimestamp
    private LocalDateTime invoiceDate;

    @Column(nullable = false)
    private String customerName;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal vatPercentage = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'CREATED'")
    private SalesStatus salesStatus = SalesStatus.CREATED;

    @OneToMany(mappedBy = "salesInvoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceDetails> invoiceDetails = new ArrayList<>();

    public void addInvoiceDetail(InvoiceDetails detail) {
        invoiceDetails.add(detail);
        detail.setSalesInvoice(this);
    }
}