package com.example.bankcards.service;

import com.example.bankcards.dto.CreateUserDTO;
import com.example.bankcards.dto.PageResponseDTO;
import com.example.bankcards.dto.UpdateUserDTO;
import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO createUser(CreateUserDTO dto) {
        log.debug("Admin creating user with username: {}", dto != null ? dto.getUsername() : null);

        if (dto == null) {
            throw new BadRequestException("Create payload is required");
        }
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new BadRequestException("Username is required");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BadRequestException("Password is required");
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new NotFoundException("Role not found: ROLE_USER"));

        User user = User.builder()
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .enabled(true)
                .build();
        user.getRoles().add(userRole);

        User saved = userRepository.save(user);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponseDTO<UserDTO> getAllUsers(Pageable pageable) {
        log.debug("Admin requested all users");
        Page<User> page = userRepository.findAll(pageable);
        return toPageDto(page);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponseDTO<UserDTO> searchUsers(String query, Pageable pageable) {
        log.debug("Admin searching users with query: {}", query);
        Page<User> page = userRepository.searchUsers(query, pageable);
        return toPageDto(page);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponseDTO<UserDTO> getUsersByRole(String roleName, Pageable pageable) {
        log.debug("Admin requesting users by role: {}", roleName);
        Page<User> page = userRepository.findByRoleName(roleName, pageable);
        return toPageDto(page);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public UserDTO getCurrentUser() {
        String username = getCurrentUsername();
        log.debug("User {} requested their own profile", username);
        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public UserDTO getUserById(Long id) {
        log.debug("Request to get user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        return toDto(user);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO updateUser(Long id, UpdateUserDTO updatedUser) {
        log.debug("Admin updating user with id: {}", id);
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        if (updatedUser == null) {
            throw new BadRequestException("Update payload is required");
        }

        if (updatedUser.getEmail() != null) {
            existing.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getFullName() != null) {
            existing.setFullName(updatedUser.getFullName());
        }
        if (updatedUser.getEnabled() != null) {
            existing.setEnabled(updatedUser.getEnabled());
        }

        User saved = userRepository.save(existing);
        return toDto(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        log.debug("Admin deleting user with id: {}", id);
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private PageResponseDTO<UserDTO> toPageDto(Page<User> page) {
        return PageResponseDTO.of(
                page.getContent().stream().map(this::toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    private UserDTO toDto(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .enabled(user.getEnabled())
                .roles(user.getRoles() == null ? java.util.Set.of() : user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toSet()))
                .build();
    }

    private String getCurrentUsername() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUsername();
    }
}
