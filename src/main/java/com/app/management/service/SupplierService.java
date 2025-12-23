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

        // Method untuk mengambil seluruh data supplier tanpa filter
        public List<Supplier> getAllSuppliers() {
                return supplierRepository.findAll();
        }
        
        // Method untuk mengambil satu supplier berdasarkan ID dengan validasi
        // keberadaan data
        public Supplier getSupplierById(Long id) {
                return supplierRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Supplier tidak ditemukan"));
        }

        // Method untuk menyimpan data supplier baru dengan validasi bisnis
        public Supplier saveSupplier(Supplier supplier) {

                validateSupplier(supplier);

                return supplierRepository.save(supplier);
        }

        // Method untuk memperbarui data supplier berdasarkan ID dengan validasi
        // server-side
        public Supplier updateSupplier(Long id, Supplier newData) {

                validateSupplier(newData);

                return supplierRepository.findById(id)
                                .map(existing -> {

                                        existing.setSupplierName(
                                                        newData.getSupplierName());
                                        existing.setTelephoneNumber(
                                                        newData.getTelephoneNumber());
                                        existing.setAddress(
                                                        newData.getAddress());

                                        return supplierRepository.save(existing);
                                })
                                .orElseThrow(() -> new IllegalArgumentException("Supplier tidak ditemukan"));
        }

        // Method untuk mencari dan mengurutkan supplier berdasarkan keyword dan
        // parameter sorting
        public List<Supplier> searchAndSort(
                        String keyword,
                        String sort) {

                Sort sortOrder;

                switch (sort) {
                        case "name_asc":
                                sortOrder = Sort.by("supplierName").ascending();
                                break;
                        case "name_desc":
                                sortOrder = Sort.by("supplierName").descending();
                                break;
                        default:
                                sortOrder = Sort.by("id").descending();
                }

                List<Supplier> suppliers = supplierRepository.findAll(sortOrder);

                if (keyword != null && !keyword.isBlank()) {

                        String key = keyword.toLowerCase();

                        suppliers = suppliers.stream()
                                        .filter(s -> s.getSupplierName()
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

        private void validateSupplier(Supplier supplier) {

                if (supplier.getSupplierName() == null
                                || supplier.getSupplierName().isBlank()) {
                        throw new IllegalArgumentException(
                                        "Nama supplier wajib diisi");
                }

                if (supplier.getTelephoneNumber() == null
                                || !supplier.getTelephoneNumber()
                                                .matches("[\\d\\s\\+\\()-]+")) {
                        throw new IllegalArgumentException(
                                        "Format nomor telepon tidak valid");
                }

                if (supplier.getAddress() == null
                                || supplier.getAddress().isBlank()) {
                        throw new IllegalArgumentException(
                                        "Alamat supplier wajib diisi");
                }
        }
}
