package com.commercex.shipping.client;

import com.commercex.shipping.client.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/internal/users/{id}")
    UserDTO getUser(@PathVariable Long id);
}
