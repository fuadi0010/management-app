package com.app.management.service;

import com.app.management.model.user.Role;
import com.app.management.model.user.User;
import com.app.management.model.user.UserStatus;
import com.app.management.repository.UserRepository;

import jakarta.transaction.Transactional;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Method untuk mengambil daftar user yang masih menunggu persetujuan admin
    public List<User> getPendingUsers() {
        return userRepository.findByStatus(UserStatus.PENDING);
    }

    // Method untuk menghitung total user selain admin
    public long getTotalUserCount() {
        return userRepository.countAllUsersExcludingAdmin();
    }

    // Method untuk menghitung jumlah user dengan status pending
    public long getPendingUserCount() {
        return userRepository.countByStatus(UserStatus.PENDING);
    }

    // Method untuk menghitung jumlah user dengan status aktif
    public long getActiveUserCount() {
        return userRepository.countByStatus(UserStatus.ACTIVE);
    }

    // Method untuk mengambil daftar staff berdasarkan satu status tertentu
    public List<User> getStaffByStatus(UserStatus status) {
        return userRepository.findByRoleAndStatus(
                Role.STAFF,
                status);
    }

    // Method untuk mengambil daftar staff berdasarkan beberapa status sekaligus
    public List<User> getStaffByStatuses(UserStatus... statuses) {
        return userRepository.findByRoleAndStatusIn(
                Role.STAFF,
                List.of(statuses));
    }

    // Method untuk menyetujui user dan mengubah statusnya menjadi aktif
    @Transactional
    public void approveUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalStateException("User tidak ditemukan"));

        user.setStatus(UserStatus.ACTIVE);
    }

    // Method untuk menolak pendaftaran user
    @Transactional
    public void rejectUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalStateException("User tidak ditemukan"));

        user.setStatus(UserStatus.REJECTED);
    }

    // Method untuk memban staff oleh admin
    @Transactional
    public void banStaff(Long targetUserId, User admin) {

        validateAdmin(admin);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() ->
                        new IllegalStateException("User tidak ditemukan"));

        if (target.getRole() != Role.STAFF) {
            throw new IllegalStateException(
                    "Hanya staff yang bisa diban");
        }

        target.setStatus(UserStatus.BANNED);
    }

    // Method untuk menghapus staff secara permanen setelah diban
    @Transactional
    public void deleteBannedStaff(Long targetUserId, User admin) {

        validateAdmin(admin);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() ->
                        new IllegalStateException("User tidak ditemukan"));

        if (target.getStatus() != UserStatus.BANNED) {
            throw new IllegalStateException(
                    "User harus diban terlebih dahulu");
        }

        userRepository.delete(target);
    }

    private void validateAdmin(User admin) {

        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Akses ditolak");
        }
    }
}
