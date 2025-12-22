package com.app.management.service;

import com.app.management.model.Supplier;
import com.app.management.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier tidak ditemukan"));
    }

    public Supplier saveSupplier(Supplier supplier) {
        validateSupplier(supplier);
        return supplierRepository.save(supplier);
    }

    public Supplier updateSupplier(Long id, Supplier newData) {
        validateSupplier(newData);

        return supplierRepository.findById(id)
                .map(existing -> {
                    existing.setSupplierName(newData.getSupplierName());
                    existing.setTelephoneNumber(newData.getTelephoneNumber());
                    existing.setAddress(newData.getAddress());
                    return supplierRepository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("Supplier tidak ditemukan"));
    }

    // ===============================
    // BUSINESS VALIDATION (CORE RULE)
    // ===============================
    private void validateSupplier(Supplier supplier) {

        if (supplier.getSupplierName() == null || supplier.getSupplierName().isBlank()) {
            throw new IllegalArgumentException("Nama supplier wajib diisi");
        }

        if (supplier.getTelephoneNumber() == null ||
                !supplier.getTelephoneNumber().matches("[\\d\\s\\+\\()-]+")) {
            throw new IllegalArgumentException("Format nomor telepon tidak valid");
        }

        if (supplier.getAddress() == null || supplier.getAddress().isBlank()) {
            throw new IllegalArgumentException("Alamat supplier wajib diisi");
        }
    }

    public List<Supplier> searchAndSort(String keyword, String sort) {

        Sort sortOrder;

        switch (sort) {
            case "name_asc":
                sortOrder = Sort.by("supplierName").ascending();
                break;
            case "name_desc":
                sortOrder = Sort.by("supplierName").descending();
                break;
            default:
                sortOrder = Sort.by("id").descending(); // default lama
        }

        List<Supplier> suppliers = supplierRepository.findAll(sortOrder);

        // 🔍 SEARCH (IN-MEMORY, AMAN)
        if (keyword != null && !keyword.isBlank()) {
            String key = keyword.toLowerCase();
            suppliers = suppliers.stream()
                    .filter(s -> s.getSupplierName().toLowerCase().contains(key) ||
                            s.getTelephoneNumber().toLowerCase().contains(key) ||
                            s.getAddress().toLowerCase().contains(key))
                    .toList();
        }

        return suppliers;
    }

}
