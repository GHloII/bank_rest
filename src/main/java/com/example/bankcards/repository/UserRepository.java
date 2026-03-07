package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    // Load user with roles 
    @Query("select u from User u left join fetch u.roles where u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    Page<User> findAll(Pageable pageable);

    @Query("select u from User u " +
           "where (:q is null or " +
           "lower(u.username) like lower(concat('%', :q, '%')) or " +
           "lower(u.email) like lower(concat('%', :q, '%'))) ")
    Page<User> searchUsers(@Param("q") String q, Pageable pageable);

    @Query("select distinct u from User u join u.roles r where r.name = :roleName")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);
}
