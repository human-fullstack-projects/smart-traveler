package com.smarttraveler.payment.controller;

import com.smarttraveler.payment.service.TossDemoService;
import com.smarttraveler.payment.service.TossDemoService.DemoOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/demo")
@RequiredArgsConstructor
public class TossDemoController {
    private final TossDemoService service;

    public record CreateRequest(String orderName, int amount) {}
    public record ConfirmRequest(String orderId, String paymentKey, int amount) {}

    @PostMapping("/orders")
    public DemoOrder create(@RequestBody CreateRequest request) {
        return service.create(request.orderName(), request.amount());
    }

    @PostMapping("/confirm")
    public DemoOrder confirm(@RequestBody ConfirmRequest request) {
        return service.confirm(request.orderId(), request.paymentKey(), request.amount());
    }

    @GetMapping("/orders/{orderId}")
    public DemoOrder get(@PathVariable String orderId) {
        return service.get(orderId);
    }
}
