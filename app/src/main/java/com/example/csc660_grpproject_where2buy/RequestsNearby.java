package com.example.csc660_grpproject_where2buy;

public class RequestsNearby {
    private String itemName, date;
    private int requestID;
    public RequestsNearby(int a, String b, String c){
        requestID = a;
        itemName = b;
        date = c;
    }

    public int getRequestID() {
        return requestID;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDate() {
        return date;
    }

    public String toString(){
        return "Posted on " + date + "\n" + itemName;
    }
}
