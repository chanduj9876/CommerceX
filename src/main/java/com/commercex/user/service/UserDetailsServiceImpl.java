package com.commercex.user.service;

import com.commercex.user.entity.User;
import com.commercex.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads user details from the database for Spring Security.
 *
 * Spring Security calls loadUserByUsername() during authentication.
 * We look up by email (our "username") and convert to Spring's UserDetails.
 *
 * Why "ROLE_" prefix? Spring Security expects authorities with "ROLE_" prefix
 * for hasRole() checks to work. hasRole("ADMIN") internally checks for "ROLE_ADMIN".
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
