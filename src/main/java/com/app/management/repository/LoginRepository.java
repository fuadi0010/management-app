package com.app.management.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.management.model.user.User;

@Repository
public interface LoginRepository extends JpaRepository<User, Long> {
    
    // Cari user berdasarkan email
    Optional<User> findByEmail(String email);
    
    // Cari user berdasarkan name (username) dan password - PERBAIKAN
    Optional<User> findByName(String name);

    
    // Cek apakah email sudah terdaftar
    boolean existsByEmail(String email);
    
    // Cari user berdasarkan role
    Optional<User> findByEmailAndRole(String email, com.app.management.model.user.Role role);
    
    // Tambah method untuk cek name sudah ada - BARU
    boolean existsByName(String name);
}