package com.logistics.service;

import com.logistics.model.Customer;
import com.logistics.repositories.CustomerRepository;
import com.logistics.session.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SessionService sessionService;

    public boolean authenticate(Long chatId, String email, String password) {
        Optional<Customer> customerOpt = customerRepository.findByEmail(email);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            // В реальном проекте используйте хеширование паролей!
            if (customer.getPasswordHash().equals(password)) {
                // Обновляем сессию
                UserSession session = sessionService.getSession(chatId);
                if (session != null) {
                    session.setAttribute("authenticated", true);
                    session.setAttribute("customer", customer);
                    session.setAttribute("customerId", customer.getCustomerId());
                    session.setCurrentState("AUTHENTICATED");
                }
                return true;
            }
        }
        return false;
    }

    public boolean isAuthenticated(Long chatId) {
        UserSession session = sessionService.getSession(chatId);
        return session != null &&
                session.getAttribute("authenticated") != null &&
                (Boolean) session.getAttribute("authenticated");
    }

    public Customer getAuthenticatedCustomer(Long chatId) {
        UserSession session = sessionService.getSession(chatId);
        if (session != null && isAuthenticated(chatId)) {
            return (Customer) session.getAttribute("customer");
        }
        return null;
    }

    public void logout(Long chatId) {
        UserSession session = sessionService.getSession(chatId);
        if (session != null) {
            session.setAttribute("authenticated", false);
            session.removeAttribute("customer");
            session.removeAttribute("customerId");
            session.setCurrentState("UNAUTHENTICATED");
        }
    }

    public boolean customerExists(String email) {
        return customerRepository.findByEmail(email).isPresent();
    }
}