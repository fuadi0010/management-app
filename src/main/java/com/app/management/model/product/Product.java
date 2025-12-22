package com.app.management.model.product;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Id;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Kode Product harus diisi")
    @Column(nullable = false, unique = true)
    private String productCode;

    @NotBlank(message = "Nama Product harus diisi")
    private String productName;

    @NotNull(message = "Stok harus diisi")
    @Min(value = 0, message = "Stok tidak boleh negatif")
    @Column(nullable = false)
    private Integer currentStock = 0;

    @NotNull(message = "Harga Jual harus diisi")
    @DecimalMin(value = "0.0", inclusive = false, message = "Harga jual harus > 0")
    @Column(precision = 19, scale = 2)
    private BigDecimal standardSellingPrice;

    @NotNull(message = "Harga Beli harus diisi")
    @DecimalMin(value = "0.0", inclusive = false, message = "Harga beli harus > 0")
    @Column(precision = 19, scale = 2)
    private BigDecimal lastPurchasePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

}