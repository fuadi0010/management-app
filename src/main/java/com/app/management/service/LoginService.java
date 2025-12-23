package com.app.management.service;

// Spring DI & Service annotation
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Enum & entity user
import com.app.management.model.user.Role;
import com.app.management.model.user.User;
import com.app.management.model.user.UserStatus;

// Repository khusus login / user lookup
import com.app.management.repository.LoginRepository;

// Menandakan class ini adalah Service (business logic auth)
@Service
public class LoginService {

    // Inject repository untuk akses data user
    @Autowired
    private LoginRepository loginRepository;

    /**
     * Proses login dengan username dan password
     */
    public User loginUser(String name, String password) {

        // =====================
        // 1. VALIDASI INPUT
        // =====================

        // Username wajib diisi
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Username harus diisi");
        }

        // Password wajib diisi
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password harus diisi");
        }

        // =====================
        // 2. CARI USER
        // =====================

        // Ambil user berdasarkan username
        // Jika tidak ditemukan → login gagal
        User user = loginRepository.findByName(name.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException("Username atau password salah"));

        // =====================
        // 3. VALIDASI PASSWORD
        // =====================

        // Bandingkan password dari input dengan password di database
        // (diasumsikan masih plaintext, lihat catatan di bawah)
        if (!user.getPassword().equals(password.trim())) {
            throw new IllegalArgumentException("Username atau password salah");
        }

        // =====================
        // 4. VALIDASI STATUS AKUN
        // =====================

        // Akun masih menunggu approval admin
        if (user.getStatus() == UserStatus.PENDING) {
            throw new IllegalStateException(
                    "Akun Anda belum disetujui oleh admin");
        }

        // Akun ditolak oleh admin
        if (user.getStatus() == UserStatus.REJECTED) {
            throw new IllegalStateException(
                    "Akun Anda ditolak oleh admin");
        }

        // Akun dibanned
        if (user.getStatus() == UserStatus.BANNED) {
            throw new IllegalStateException(
                    "Akun Anda Diban Oleh Admin");
        }

        // Jika lolos semua validasi → login sukses
        return user;
    }

    /**
     * Tentukan redirect URL berdasarkan role user
     */
    public String getRedirectUrlByRole(Role role) {

        // Routing dashboard berdasarkan role
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

        // =====================
        // VALIDASI UNIK
        // =====================

        // Username tidak boleh duplikat
        if (loginRepository.existsByName(user.getName().trim())) {
            throw new IllegalArgumentException("Username sudah terdaftar");
        }

        // Email tidak boleh duplikat
        if (loginRepository.existsByEmail(user.getEmail().trim())) {
            throw new IllegalArgumentException("Email sudah terdaftar");
        }

        // =====================
        // HARD RULE (SERVER SIDE)
        // =====================

        // Default role untuk user baru adalah STAFF
        // (tidak boleh ditentukan dari client)
        user.setRole(Role.STAFF);

        // Status awal PENDING (menunggu approval admin)
        user.setStatus(UserStatus.PENDING);

        // Simpan user ke database
        return loginRepository.save(user);
    }

    /**
     * Cek apakah user sudah login
     */
    public boolean isUserLoggedIn(Object sessionUser) {

        // Session dianggap valid jika object user ada
        return sessionUser != null;
    }

    /**
     * Validasi akses berdasarkan role
     */
    public void validateAccess(User user, Role requiredRole) {

        // User wajib login
        if (user == null) {
            throw new IllegalStateException("User belum login");
        }

        // Akun harus aktif
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Akun belum aktif");
        }

        // Role harus sesuai dengan yang dibutuhkan
        if (user.getRole() != requiredRole) {
            throw new IllegalStateException("Akses ditolak");
        }
    }

}
