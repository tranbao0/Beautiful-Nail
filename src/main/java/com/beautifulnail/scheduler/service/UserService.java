package com.beautifulnail.scheduler.service;

import com.beautifulnail.scheduler.model.User;
import com.beautifulnail.scheduler.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * Verify credentials and return the matching user, or empty if invalid.
     */
    public Optional<User> authenticate(String email, String password) {
        return userRepo.findByEmail(email.trim().toLowerCase())
                .filter(u -> u.getPasswordHash().equals(hashPassword(password)));
    }

    /**
     * Create a new customer account.  Throws if the email is already taken.
     */
    public User register(String firstName, String mInit, String lastName,
                         String email, String phone, String password) {
        User user = new User();
        user.setFirstName(firstName.trim());
        user.setMInit((mInit != null && !mInit.isBlank()) ? mInit.trim() : null);
        user.setLastName(lastName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPhone((phone != null && !phone.isBlank()) ? phone.trim() : null);
        user.setRole("customer");
        user.setPasswordHash(hashPassword(password));
        userRepo.save(user);
        return userRepo.findByEmail(user.getEmail()).orElseThrow();
    }

    public boolean emailExists(String email) {
        return userRepo.findByEmail(email.trim().toLowerCase()).isPresent();
    }

    /**
     * SHA-256 hex digest of the password.
     * Seed data uses SHA-256("password") =
     * 5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8
     */
    static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
}
