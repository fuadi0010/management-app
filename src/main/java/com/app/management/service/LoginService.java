package com.app.management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.management.model.user.Role;
import com.app.management.model.user.User;
import com.app.management.model.user.UserStatus;
import com.app.management.repository.LoginRepository;

@Service
public class LoginService {

    @Autowired
    private LoginRepository loginRepository;

    // Method untuk memproses autentikasi user berdasarkan username dan password
    public User loginUser(String name, String password) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Username harus diisi");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password harus diisi");
        }

        User user = loginRepository.findByName(name.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException("Username atau password salah"));

        if (!user.getPassword().equals(password.trim())) {
            throw new IllegalArgumentException("Username atau password salah");
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new IllegalStateException(
                    "Akun Anda belum disetujui oleh admin");
        }

        if (user.getStatus() == UserStatus.REJECTED) {
            throw new IllegalStateException(
                    "Akun Anda ditolak oleh admin");
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new IllegalStateException(
                    "Akun Anda Diban Oleh Admin");
        }

        return user;
    }

    // Method untuk menentukan URL redirect dashboard berdasarkan role user
    public String getRedirectUrlByRole(Role role) {

        switch (role) {
            case ADMIN:
                return "/admin/dashboard";
            case STAFF:
                return "/access/staff/dashboard";
            default:
                return "/access/user/dashboard";
        }
    }

    // Method untuk melakukan registrasi user baru dengan rule default role dan status
    public User registerUser(User user) {

        if (loginRepository.existsByName(user.getName().trim())) {
            throw new IllegalArgumentException("Username sudah terdaftar");
        }

        if (loginRepository.existsByEmail(user.getEmail().trim())) {
            throw new IllegalArgumentException("Email sudah terdaftar");
        }

        user.setRole(Role.STAFF);
        user.setStatus(UserStatus.PENDING);

        return loginRepository.save(user);
    }

    // Method untuk mengecek apakah session user masih valid (sudah login)
    public boolean isUserLoggedIn(Object sessionUser) {
        return sessionUser != null;
    }

    // Method untuk memvalidasi hak akses user berdasarkan role yang dibutuhkan
    public void validateAccess(User user, Role requiredRole) {

        if (user == null) {
            throw new IllegalStateException("User belum login");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Akun belum aktif");
        }

        if (user.getRole() != requiredRole) {
            throw new IllegalStateException("Akses ditolak");
        }
    }
}
