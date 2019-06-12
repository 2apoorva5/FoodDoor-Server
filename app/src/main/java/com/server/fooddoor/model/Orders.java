package com.server.fooddoor.model;

public class Orders {
    private String FoodId, FoodName, Quantity, Price, Discount;

    public Orders() {
    }

    public Orders(String foodId, String foodName, String quantity, String price, String discount) {
        FoodId = foodId;
        FoodName = foodName;
        Quantity = quantity;
        Price = price;
        Discount = discount;
    }

    public String getFoodId() {
        return FoodId;
    }

    public void setFoodId(String foodId) {
        FoodId = foodId;
    }

    public String getFoodName() {
        return FoodName;
    }

    public void setFoodName(String foodName) {
        FoodName = foodName;
    }

    public String getQuantity() {
        return Quantity;
    }

    public void setQuantity(String quantity) {
        Quantity = quantity;
    }

    public String getPrice() {
        return Price;
    }

    public void setPrice(String price) {
        Price = price;
    }

    public String getDiscount() {
        return Discount;
    }

    public void setDiscount(String discount) {
        Discount = discount;
    }
}
