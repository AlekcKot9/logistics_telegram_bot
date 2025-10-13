package com.logistics.repositories;

import com.logistics.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByEmail(String email);

    List<Customer> findByFullNameContainingIgnoreCase(String fullName);

    List<Customer> findByPhone(String phone);

    @Query("SELECT MAX(c.customerId) FROM Customer c")
    Integer findMaxCustomerId();

    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "c.phone LIKE CONCAT('%', :searchTerm, '%')")
    List<Customer> searchCustomers(@Param("searchTerm") String searchTerm);

    @Query("SELECT c FROM Customer c WHERE c.email = :email AND c.passwordHash = :password")
    Optional<Customer> findByEmailAndPassword(@Param("email") String email,
                                              @Param("password") String password);

    boolean existsByEmail(String email);
}
