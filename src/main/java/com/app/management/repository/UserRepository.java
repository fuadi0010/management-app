package com.app.management.repository;

import com.app.management.model.user.Role;
import com.app.management.model.user.User;
import com.app.management.model.user.UserStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByRole(Role role);
    List<User> findByRoleAndStatus(Role role, UserStatus status);
    List<User> findByStatus(UserStatus status);
    List<User> findByRoleAndStatusIn(Role role, List<UserStatus> statuses);
    long countByStatus(UserStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role != 'ADMIN'")
    long countAllUsersExcludingAdmin();

}