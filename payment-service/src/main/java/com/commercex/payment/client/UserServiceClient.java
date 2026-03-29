package com.commercex.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/internal/users")
public interface UserServiceClient {

    @GetMapping("/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);

    @GetMapping("/by-email/{email}")
    UserDTO getUserByEmail(@PathVariable("email") String email);
}
