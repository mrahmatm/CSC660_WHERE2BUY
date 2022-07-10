package com.example.csc660_grpproject_where2buy;

import android.graphics.Bitmap;

public class RequestsNearby {
    private String itemName, date;
    private int requestID;
    private Bitmap image;
    public RequestsNearby(int a, String b, String c, Bitmap d){
        requestID = a;
        itemName = b;
        date = c;
        image = d;
    }

    public int getRequestID() {
        return requestID;
    }

    public Bitmap getImage() { return image; }

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
