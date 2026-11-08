package com.university.management.config;

import com.university.management.entity.Admin;
import com.university.management.entity.UserAccount;
import com.university.management.enums.Role;
import com.university.management.repository.AdminRepository;
import com.university.management.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserAccountRepository userAccountRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${application.admin.bootstrap.enabled}")
    private boolean bootstrapEnabled;

    @Value("${application.admin.bootstrap.username:}")
    private String bootstrapUsername;

    @Value("${application.admin.bootstrap.email:}")
    private String bootstrapEmail;

    @Value("${application.admin.bootstrap.password:}")
    private String bootstrapPassword;

    @Value("${application.admin.bootstrap.fullName:System Administrator}")
    private String bootstrapFullName;

    @Bean
    public CommandLineRunner initializeData() {
        return args -> {
            if (!bootstrapEnabled) {
                log.debug("Admin bootstrap is disabled. Skipping.");
                return;
            }

            // Validate required bootstrap values
            if (!StringUtils.hasText(bootstrapUsername)
                    || !StringUtils.hasText(bootstrapEmail)
                    || !StringUtils.hasText(bootstrapPassword)) {
                log.warn("Bootstrap enabled but ADMIN_BOOTSTRAP_USERNAME, ADMIN_BOOTSTRAP_EMAIL, "
                        + "or ADMIN_BOOTSTRAP_PASSWORD not set. Skipping bootstrap.");
                return;
            }

            // Only create if no admin exists yet
            long adminCount = userAccountRepository.countByRole(Role.ADMIN);
            if (adminCount > 0) {
                log.info("Admin accounts already exist (count={}). Skipping bootstrap.", adminCount);
                return;
            }

            log.info("No admins found. Creating initial Admin account: {}", bootstrapUsername);

            UserAccount account = UserAccount.builder()
                    .username(bootstrapUsername)
                    .email(bootstrapEmail)
                    .passwordHash(passwordEncoder.encode(bootstrapPassword))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            account = userAccountRepository.save(account);

            Admin admin = Admin.builder()
                    .userAccount(account)
                    .fullName(bootstrapFullName)
                    .build();
            adminRepository.save(admin);

            log.info("Initial Admin account created successfully. "
                    + "DISABLE bootstrap (ADMIN_BOOTSTRAP_ENABLED=false) after first login.");
        };
    }
}
