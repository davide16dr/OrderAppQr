package com.orderapp.ordering.controller;

import com.orderapp.ordering.entity.SubscriptionPlan;
import com.orderapp.ordering.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {
    private final SubscriptionPlanService subscriptionPlanService;

    @GetMapping
    public ResponseEntity<List<SubscriptionPlan>> getAllActivePlans() {
        return ResponseEntity.ok(subscriptionPlanService.getAllActivePlans());
    }

    @GetMapping("/{code}")
    public ResponseEntity<SubscriptionPlan> getPlanByCode(@PathVariable String code) {
        return subscriptionPlanService.getPlanByCode(code)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
