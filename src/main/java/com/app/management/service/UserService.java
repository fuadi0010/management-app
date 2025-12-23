package com.app.management.service;

// Enum & entity user
import com.app.management.model.user.Role;
import com.app.management.model.user.User;
import com.app.management.model.user.UserStatus;

// Repository layer user
import com.app.management.repository.UserRepository;

// Transaction management
import jakarta.transaction.Transactional;

// Collection
import java.util.List;

// Spring DI & Service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Menandakan class ini adalah Service (business logic user management)
@Service
public class UserService {

    // Inject UserRepository
    @Autowired
    private UserRepository userRepository;

    // ======================
    // DASHBOARD DATA
    // ======================

    // Ambil seluruh user dengan status PENDING
    // Digunakan di admin dashboard (approval list)
    public List<User> getPendingUsers() {
        return userRepository.findByStatus(UserStatus.PENDING);
    }

    // Hitung total user (biasanya exclude ADMIN)
    public long getTotalUserCount() {
        return userRepository.countAllUsersExcludingAdmin();
    }

    // Hitung jumlah user pending
    public long getPendingUserCount() {
        return userRepository.countByStatus(UserStatus.PENDING);
    }

    // Hitung jumlah user aktif
    public long getActiveUserCount() {
        return userRepository.countByStatus(UserStatus.ACTIVE);
    }

    // ======================
    // STAFF MANAGEMENT
    // ======================

    // Ambil staff berdasarkan satu status tertentu
    public List<User> getStaffByStatus(UserStatus status) {
        return userRepository.findByRoleAndStatus(
                Role.STAFF,
                status);
    }

    // Ambil staff berdasarkan banyak status (IN clause)
    public List<User> getStaffByStatuses(UserStatus... statuses) {
        return userRepository.findByRoleAndStatusIn(
                Role.STAFF,
                List.of(statuses));
    }

    // ======================
    // APPROVAL FLOW
    // ======================

    // Set user menjadi ACTIVE (approve)
    @Transactional
    public void approveUser(Long id) {

        // Ambil user berdasarkan ID
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalStateException("User tidak ditemukan"));

        // Update status menjadi ACTIVE
        // Perubahan otomatis dipersist oleh JPA (dirty checking)
        user.setStatus(UserStatus.ACTIVE);
    }

    // Set user menjadi REJECTED
    @Transactional
    public void rejectUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalStateException("User tidak ditemukan"));

        user.setStatus(UserStatus.REJECTED);
    }

    // Approve seluruh user yang masih PENDING
    @Transactional
    public void approveAllPending() {

        // Ambil semua user pending
        List<User> pendingUsers = getPendingUsers();

        // Set status menjadi ACTIVE
        pendingUsers.forEach(
                u -> u.setStatus(UserStatus.ACTIVE));

        // Save batch (opsional, tapi eksplisit)
        userRepository.saveAll(pendingUsers);
    }

    // ======================
    // BAN STAFF
    // ======================

    // Memban staff tertentu
    @Transactional
    public void banStaff(Long targetUserId, User admin) {

        // Validasi admin (server authority)
        validateAdmin(admin);

        // Ambil user target
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() ->
                        new IllegalStateException("User tidak ditemukan"));

        // Hanya staff yang boleh diban
        if (target.getRole() != Role.STAFF) {
            throw new IllegalStateException(
                    "Hanya staff yang bisa diban");
        }

        // Set status menjadi BANNED
        target.setStatus(UserStatus.BANNED);
    }

    // ======================
    // HARD DELETE (ONLY BANNED)
    // ======================

    // Hapus staff secara permanen (hard delete)
    @Transactional
    public void deleteBannedStaff(Long targetUserId, User admin) {

        // Validasi admin
        validateAdmin(admin);

        // Ambil user target
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() ->
                        new IllegalStateException("User tidak ditemukan"));

        // Hard rule:
        // user harus BANNED sebelum boleh dihapus
        if (target.getStatus() != UserStatus.BANNED) {
            throw new IllegalStateException(
                    "User harus diban terlebih dahulu");
        }

        // Hard delete dari database
        userRepository.delete(target);
    }

    // ======================
    // COMMON VALIDATION
    // ======================

    // Validasi hak akses admin
    private void validateAdmin(User admin) {

        // Admin wajib login & memiliki role ADMIN
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Akses ditolak");
        }
    }
}
