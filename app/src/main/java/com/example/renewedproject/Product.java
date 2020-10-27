package com.example.renewedproject;

import com.google.gson.annotations.SerializedName;

public class Product {
    //JSON 객체를 매칭해 준다.
    @SerializedName("prod_name")
    private String prod_name;

    @SerializedName("prod_price")
    private String prod_price;

    @SerializedName("event_cd")
    private String event_cd;

    public String getProd_name(){
        return prod_name;
    }
    public void setProd_name(String prod_name){
        this.prod_name = prod_name;
    }

    public String getProd_price(){
        return prod_price;
    }
    public void setProd_price(String prod_price){
        this.prod_price = prod_price;
    }

    public String getEvent_cd(){
        return event_cd;
    }
    public void setEvent_cd(String event_cd){
        this.event_cd = event_cd;
    }
}
