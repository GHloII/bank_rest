package com.example.bankcards.controller;

import com.example.bankcards.dto.PageResponseDTO;
import com.example.bankcards.dto.CreateUserDTO;
import com.example.bankcards.dto.UpdateUserDTO;
import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @PostMapping("/admin")
    @Operation(summary = "Admin: create user")
    public ResponseEntity<UserDTO> create(@Valid @ParameterObject @ModelAttribute CreateUserDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(dto));
    }

    @GetMapping("/admin")
    @Operation(summary = "Admin: list all users")
    public ResponseEntity<PageResponseDTO<UserDTO>> list(@RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size) {
        Pageable pageable = PageRequest.of(page != null ? Math.max(0, page) : 0,
                size != null ? Math.min(Math.max(1, size), 100) : 10,
                Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/admin/search")
    @Operation(summary = "Admin: search users by username/email")
    public ResponseEntity<PageResponseDTO<UserDTO>> search(@RequestParam(required = false) String q,
                                                           @RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size) {
        Pageable pageable = PageRequest.of(page != null ? Math.max(0, page) : 0,
                size != null ? Math.min(Math.max(1, size), 100) : 10,
                Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(userService.searchUsers(q, pageable));
    }

    @GetMapping("/admin/role")
    @Operation(summary = "Admin: list users by role name")
    public ResponseEntity<PageResponseDTO<UserDTO>> byRole(@RequestParam String roleName,
                                                           @RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size) {
        Pageable pageable = PageRequest.of(page != null ? Math.max(0, page) : 0,
                size != null ? Math.min(Math.max(1, size), 100) : 10,
                Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(userService.getUsersByRole(roleName, pageable));
    }

    @GetMapping("/admin/{id}")
    @Operation(summary = "Admin: get user by id")
    public ResponseEntity<UserDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/admin/{id}")
    @Operation(summary = "Admin: update user (email/fullName/enabled)")
    public ResponseEntity<UserDTO> update(@PathVariable Long id, @Valid @ParameterObject @ModelAttribute UpdateUserDTO dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @DeleteMapping("/admin/{id}")
    @Operation(summary = "Admin: delete user")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "User: get my profile")
    public ResponseEntity<UserDTO> me() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }
}
