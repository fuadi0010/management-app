package com.app.management.service;

// Entity Supplier
import com.app.management.model.Supplier;

// Repository layer untuk Supplier
import com.app.management.repository.SupplierRepository;

// Spring DI & Service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Sorting JPA
import org.springframework.data.domain.Sort;

// Collection
import java.util.List;

// Menandakan class ini adalah Service (business logic supplier)
@Service
public class SupplierService {

    // Inject SupplierRepository
    @Autowired
    private SupplierRepository supplierRepository;

    // Ambil semua supplier (tanpa filter)
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    // Ambil supplier berdasarkan ID
    // Jika tidak ditemukan → exception
    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Supplier tidak ditemukan"));
    }

    // ===============================
    // CREATE SUPPLIER
    // ===============================

    // Simpan supplier baru
    public Supplier saveSupplier(Supplier supplier) {

        // Validasi business rule sebelum save
        validateSupplier(supplier);

        // Persist ke database
        return supplierRepository.save(supplier);
    }

    // ===============================
    // UPDATE SUPPLIER
    // ===============================

    // Update data supplier berdasarkan ID
    public Supplier updateSupplier(Long id, Supplier newData) {

        // Validasi data baru (server-side rule)
        validateSupplier(newData);

        return supplierRepository.findById(id)
                .map(existing -> {

                    // Update field yang diizinkan
                    existing.setSupplierName(
                            newData.getSupplierName());
                    existing.setTelephoneNumber(
                            newData.getTelephoneNumber());
                    existing.setAddress(
                            newData.getAddress());

                    // Simpan perubahan
                    return supplierRepository.save(existing);
                })
                .orElseThrow(() ->
                        new IllegalArgumentException("Supplier tidak ditemukan"));
    }

    // ===============================
    // BUSINESS VALIDATION (CORE RULE)
    // ===============================

    // Validasi inti supplier
    // Dipakai untuk CREATE & UPDATE
    private void validateSupplier(Supplier supplier) {

        // Nama supplier wajib diisi
        if (supplier.getSupplierName() == null
                || supplier.getSupplierName().isBlank()) {
            throw new IllegalArgumentException(
                    "Nama supplier wajib diisi");
        }

        // Nomor telepon wajib valid
        // Mengizinkan angka, spasi, +, (, )
        if (supplier.getTelephoneNumber() == null
                || !supplier.getTelephoneNumber()
                        .matches("[\\d\\s\\+\\()-]+")) {
            throw new IllegalArgumentException(
                    "Format nomor telepon tidak valid");
        }

        // Alamat supplier wajib diisi
        if (supplier.getAddress() == null
                || supplier.getAddress().isBlank()) {
            throw new IllegalArgumentException(
                    "Alamat supplier wajib diisi");
        }
    }

    // ===============================
    // SEARCH & SORT
    // ===============================

    // Digunakan di halaman list supplier
    public List<Supplier> searchAndSort(
            String keyword,
            String sort) {

        Sort sortOrder;

        // Tentukan sorting
        switch (sort) {
            case "name_asc":
                sortOrder =
                        Sort.by("supplierName").ascending();
                break;
            case "name_desc":
                sortOrder =
                        Sort.by("supplierName").descending();
                break;
            default:
                // Default lama: ID desc
                sortOrder =
                        Sort.by("id").descending();
        }

        // Ambil semua supplier sesuai sorting
        List<Supplier> suppliers =
                supplierRepository.findAll(sortOrder);

        // 🔍 SEARCH (IN-MEMORY, AMAN)
        if (keyword != null && !keyword.isBlank()) {

            String key = keyword.toLowerCase();

            suppliers = suppliers.stream()
                    .filter(s ->
                            s.getSupplierName()
                                    .toLowerCase()
                                    .contains(key)
                                    ||
                                    s.getTelephoneNumber()
                                            .toLowerCase()
                                            .contains(key)
                                    ||
                                    s.getAddress()
                                            .toLowerCase()
                                            .contains(key))
                    .toList();
        }

        return suppliers;
    }

}
