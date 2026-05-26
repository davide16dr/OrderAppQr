package com.orderapp.ordering.dto;

import java.math.BigDecimal;
import java.util.List;

public class CreateTenantProductRequestDto {
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Boolean availableForOrder;
    private String imageDataUrl;
    private List<VariantEntry> variants;
    private List<VariantEntry> extras;

    public static class VariantEntry {
        private String name;
        private BigDecimal priceDelta;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getPriceDelta() {
            return priceDelta;
        }

        public void setPriceDelta(BigDecimal priceDelta) {
            this.priceDelta = priceDelta;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getAvailableForOrder() {
        return availableForOrder;
    }

    public void setAvailableForOrder(Boolean availableForOrder) {
        this.availableForOrder = availableForOrder;
    }

    public String getImageDataUrl() {
        return imageDataUrl;
    }

    public void setImageDataUrl(String imageDataUrl) {
        this.imageDataUrl = imageDataUrl;
    }

    public List<VariantEntry> getVariants() {
        return variants;
    }

    public void setVariants(List<VariantEntry> variants) {
        this.variants = variants;
    }

    public List<VariantEntry> getExtras() {
        return extras;
    }

    public void setExtras(List<VariantEntry> extras) {
        this.extras = extras;
    }
}
