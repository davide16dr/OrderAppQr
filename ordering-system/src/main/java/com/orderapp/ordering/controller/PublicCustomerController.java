package com.orderapp.ordering.controller;

import org.springframework.web.bind.annotation.GetMapping;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.orderapp.ordering.dto.CreatePublicOrderRequest;
import com.orderapp.ordering.dto.CreatePublicOrderResponse;
import com.orderapp.ordering.dto.CustomerMenuViewModelDTO;
import com.orderapp.ordering.service.PublicCustomerMenuService;
import com.orderapp.ordering.service.PublicOrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/customer")
public class PublicCustomerController {
	private final PublicCustomerMenuService publicCustomerMenuService;
	private final PublicOrderService publicOrderService;

	@GetMapping("/menu")
	public CustomerMenuViewModelDTO getMenu(
		@RequestParam(name = "token", required = false) String token,
		@RequestParam(name = "tenant", required = false) String tenant,
		@RequestParam(name = "location", required = false) String location
	) {
		return publicCustomerMenuService.getMenu(token, tenant, location);
	}

	@PostMapping("/orders")
	public CreatePublicOrderResponse createOrder(@RequestBody CreatePublicOrderRequest request) {
		return publicOrderService.createPublicOrder(request);
	}

	@GetMapping("/orders/{orderId}")
	public Map<String, String> getOrderStatus(@PathVariable long orderId) {
		return publicOrderService.getPublicOrderStatus(orderId);
	}
}
