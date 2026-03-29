package com.commercex.shipping.client;

import com.commercex.shipping.client.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/internal/orders/{id}")
    OrderDTO getOrder(@PathVariable Long id);
}
