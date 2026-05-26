package com.orderapp.ordering.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class DashboardMetricsDto {
    private BigDecimal totalRevenueToday;
    private Integer totalOrdersToday;
    private Integer activeCustomerCount;
    private Double averageOrderTime; // in minutes
    private LocalDateTime lastUpdated;
    private List<OrderByHourDto> ordersByHour;
    private List<WeeklyRevenueDto> weeklyRevenue;
    private List<TopProductDto> topProducts;
    private List<AreaDistributionDto> areaDistribution;

    public DashboardMetricsDto() {}

    public DashboardMetricsDto(BigDecimal totalRevenueToday, Integer totalOrdersToday,
                               Integer activeCustomerCount, Double averageOrderTime) {
        this.totalRevenueToday = totalRevenueToday;
        this.totalOrdersToday = totalOrdersToday;
        this.activeCustomerCount = activeCustomerCount;
        this.averageOrderTime = averageOrderTime;
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public BigDecimal getTotalRevenueToday() {
        return totalRevenueToday;
    }

    public void setTotalRevenueToday(BigDecimal totalRevenueToday) {
        this.totalRevenueToday = totalRevenueToday;
    }

    public Integer getTotalOrdersToday() {
        return totalOrdersToday;
    }

    public void setTotalOrdersToday(Integer totalOrdersToday) {
        this.totalOrdersToday = totalOrdersToday;
    }

    public Integer getActiveCustomerCount() {
        return activeCustomerCount;
    }

    public void setActiveCustomerCount(Integer activeCustomerCount) {
        this.activeCustomerCount = activeCustomerCount;
    }

    public Double getAverageOrderTime() {
        return averageOrderTime;
    }

    public void setAverageOrderTime(Double averageOrderTime) {
        this.averageOrderTime = averageOrderTime;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<OrderByHourDto> getOrdersByHour() {
        return ordersByHour;
    }

    public void setOrdersByHour(List<OrderByHourDto> ordersByHour) {
        this.ordersByHour = ordersByHour;
    }

    public List<WeeklyRevenueDto> getWeeklyRevenue() {
        return weeklyRevenue;
    }

    public void setWeeklyRevenue(List<WeeklyRevenueDto> weeklyRevenue) {
        this.weeklyRevenue = weeklyRevenue;
    }

    public List<TopProductDto> getTopProducts() {
        return topProducts;
    }

    public void setTopProducts(List<TopProductDto> topProducts) {
        this.topProducts = topProducts;
    }

    public List<AreaDistributionDto> getAreaDistribution() {
        return areaDistribution;
    }

    public void setAreaDistribution(List<AreaDistributionDto> areaDistribution) {
        this.areaDistribution = areaDistribution;
    }

    // Inner DTOs
    public static class OrderByHourDto {
        private Integer hour;
        private Integer count;

        public OrderByHourDto() {}

        public OrderByHourDto(Integer hour, Integer count) {
            this.hour = hour;
            this.count = count;
        }

        public Integer getHour() {
            return hour;
        }

        public void setHour(Integer hour) {
            this.hour = hour;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }

    public static class WeeklyRevenueDto {
        private String day;
        private Double revenue;

        public WeeklyRevenueDto() {}

        public WeeklyRevenueDto(String day, Double revenue) {
            this.day = day;
            this.revenue = revenue;
        }

        public String getDay() {
            return day;
        }

        public void setDay(String day) {
            this.day = day;
        }

        public Double getRevenue() {
            return revenue;
        }

        public void setRevenue(Double revenue) {
            this.revenue = revenue;
        }
    }

    public static class TopProductDto {
        private Long id;
        private String name;
        private Integer quantity;

        public TopProductDto() {}

        public TopProductDto(Long id, String name, Integer quantity) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    public static class AreaDistributionDto {
        private Long areaId;
        private String areaName;
        private Integer orderCount;

        public AreaDistributionDto() {}

        public AreaDistributionDto(Long areaId, String areaName, Integer orderCount) {
            this.areaId = areaId;
            this.areaName = areaName;
            this.orderCount = orderCount;
        }

        public Long getAreaId() {
            return areaId;
        }

        public void setAreaId(Long areaId) {
            this.areaId = areaId;
        }

        public String getAreaName() {
            return areaName;
        }

        public void setAreaName(String areaName) {
            this.areaName = areaName;
        }

        public Integer getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(Integer orderCount) {
            this.orderCount = orderCount;
        }
    }
}
