package com.commercex.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", path = "/internal/orders")
public interface OrderServiceClient {

    @GetMapping("/{id}")
    OrderDTO getOrder(@PathVariable("id") Long id);
}
