package com.example.bankcards.service;

import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<User> getAllUsers(Pageable pageable) {
        log.debug("Admin requested all users");
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<User> searchUsers(String query, Pageable pageable) {
        log.debug("Admin searching users with query: {}", query);
        return userRepository.searchUsers(query, pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<User> getUsersByRole(String roleName, Pageable pageable) {
        log.debug("Admin requesting users by role: {}", roleName);
        return userRepository.findByRoleName(roleName, pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public User getCurrentUser() {
        String username = getCurrentUsername();
        log.debug("User {} requested their own profile", username);
        return userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public User getUserById(Long id) {
        log.debug("Request to get user with id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User updateUser(Long id, User updatedUser) {
        log.debug("Admin updating user with id: {}", id);
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        existing.setEmail(updatedUser.getEmail());
        existing.setFullName(updatedUser.getFullName());
        existing.setEnabled(updatedUser.getEnabled());

        return userRepository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        log.debug("Admin deleting user with id: {}", id);
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private String getCurrentUsername() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUsername();
    }
}
