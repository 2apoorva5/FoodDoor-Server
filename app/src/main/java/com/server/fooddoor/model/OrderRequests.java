package com.server.fooddoor.model;

import java.util.List;

public class OrderRequests {
    private String name, email, status, address, latlng, phone, total;
    private List<Orders> foods;

    public OrderRequests() {
    }

    public OrderRequests(String name, String email, String status, String address, String latlng, String phone, String total, List<Orders> foods) {
        this.name = name;
        this.email = email;
        this.status = status;
        this.address = address;
        this.latlng = latlng;
        this.phone = phone;
        this.total = total;
        this.foods = foods;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLatlng() {
        return latlng;
    }

    public void setLatlng(String latlng) {
        this.latlng = latlng;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public List<Orders> getFoods() {
        return foods;
    }

    public void setFoods(List<Orders> foods) {
        this.foods = foods;
    }
}
