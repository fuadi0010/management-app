package com.app.management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.management.model.product.Product;
import com.app.management.model.product.ProductStatus;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findTopByOrderByIdDesc();

    boolean existsByProductCodeIgnoreCase(String productCode);

    List<Product> findByProductNameContainingIgnoreCase(String keyword);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByProductNameContainingIgnoreCaseAndStatus(
            String keyword,
            ProductStatus status);

}
