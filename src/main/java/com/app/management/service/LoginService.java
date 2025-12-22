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

    /**
     * Proses login dengan username dan password
     * 
     * @return
     */
    public User loginUser(String name, String password) {
        // 1. Validasi input
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Username harus diisi");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password harus diisi");
        }

        // 2. Cari user berdasarkan name (username) dan password
        User user = loginRepository.findByNameAndPassword(name.trim(), password.trim())
                .orElse(null);

        // 3. Jika user tidak ditemukan
        if (user == null) {
            throw new IllegalArgumentException("Username atau password salah");
        }

        // 4. Cek status akun (role PENDING)
        if (user.getStatus() == UserStatus.PENDING) {
            throw new IllegalStateException("Akun Anda belum disetujui oleh admin");
        }

        if (user.getStatus() == UserStatus.REJECTED) {
            throw new IllegalStateException("Akun Anda ditolak oleh admin");
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new IllegalStateException("Akun Anda Diban Oleh Admin");
        }

        return user;
    }

    /**
     * Tentukan redirect URL berdasarkan role user
     */
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

    /**
     * Proses registrasi user baru
     * 
     * @return User yang berhasil diregistrasi
     */
    public User registerUser(User user) {

        // VALIDASI
        if (loginRepository.existsByName(user.getName().trim())) {
            throw new IllegalArgumentException("Username sudah terdaftar");
        }

        if (loginRepository.existsByEmail(user.getEmail().trim())) {
            throw new IllegalArgumentException("Email sudah terdaftar");
        }

        // HARD RULE (SERVER SIDE)
        user.setRole(Role.STAFF); // default role
        user.setStatus(UserStatus.PENDING); // workflow status

        return loginRepository.save(user);
    }

    /**
     * Cek apakah user sudah login
     */
    public boolean isUserLoggedIn(Object sessionUser) {
        return sessionUser != null;
    }

    /**
     * Validasi akses berdasarkan role
     */
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