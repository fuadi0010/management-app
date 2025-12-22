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

    // ======================
    // DASHBOARD DATA
    // ======================

    public List<User> getPendingUsers() {
        return userRepository.findByStatus(UserStatus.PENDING);
    }

    public long getTotalUserCount() {
        return userRepository.countAllUsersExcludingAdmin();
    }

    public long getPendingUserCount() {
        return userRepository.countByStatus(UserStatus.PENDING);
    }

    public long getActiveUserCount() {
        return userRepository.countByStatus(UserStatus.ACTIVE);
    }

    // ======================
    // STAFF MANAGEMENT
    // ======================

    public List<User> getStaffByStatus(UserStatus status) {
        return userRepository.findByRoleAndStatus(Role.STAFF, status);
    }

    public List<User> getStaffByStatuses(UserStatus... statuses) {
        return userRepository.findByRoleAndStatusIn(
                Role.STAFF,
                List.of(statuses));
    }

    // ======================
    // APPROVAL FLOW
    // ======================

    @Transactional
    public void approveUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("User tidak ditemukan"));
        user.setStatus(UserStatus.ACTIVE);
    }

    @Transactional
    public void rejectUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("User tidak ditemukan"));
        user.setStatus(UserStatus.REJECTED);
    }

    @Transactional
    public void approveAllPending() {
        List<User> pendingUsers = getPendingUsers();
        pendingUsers.forEach(u -> u.setStatus(UserStatus.ACTIVE));
        userRepository.saveAll(pendingUsers);
    }

    // ======================
    // BAN STAFF
    // ======================

    @Transactional
    public void banStaff(Long targetUserId, User admin) {

        validateAdmin(admin);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalStateException("User tidak ditemukan"));

        if (target.getRole() != Role.STAFF) {
            throw new IllegalStateException("Hanya staff yang bisa diban");
        }

        target.setStatus(UserStatus.BANNED);
    }

    // ======================
    // HARD DELETE (ONLY BANNED)
    // ======================

    @Transactional
    public void deleteBannedStaff(Long targetUserId, User admin) {

        validateAdmin(admin);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalStateException("User tidak ditemukan"));

        if (target.getStatus() != UserStatus.BANNED) {
            throw new IllegalStateException("User harus diban terlebih dahulu");
        }

        userRepository.delete(target);
    }

    // ======================
    // COMMON VALIDATION
    // ======================

    private void validateAdmin(User admin) {
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Akses ditolak");
        }
    }
}
