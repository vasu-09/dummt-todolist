package com.om.To_Do.List.ecosystem.dto;

import lombok.*;

@Data

public class UpdateItemRequest {
    private String itemName;
    private String quantity;
    private String priceText;
    private String subQuantitiesJson;

    public UpdateItemRequest() {
    }

    public UpdateItemRequest(String itemName, String quantity, String priceText, String subQuantitiesJson) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.priceText = priceText;
        this.subQuantitiesJson = subQuantitiesJson;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getPriceText() {
        return priceText;
    }

    public void setPriceText(String priceText) {
        this.priceText = priceText;
    }

    public String getSubQuantitiesJson() {
        return subQuantitiesJson;
    }

    public void setSubQuantitiesJson(String subQuantitiesJson) {
        this.subQuantitiesJson = subQuantitiesJson;
    }
}