package com.vestisen.repository;

import com.vestisen.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    @Query("SELECT u FROM User u WHERE u.email = :value OR u.phone = :value")
    Optional<User> findByEmailOrPhone(@Param("value") String value);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByCode(String code);
    
    @Query("SELECT u FROM User u WHERE u.verificationToken = :token")
    Optional<User> findByVerificationToken(@Param("token") String token);
    
    @Query("SELECT u FROM User u WHERE u.resetPasswordToken = :token")
    Optional<User> findByResetPasswordToken(@Param("token") String token);
}
