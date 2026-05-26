package com.orderapp.ordering.service;

import com.orderapp.ordering.entity.SubscriptionPlan;
import com.orderapp.ordering.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public List<SubscriptionPlan> getAllActivePlans() {
        return subscriptionPlanRepository.findByIsActiveTrue();
    }

    public Optional<SubscriptionPlan> getPlanByCode(String code) {
        return subscriptionPlanRepository.findById(code);
    }

    public List<SubscriptionPlan> getAllPlans() {
        return subscriptionPlanRepository.findAll();
    }

    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        return subscriptionPlanRepository.save(plan);
    }

    public SubscriptionPlan updatePlan(String code, SubscriptionPlan plan) {
        SubscriptionPlan existingPlan = subscriptionPlanRepository.findById(code)
            .orElseThrow(() -> new RuntimeException("Plan not found: " + code));
        
        existingPlan.setName(plan.getName());
        existingPlan.setDescription(plan.getDescription());
        existingPlan.setPriceMonthly(plan.getPriceMonthly());
        existingPlan.setPriceYearly(plan.getPriceYearly());
        existingPlan.setMaxLocations(plan.getMaxLocations());
        existingPlan.setMaxStaffUsers(plan.getMaxStaffUsers());
        existingPlan.setMaxProducts(plan.getMaxProducts());
        existingPlan.setQrBatchEnabled(plan.getQrBatchEnabled());
        existingPlan.setRealtimeDashboard(plan.getRealtimeDashboard());
        existingPlan.setGlobalCatalogEnabled(plan.getGlobalCatalogEnabled());
        existingPlan.setIsActive(plan.getIsActive());
        
        return subscriptionPlanRepository.save(existingPlan);
    }

    public void deletePlan(String code) {
        subscriptionPlanRepository.deleteById(code);
    }
}
