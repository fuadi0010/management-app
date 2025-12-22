package com.app.management;

import com.app.management.model.user.Role;
import com.app.management.model.user.User;
import com.app.management.model.user.UserStatus;
import com.app.management.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
public class ManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagementApplication.class, args);
    }

    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository) {
        return args -> {

            boolean adminExists = userRepository.existsByRole(Role.ADMIN);

            if (!adminExists) {
                User admin = new User();
                admin.setName("Administrator");
                admin.setEmail("admin@gmail.com");
                admin.setPassword("admin123"); // plain text (sesuai permintaan)
                admin.setRole(Role.ADMIN);
                admin.setStatus(UserStatus.ACTIVE);
                admin.setCreatedAt(LocalDateTime.now());

                userRepository.save(admin);
            }
        };
    }
}
