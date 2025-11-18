package com.logistics.repositories;

import com.logistics.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Integer> {

    @Query("SELECT a FROM Admin a WHERE a.customerId = :adminId AND a.hashPassword = :password")
    Optional<Admin> findByAdminIdAndPassword(@Param("adminId") Integer adminId,
                                             @Param("password") String password);

    // Добавляем метод для проверки существования администратора по ID
    boolean existsByCustomerId(Integer adminId);
}